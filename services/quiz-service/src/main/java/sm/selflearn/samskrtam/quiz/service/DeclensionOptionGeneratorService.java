package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;
import sm.selflearn.samskrtam.quiz.dto.QuestionOptionDto;

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
            Case targetCase,
            Number targetNumber,
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
}
