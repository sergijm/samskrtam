package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassGroupDto;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassTreeNodeDto;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.WorksClass;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorksClassRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorksWorkClassRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Классификатор произведений (works_class + works_work_class).
 * <ul>
 *   <li>GET /works/classes — дерево всех классификаторов, сгруппированных
 *       по значению classification (один дропдаун с множественным выбором на группу);</li>
 *   <li>фильтрация GET /works?classId=... — по выбранным категориям с учётом
 *       всех потомков (выбор родителя включает произведения его подкатегорий).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class WorksClassService {

    private final WorksClassRepository worksClassRepository;
    private final WorksWorkClassRepository worksWorkClassRepository;
    private final WorkRepository workRepository;

    @Transactional(readOnly = true)
    public List<WorksClassGroupDto> getClassGroups() {
        List<WorksClass> all = worksClassRepository.findAllByOrderByClassificationAscSortOrderAsc();
        Map<String, List<WorksClass>> byClassification = new LinkedHashMap<>();
        for (WorksClass c : all) {
            byClassification.computeIfAbsent(c.getClassification(), k -> new ArrayList<>()).add(c);
        }

        List<WorksClassGroupDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<WorksClass>> e : byClassification.entrySet()) {
            groups.add(new WorksClassGroupDto(e.getKey(), buildForest(e.getValue())));
        }
        return groups;
    }

    /**
     * Фильтрация произведений по выбранным категориям классификатора.
     * Выбор категории включает все её подкатегории (рекурсивно). Произведение
     * подходит, если оно привязано хотя бы к одной категории из выбранного набора.
     *
     * @param classIds пустой/null — без фильтра, возвращает все произведения
     */
    @Transactional(readOnly = true)
    public List<Work> filterWorks(Collection<UUID> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc();
        }

        Set<UUID> expanded = expandWithDescendants(classIds);
        List<UUID> workIds = worksWorkClassRepository.findWorkIdsByClassIdIn(List.copyOf(expanded));
        if (workIds.isEmpty()) {
            return List.of();
        }
        return workRepository.findAllByIdInAndDeletedAtIsNullOrderByCreatedAtAsc(
                new HashSet<>(workIds));
    }

    private Set<UUID> expandWithDescendants(Collection<UUID> classIds) {
        List<WorksClass> all = worksClassRepository.findAll();
        Map<UUID, List<WorksClass>> childrenByParent = new HashMap<>();
        for (WorksClass c : all) {
            if (c.getParentId() != null) {
                childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }

        Set<UUID> expanded = new HashSet<>();
        List<UUID> queue = new ArrayList<>(classIds);
        while (!queue.isEmpty()) {
            UUID id = queue.remove(queue.size() - 1);
            if (!expanded.add(id)) {
                continue;
            }
            List<WorksClass> children = childrenByParent.get(id);
            if (children != null) {
                for (WorksClass child : children) {
                    queue.add(child.getId());
                }
            }
        }
        return expanded;
    }

    private List<WorksClassTreeNodeDto> buildForest(List<WorksClass> classes) {
        Map<UUID, WorksClassTreeNodeDto> nodes = new LinkedHashMap<>();
        for (WorksClass c : classes) {
            nodes.put(c.getId(), new WorksClassTreeNodeDto(
                    c.getId(), c.getParentId(), c.getCode(), c.getTitleRu(), c.getTitleEn(),
                    c.getTitleSaIast(), c.getTitleSaDeva(), c.getSortOrder(), new ArrayList<>()));
        }

        List<WorksClassTreeNodeDto> roots = new ArrayList<>();
        for (WorksClassTreeNodeDto node : nodes.values()) {
            if (node.parentId() != null && nodes.containsKey(node.parentId())) {
                nodes.get(node.parentId()).children().add(node);
            } else {
                roots.add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<WorksClassTreeNodeDto> nodes) {
        nodes.sort(Comparator.comparingInt(WorksClassTreeNodeDto::sortOrder));
        for (WorksClassTreeNodeDto node : nodes) {
            sortTree(node.children());
        }
    }
}