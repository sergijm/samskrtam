package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.WorkSummaryDto;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassGroupDto;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassTreeNodeDto;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.WorksClass;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorksClassRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorksWorkClassRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.service.SourceService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты классификатора произведений (works_class): построение дерева по
 * classification и фильтрация GET /works?classId=... с учётом потомков.
 */
class WorksClassServiceTest {

    private WorksClassRepository classRepository;
    private WorksWorkClassRepository linkRepository;
    private WorkRepository workRepository;
    private SourceService sourceService;
    private ChapterRepository chapterRepository;
    private VerseRepository verseRepository;
    private WorksClassService service;

    private static UUID uuid(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + suffix);
    }

    @BeforeEach
    void setUp() {
        classRepository = mock(WorksClassRepository.class);
        linkRepository = mock(WorksWorkClassRepository.class);
        workRepository = mock(WorkRepository.class);
        sourceService = mock(SourceService.class);
        chapterRepository = mock(ChapterRepository.class);
        verseRepository = mock(VerseRepository.class);
        service = new WorksClassService(classRepository, linkRepository, workRepository,
                sourceService, chapterRepository, verseRepository);
    }

    private static WorksClass buildClass(UUID id, UUID parentId, String classification,
                                         String code, String titleRu, int sortOrder) {
        return WorksClass.builder()
                .id(id)
                .parentId(parentId)
                .classification(classification)
                .code(code)
                .titleSaIast(code)
                .titleRu(titleRu)
                .titleEn(titleRu)
                .sortOrder(sortOrder)
                .build();
    }

    @Test
    void getClassGroups_groupsByClassification_andBuildsTree() {
        UUID genre = uuid("000000000001");
        UUID epic = uuid("000000000002");
        UUID era = uuid("000000000003");

        when(classRepository.findAllByOrderByClassificationAscSortOrderAsc())
                .thenReturn(List.of(
                        buildClass(genre, null, "GENRE", "genre", "Жанр", 0),
                        buildClass(epic, genre, "GENRE", "epic", "Эпос", 1),
                        buildClass(era, null, "ERA", "era", "Эпоха", 0)
                ));

        List<WorksClassGroupDto> groups = service.getClassGroups();

        assertThat(groups).hasSize(2);
        WorksClassGroupDto genreGroup = groups.get(0);
        assertThat(genreGroup.classification()).isEqualTo("GENRE");
        assertThat(genreGroup.classes()).hasSize(1);
        WorksClassTreeNodeDto root = genreGroup.classes().get(0);
        assertThat(root.code()).isEqualTo("genre");
        assertThat(root.children()).hasSize(1);
        assertThat(root.children().get(0).code()).isEqualTo("epic");

        WorksClassGroupDto eraGroup = groups.get(1);
        assertThat(eraGroup.classification()).isEqualTo("ERA");
        assertThat(eraGroup.classes()).hasSize(1);
    }

    @Test
    void getClassGroups_sortsChildrenBySortOrder() {
        UUID root = uuid("000000000010");
        UUID c1 = uuid("000000000011");
        UUID c2 = uuid("000000000012");

        when(classRepository.findAllByOrderByClassificationAscSortOrderAsc())
                .thenReturn(List.of(
                        buildClass(root, null, "GENRE", "root", "Корень", 0),
                        buildClass(c1, root, "GENRE", "z-late", "Поздний", 5),
                        buildClass(c2, root, "GENRE", "a-early", "Ранний", 1)
                ));

        List<WorksClassGroupDto> groups = service.getClassGroups();
        List<WorksClassTreeNodeDto> children = groups.get(0).classes().get(0).children();

        assertThat(children).extracting(WorksClassTreeNodeDto::code)
                .containsExactly("a-early", "z-late");
    }

    @Test
    void filterWorks_emptyClassIds_returnsAllWorks() {
        List<Work> all = List.of(Work.builder()
                .id(uuid("000000000020"))
                .slug("gita").titleRu("Гита").titleEn("Gita").build());
        when(workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc()).thenReturn(all);
        when(chapterRepository.countByWorkIdAndDeletedAtIsNull(any())).thenReturn(0L);
        when(verseRepository.countByWorkIdAndDeletedAtIsNull(any())).thenReturn(0);

        List<WorkSummaryDto> result = service.filterWorks(List.of(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("gita");
    }

    @Test
    void filterWorks_nullClassIds_returnsAllWorks() {
        List<Work> all = List.of(Work.builder()
                .id(uuid("000000000021"))
                .slug("gita").titleRu("Гита").titleEn("Gita").build());
        when(workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc()).thenReturn(all);
        when(chapterRepository.countByWorkIdAndDeletedAtIsNull(any())).thenReturn(0L);
        when(verseRepository.countByWorkIdAndDeletedAtIsNull(any())).thenReturn(0);

        assertThat(service.filterWorks(null, null)).hasSize(1);
    }

    @Test
    void filterWorks_parentClass_includesWorksOfDescendants() {
        UUID genre = uuid("000000000001");
        UUID epic = uuid("000000000002");
        UUID ramayana = uuid("000000000003");
        UUID workId = uuid("000000000004");

        when(classRepository.findAll()).thenReturn(List.of(
                buildClass(genre, null, "GENRE", "genre", "Жанр", 0),
                buildClass(epic, genre, "GENRE", "epic", "Эпос", 1),
                buildClass(ramayana, epic, "GENRE", "ramayana", "Рамаяна", 2)
        ));
        when(linkRepository.findWorkIdsByClassIdIn(List.of(genre, epic, ramayana)))
                .thenReturn(List.of(workId));
        when(workRepository.findAllByIdInAndDeletedAtIsNullOrderByCreatedAtAsc(anyCollection()))
                .thenReturn(List.of(Work.builder().id(workId).slug("ram").titleRu("Р").titleEn("R").build()));
        when(chapterRepository.countByWorkIdAndDeletedAtIsNull(workId)).thenReturn(2L);
        when(verseRepository.countByWorkIdAndDeletedAtIsNull(workId)).thenReturn(24);

        List<WorkSummaryDto> works = service.filterWorks(List.of(genre), null);

        assertThat(works).hasSize(1);
        assertThat(works.get(0).id()).isEqualTo(workId);
        assertThat(works.get(0).chapterCount()).isEqualTo(2);
        assertThat(works.get(0).verseCount()).isEqualTo(24);
    }

    @Test
    void filterWorks_withoutMatchingWorks_returnsEmpty() {
        UUID genre = uuid("000000000001");
        when(classRepository.findAll()).thenReturn(List.of(
                buildClass(genre, null, "GENRE", "genre", "Жанр", 0)));
        when(linkRepository.findWorkIdsByClassIdIn(List.of(genre))).thenReturn(List.of());

        assertThat(service.filterWorks(List.of(genre), null)).isEmpty();
    }
}