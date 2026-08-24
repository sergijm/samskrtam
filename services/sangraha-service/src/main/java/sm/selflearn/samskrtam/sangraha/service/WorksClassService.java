package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassGroupDto;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassTreeNodeDto;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.WorksClass;
import sm.selflearn.samskrtam.sangraha.model.Source;
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
    private final SourceService sourceService;

    @Transactional(readOnly = true)
    public List<WorksClassGroupDto> getClassGroups() {
        List<WorksClass> all = worksClassRepository.findAllByOrderByClassificationAscSortOrderAsc();

        // Preload direct work counts per class ID
        Map<UUID, Integer> directCounts = new HashMap<>();
        for (Object[] row : worksWorkClassRepository.countWorksByClassId()) {
            directCounts.put((UUID) row[0], ((Long) row[1]).intValue());
        }

        // Compute aggregated counts across ALL classifications (cross-group children)
        Map<UUID, Integer> aggregatedCounts = computeAggregatedCounts(all, directCounts);

        Map<String, List<WorksClass>> byClassification = new LinkedHashMap<>();
        for (WorksClass c : all) {
            byClassification.computeIfAbsent(c.getClassification(), k -> new ArrayList<>()).add(c);
        }

        List<WorksClassGroupDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<WorksClass>> e : byClassification.entrySet()) {
            groups.add(new WorksClassGroupDto(e.getKey(),
                    buildForestWithAggregatedCounts(e.getValue(), aggregatedCounts)));
        }
        return groups;
    }

    /**
     * Computes the total work count for each class by expanding all descendants
     * (cross-classification) and summing their direct counts.  This exactly
     * mirrors what {@link #filterWorks} does with {@link #expandWithDescendants}.
     */
    private Map<UUID, Integer> computeAggregatedCounts(List<WorksClass> all, Map<UUID, Integer> directCounts) {
        // Build descendant map: parentId → list of child IDs (from ALL classes)
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        for (WorksClass c : all) {
            if (c.getParentId() != null) {
                childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            }
        }

        Map<UUID, Integer> aggregated = new HashMap<>();
        for (WorksClass c : all) {
            int total = directCounts.getOrDefault(c.getId(), 0);
            // BFS to collect all descendants
            List<UUID> queue = new ArrayList<>();
            List<UUID> ch = childrenByParent.get(c.getId());
            if (ch != null) queue.addAll(ch);
            Set<UUID> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                UUID childId = queue.remove(queue.size() - 1);
                if (!visited.add(childId)) continue;
                total += directCounts.getOrDefault(childId, 0);
                List<UUID> grandChildren = childrenByParent.get(childId);
                if (grandChildren != null) queue.addAll(grandChildren);
            }
            aggregated.put(c.getId(), total);
        }
        return aggregated;
    }

    /** Builds forest for a single group using pre-computed aggregated counts. */
    private List<WorksClassTreeNodeDto> buildForestWithAggregatedCounts(
            List<WorksClass> classes, Map<UUID, Integer> aggregatedCounts) {
        Map<UUID, WorksClassTreeNodeDto> nodes = new LinkedHashMap<>();
        for (WorksClass c : classes) {
            int count = aggregatedCounts.getOrDefault(c.getId(), 0);
            nodes.put(c.getId(), new WorksClassTreeNodeDto(
                    c.getId(), c.getParentId(), c.getCode(), c.getTitleRu(), c.getTitleEn(),
                    c.getTitleSaIast(), c.getTitleSaDeva(), c.getSortOrder(), count, new ArrayList<>()));
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

    /**
     * Фильтрация произведений по выбранным категориям классификатора и (опционально)
     * по источнику. Выбор категории включает все её подкатегории (рекурсивно).
     * Произведение подходит, если оно привязано хотя бы к одной категории из
     * выбранного набора И (при заданном sourceCode) относится к этому источнику.
     * Фильтры комбинируются по И (AND).
     *
     * @param classIds   пустой/null — без фильтра по классификатору
     * @param sourceCode пустой/null — без фильтра по источнику
     */
    @Transactional(readOnly = true)
    public List<Work> filterWorks(Collection<UUID> classIds, String sourceCode) {
        List<Work> base;
        if (classIds == null || classIds.isEmpty()) {
            base = workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc();
        } else {
            Set<UUID> expanded = expandWithDescendants(classIds);
            List<UUID> workIds = worksWorkClassRepository.findWorkIdsByClassIdIn(List.copyOf(expanded));
            if (workIds.isEmpty()) {
                return List.of();
            }
            base = workRepository.findAllByIdInAndDeletedAtIsNullOrderByCreatedAtAsc(
                    new HashSet<>(workIds));
        }

        if (sourceCode == null || sourceCode.isBlank()) {
            return base;
        }
        Source source = sourceService.findByCode(sourceCode).orElse(null);
        if (source == null) {
            return List.of();
        }
        UUID sourceId = source.getId();
        return base.stream()
                .filter(w -> sourceId.equals(w.getSourceId()))
                .toList();
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

    private void sortTree(List<WorksClassTreeNodeDto> nodes) {
        nodes.sort(Comparator.comparingInt(WorksClassTreeNodeDto::sortOrder));
        for (WorksClassTreeNodeDto node : nodes) {
            sortTree(node.children());
        }
    }
}