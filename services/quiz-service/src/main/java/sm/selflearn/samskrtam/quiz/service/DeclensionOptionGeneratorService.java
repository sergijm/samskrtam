package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.quiz.dto.QuestionOptionDto;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeclensionOptionGeneratorService {

    private final ContentClient contentClient;

    public Mono<List<QuestionOptionDto>> generateOptions(
            UUID declensionStemId,
            CaseType targetCase,
            NumberType targetNumber,
            String correctFormIast
    ) {
        return contentClient.getDeclensionForms(declensionStemId)
                .flatMap(allForms -> {
                    if (allForms.isEmpty()) {
                        return Mono.error(new SamskrtamException("NO_DECLENSION_FORMS", "No forms found for stem: " + declensionStemId));
                    }

                    // Find the correct option
                    DeclensionFormDto correctFormDto = allForms.stream()
                            .filter(form -> form.getCaseType() == targetCase && form.getNumberType() == targetNumber)
                            .findFirst()
                            .orElseThrow(() -> new SamskrtamException("CORRECT_FORM_MISSING", "Correct form not found for stem: " + declensionStemId + ", case: " + targetCase + ", number: " + targetNumber));

                    // Ensure the correctFormIast matches
                    if (!correctFormDto.getFormIast().equals(correctFormIast)) {
                        return Mono.error(new SamskrtamException("FORM_MISMATCH", "Correct form IAST mismatch for stem: " + declensionStemId));
                    }

                    QuestionOptionDto correctOption = QuestionOptionDto.builder()
                            .id(UUID.randomUUID()) // Generate a new ID for the option
                            .formIast(correctFormDto.getFormIast())
                            .formDevanagari(correctFormDto.getFormDevanagari())
                            .build();

                    // Generate distractors
                    List<DeclensionFormDto> distractorPool = allForms.stream()
                            .filter(form -> !(form.getCaseType() == targetCase && form.getNumberType() == targetNumber)) // Exclude the correct form
                            .filter(form -> !form.getFormIast().equals(correctFormIast)) // Exclude homonymous forms
                            .collect(Collectors.toCollection(ArrayList::new));

                    Collections.shuffle(distractorPool);

                    List<QuestionOptionDto> options = new ArrayList<>();
                    options.add(correctOption);

                    // Add up to 3 unique distractors
                    distractorPool.stream()
                            .limit(3)
                            .map(form -> QuestionOptionDto.builder()
                                    .id(UUID.randomUUID())
                                    .formIast(form.getFormIast())
                                    .formDevanagari(form.getFormDevanagari())
                                    .build())
                            .forEach(options::add);

                                        Collections.shuffle(options); // Shuffle all options (correct + distractors)
                    return Mono.just(options);
                });
    }

    /**
     * Generates options with {@code optionType="CASE_COMBINATION"} for CASE_BY_FORM questions.
     * <p>
     * The user sees a form (IAST/Devanagari) and must select the correct
     * case×number×gender combination from the options.
     * <p>
     * For gender=UNSPECIFIED stems (vowel_type I/I_LONG/U/U_LONG/R),
     * targetGender arrives as "UNSPECIFIED" — no special branch needed.
     */
    public Mono<List<QuestionOptionDto>> generateCaseOptions(
            UUID declensionStemId,
            CaseType targetCase,
            NumberType targetNumber,
            String targetGender
    ) {
        return contentClient.getDeclensionForms(declensionStemId)
                .flatMap(allForms -> {
                    if (allForms.isEmpty()) {
                        return Mono.error(new SamskrtamException("NO_DECLENSION_FORMS",
                                "No forms found for stem: " + declensionStemId));
                    }

                    // Build lookup: (caseType, numberType) → formIast
                    var formLookup = allForms.stream()
                            .collect(Collectors.toMap(
                                    f -> f.getCaseType().name() + ":" + f.getNumberType().name(),
                                    DeclensionFormDto::getFormIast,
                                    (a, b) -> a));

                    // Correct form IAST
                    String correctKey = targetCase.name() + ":" + targetNumber.name();
                    String correctFormIast = formLookup.get(correctKey);
                    if (correctFormIast == null) {
                        return Mono.error(new SamskrtamException("CORRECT_FORM_MISSING",
                                "Correct form not found for stem: " + declensionStemId
                                + ", case: " + targetCase + ", number: " + targetNumber));
                    }

                    // Find correct form DTO for devanagari
                    DeclensionFormDto correctDto = allForms.stream()
                            .filter(f -> f.getCaseType() == targetCase && f.getNumberType() == targetNumber)
                            .findFirst()
                            .orElseThrow();

                    // 1. Correct option
                    QuestionOptionDto correctOption = QuestionOptionDto.builder()
                            .id(UUID.randomUUID())
                            .optionType("CASE_COMBINATION")
                            .formIast(correctDto.getFormIast())
                            .formDevanagari(correctDto.getFormDevanagari())
                            .caseType(targetCase.name())
                            .caseRu(CaseNumberGenderLocalizer.caseTypeRu(targetCase))
                            .caseEn(CaseNumberGenderLocalizer.caseTypeEn(targetCase))
                            .numberType(targetNumber.name())
                            .numberRu(CaseNumberGenderLocalizer.numberTypeRu(targetNumber))
                            .numberEn(CaseNumberGenderLocalizer.numberTypeEn(targetNumber))
                            .gender(targetGender)
                            .genderRu(CaseNumberGenderLocalizer.genderRu(targetGender))
                            .genderEn(CaseNumberGenderLocalizer.genderEn(targetGender))
                            .build();

                    // 2. Build all other (case, number) triples for this stem
                    record Triple(CaseType c, NumberType n, String formIast) {}
                    List<Triple> otherTriples = new ArrayList<>();
                    for (CaseType c : CaseType.values()) {
                        for (NumberType n : NumberType.values()) {
                            if (c == targetCase && n == targetNumber) continue;
                            String key = c.name() + ":" + n.name();
                            String fiast = formLookup.get(key);
                            if (fiast != null) {
                                otherTriples.add(new Triple(c, n, fiast));
                            }
                        }
                    }

                    // 3. Split into homonyms and others
                    List<Triple> homonyms = new ArrayList<>();
                    List<Triple> otherDistractors = new ArrayList<>();
                    for (Triple t : otherTriples) {
                        if (t.formIast.equals(correctFormIast)) {
                            homonyms.add(t);
                        } else {
                            otherDistractors.add(t);
                        }
                    }

                    Collections.shuffle(homonyms);
                    Collections.shuffle(otherDistractors);

                    // 4. Assemble distractors: at most 1 homonym, rest from others up to 3 total
                    List<Triple> selectedDistractors = new ArrayList<>();
                    if (!homonyms.isEmpty()) {
                        selectedDistractors.add(homonyms.get(0));
                    }
                    int remaining = 3 - selectedDistractors.size();
                    for (int i = 0; i < remaining && i < otherDistractors.size(); i++) {
                        selectedDistractors.add(otherDistractors.get(i));
                    }

                    // 5. Build options list
                    List<QuestionOptionDto> options = new ArrayList<>();
                    options.add(correctOption);

                    for (Triple t : selectedDistractors) {
                        options.add(QuestionOptionDto.builder()
                                .id(UUID.randomUUID())
                                .optionType("CASE_COMBINATION")
                                .formIast(t.formIast)
                                .formDevanagari(null)
                                .caseType(t.c.name())
                                .caseRu(CaseNumberGenderLocalizer.caseTypeRu(t.c))
                                .caseEn(CaseNumberGenderLocalizer.caseTypeEn(t.c))
                                .numberType(t.n.name())
                                .numberRu(CaseNumberGenderLocalizer.numberTypeRu(t.n))
                                .numberEn(CaseNumberGenderLocalizer.numberTypeEn(t.n))
                                .gender(targetGender)
                                .genderRu(CaseNumberGenderLocalizer.genderRu(targetGender))
                                .genderEn(CaseNumberGenderLocalizer.genderEn(targetGender))
                                .build());
                    }

                    Collections.shuffle(options);
                    return Mono.just(options);
                });
    }
}

