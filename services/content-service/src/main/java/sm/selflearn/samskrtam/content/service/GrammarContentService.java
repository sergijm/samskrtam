package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.mapper.CaseEndingMapper;
import sm.selflearn.samskrtam.content.mapper.DeclensionParadigmMapper;
import sm.selflearn.samskrtam.content.mapper.DeclensionStemMapper;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.repository.DeclensionStemRepository;
import sm.selflearn.samskrtam.content.repository.LessonRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarContentService {

    private final LessonRepository lessonRepository;
    private final DeclensionStemRepository declensionStemRepository;
    private final DeclensionFormRepository declensionFormRepository;
    private final DeclensionCaseEndingFilterService caseEndingFilterService;
    private final CaseEndingMapper caseEndingMapper;
    private final DeclensionStemMapper declensionStemMapper;
    private final DeclensionParadigmMapper declensionParadigmMapper;

    public List<DeclensionStemDto> getDeclensionStemsForLesson(String slug) {
        log.info("Fetching declension stems for slug: {}", slug);
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        List<DeclensionStem> stems;
        if (!vowelTypes.isEmpty()) {
            stems = declensionStemRepository.findByVowelTypeIn(vowelTypes);
        } else {
            stems = declensionStemRepository.findAll();
        }

        return stems.stream()
                .map(stem -> declensionStemMapper.toDeclensionStemDto(stem, lesson.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Получить список case_endings для урока склонений с опциональной фильтрацией.
     * <p>
     * Определяет VowelType из slug урока, затем возвращает все case_endings,
     * соответствующие (vowelType + опциональным фильтрам).
     * <p>
     * Используется как scope-запрос для {@code QuizGenerator} при itemType=DECLENSION_FORM.
     * Снимает открытый вопрос §3.4 "признак типа фильтра" — фильтрация данных
     * происходит на уровне content-service, а не в полях QuizSession.
     *
     * @param slug       slug урока склонений (например, "declensions-a-masc")
     * @param caseType   опциональный фильтр по падежу (null = все падежи)
     * @param numberType опциональный фильтр по числу (null = все числа)
     * @param gender     опциональный фильтр по роду
     * @return список CaseEndingDto, подходящих под фильтр
     */
    public List<CaseEndingDto> getCaseEndingsForLesson(
            String slug,
            CaseType caseType,
            NumberType numberType,
            Gender gender) {
        log.info("Fetching case endings for lesson slug: {}, caseType: {}, numberType: {}, gender: {}",
                slug, caseType, numberType, gender);

        lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        if (vowelTypes.isEmpty()) {
            log.warn("Could not determine VowelType from slug: {}, returning empty case endings", slug);
            return List.of();
        }

        return caseEndingFilterService.filter(vowelTypes, caseType, numberType, gender).stream()
                .map(caseEndingMapper::toCaseEndingDto)
                .collect(Collectors.toList());
    }

    public List<CaseEndingDto> getCaseEndingsByVowelType(VowelType vowelType) {
        log.info("Fetching case endings for vowelType: {}", vowelType.name());
        return caseEndingFilterService.filter(List.of(vowelType), null, null, null).stream()
                .map(caseEndingMapper::toCaseEndingDto)
                .collect(Collectors.toList());
    }

        /**
     * Возвращает ОДНУ парадигму (стем + все его формы) по индексу,
     * плюс carousel-метаданные (index, totalCount).
     * <p>
     * Стемы стабильно сортируются по {@code id} (UUID — лексикографический порядок).
     * Формы загружаются ТОЛЬКО для index-го стема, формы других стемов не грузятся.
     *
     * @param slug  slug урока DECLENSIONS
     * @param index 0-based индекс стема в отсортированном списке
     * @return DeclensionParadigmPageDto с текущей парадигмой и метаданными карусели
     * @throws SamskrtamException если урок не найден, не DECLENSIONS, или index вне диапазона
     */
    public DeclensionParadigmPageDto getDeclensionParadigmForLesson(String slug, int index) {
        log.info("Fetching declension paradigm [{}/?] for lesson slug: {}", index, slug);
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        if (lesson.getLessonType() != LessonType.DECLENSIONS) {
            throw new SamskrtamException("LESSON_NOT_FOUND",
                    "Lesson with slug '%s' is not a DECLENSIONS lesson".formatted(slug));
        }

        List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        List<DeclensionStem> stems;
        if (!vowelTypes.isEmpty()) {
            stems = declensionStemRepository.findByVowelTypeIn(vowelTypes);
        } else {
            stems = declensionStemRepository.findAll();
        }

        // Stable order: by id (UUID — natural ordering, consistent across calls)
        stems.sort((a, b) -> a.getId().compareTo(b.getId()));

        int totalCount = stems.size();
        if (index < 0 || index >= totalCount) {
            throw new SamskrtamException("LESSON_NOT_FOUND",
                    "Index %d out of range [0, %d) for lesson slug '%s'".formatted(index, totalCount, slug));
        }

        DeclensionStem stem = stems.get(index);
        List<DeclensionForm> forms = declensionFormRepository.findByDeclensionStemId(stem.getId());
        DeclensionParadigmDto paradigm = declensionParadigmMapper.toDeclensionParadigmDto(stem, forms);

        return DeclensionParadigmPageDto.builder()
                .index(index)
                .totalCount(totalCount)
                .paradigm(paradigm)
                .build();
    }

    /**
     * @deprecated replaced by {@link #getDeclensionParadigmForLesson(String, int)} — index-based pagination.
     * Kept temporarily for backward compatibility; will be removed after frontend and
     * gateway switch to the index-based endpoint.
     */
        @Deprecated
    public List<DeclensionParadigmDto> getDeclensionParadigmsForLesson(String slug) {
        log.info("Fetching declension paradigms (deprecated all-at-once) for lesson slug: {}", slug);
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        if (lesson.getLessonType() != LessonType.DECLENSIONS) {
            throw new SamskrtamException("LESSON_NOT_FOUND",
                    "Lesson with slug '%s' is not a DECLENSIONS lesson".formatted(slug));
        }

        List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        List<DeclensionStem> stems;
        if (!vowelTypes.isEmpty()) {
            stems = declensionStemRepository.findByVowelTypeIn(vowelTypes);
        } else {
            stems = declensionStemRepository.findAll();
        }

        return stems.stream()
                .map(stem -> {
                    List<DeclensionForm> forms = declensionFormRepository.findByDeclensionStemId(stem.getId());
                    return declensionParadigmMapper.toDeclensionParadigmDto(stem, forms);
                })
                .collect(Collectors.toList());
    }

        /**
     * @deprecated Use {@link SlugToVowelTypeMapper#mapSlugToVowelType(String)}.
     */
    @Deprecated
    public VowelType mapSlugToVowelType(String slug) {
        return SlugToVowelTypeMapper.mapSlugToVowelType(slug);
    }
}
