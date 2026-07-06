package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.CaseEndingRepository;
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
    private final CaseEndingRepository caseEndingRepository;

    public List<DeclensionStemDto> getDeclensionStemsForLesson(String slug) {
        log.info("Fetching declension stems for slug: {}", slug);
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        VowelType vowelType = mapSlugToVowelType(slug);
        List<DeclensionStem> stems;
        if (vowelType != null) {
            stems = declensionStemRepository.findByVowelType(vowelType);
        } else {
            stems = declensionStemRepository.findAll();
        }

        return stems.stream()
                .map(stem -> DeclensionStemDto.builder()
                        .id(stem.getId())
                                                .lessonId(lesson.getId())
                                                .slug(stem.getStemIast()) // Changed from getStemNameIast() to getStemIast()
                                                .gender(stem.getGender())
                                                .vowelType(stem.getVowelType())
                                                .stemDevanagari(stem.getStemDevanagari())
                                                .translationRu(stem.getTranslationRu())
                                                .translationEn(stem.getTranslationEn())
                                                .build())
                .collect(Collectors.toList());
    }

    public List<CaseEndingDto> getCaseEndingsByVowelType(VowelType vowelType) {
        log.info("Fetching case endings for vowelType: {}", vowelType.name());
        return caseEndingRepository.findByVowelType(vowelType).stream()
                .map(ce -> CaseEndingDto.builder()
                        .id(ce.getId())
                        .vowelType(ce.getVowelType())
                        .gender(ce.getGender())
                        .caseType(ce.getCaseType())
                        .numberType(ce.getNumberType())
                        .endingIast(ce.getEndingIast())
                        .endingDevanagari(ce.getEndingDevanagari())
                        .build())
                .collect(Collectors.toList());
    }

    private VowelType mapSlugToVowelType(String slug) {
        if (slug == null) return null;
        if (slug.startsWith("declensions-a-") || slug.equals("declensions-a-masc") || slug.equals("declensions-a-neut")) return VowelType.A_STEM;
        if (slug.startsWith("declensions-aa-") || slug.equals("declensions-a-fem")) return VowelType.AA_STEM;
        if (slug.startsWith("declensions-ii-") || slug.equals("declensions-ii") || slug.equals("declensions-ii-fem")) return VowelType.II_STEM;
        if (slug.startsWith("declensions-i-") || slug.equals("declensions-i")) return VowelType.I_STEM;
        if (slug.startsWith("declensions-uu-") || slug.equals("declensions-uu") || slug.equals("declensions-uu-fem")) return VowelType.UU_STEM;
        if (slug.startsWith("declensions-u-") || slug.equals("declensions-u")) return VowelType.U_STEM;
        if (slug.startsWith("declensions-r-") || slug.equals("declensions-r")) return VowelType.R_STEM;
        return null;
    }
}