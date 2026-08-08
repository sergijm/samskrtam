package sm.selflearn.samskrtam.curriculum.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Optional;

/**
 * Наполняет лексикон curriculum существительными, полученными от sangraha-service,
 * для тем склонений (a-stem-masc/neut/fem, i/u/r). Каждая тема с
 * {@code targetItemCount > 0} тянет до этого числа кандидатов того же класса
 * основы и создаёт {@link Lexeme}, привязанные к MorphologyClass с кодом темы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclensionDataImporter {

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final SangrahaClient sangrahaClient;

    /**
     * Импортирует недостающие существительные для всех тем, у которых
     * {@link Topic#getTargetItemCount()} > 0 и в морфо-классе ещё нет лексем.
     *
     * @return число созданных Lexeme
     */
    @Transactional
    public int importForPendingTopics() {
        List<Topic> targets = topicRepository.findByTargetItemCountGreaterThan(0);
        int created = 0;
        for (Topic topic : targets) {
            String classCode = topic.getCode();
            if (lexemeRepository.countByMorphologyClasses_Code(classCode) > 0) {
                log.debug("Morphology class {} already has lexemes; skipping {}", classCode, topic.getTitleRu());
                continue;
            }
            created += importTopic(topic);
        }
        return created;
    }

    private int importTopic(Topic topic) {
        String classCode = topic.getCode();
        String stemClass = stemClassFor(classCode);
        if (stemClass == null) {
            log.debug("Topic {} is not a noun declension class; skipped", classCode);
            return 0;
        }
        int target = Math.max(0, topic.getTargetItemCount());
        NominalLemmaCandidatesResponse response = sangrahaClient.fetchCandidates(stemClass, target);
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            log.debug("No sangraha candidates for {} (stemClass={})", classCode, stemClass);
            return 0;
        }

        MorphologyClass clazz = morphologyClassRepository.findByCode(classCode).orElse(null);
        if (clazz == null) {
            log.warn("MorphologyStemclass {} not found; skipping import", classCode);
            return 0;
        }
        Optional<LexemeGender> fixedGender = fixedGenderFor(classCode);

        int count = 0;
        for (NominalLemmaCandidateDto candidate : response.candidates()) {
            if (count >= target) {
                break;
            }
            if (candidate.lemmaSlp1() == null || candidate.lemmaSlp1().isBlank()) {
                continue;
            }
            LexemeGender candidateGender = parseGender(candidate.gender());
            if (!acceptsGender(fixedGender, candidateGender)) {
                continue;
            }
            if (lexemeRepository.findByLemmaSlp1(candidate.lemmaSlp1()).isPresent()) {
                continue;
            }
            Lexeme lexeme = new Lexeme();
            lexeme.setLemmaIast(candidate.lemmaIast());
            lexeme.setLemmaDevanagari(candidate.lemmaDevanagari());
            lexeme.setLemmaSlp1(candidate.lemmaSlp1());
            lexeme.setGender(fixedGender.orElse(candidateGender != null ? candidateGender : LexemeGender.UNSPECIFIED));
            lexeme.setGlossRu(candidate.lemmaIast());
            lexeme.setGlossEn(candidate.lemmaIast());
            lexeme.getMorphologyClasses().add(clazz);
            lexemeRepository.save(lexeme);
            count++;
        }
        log.info("Imported {} lexemes for topic {} (target={})", count, topic.getTitleEn(), target);
        return count;
    }

    // ---- mapping helpers -------------------------------------------------

    /** topic.code → sangraha stemClass (соответствует VowelType регулярного класса). */
    private String stemClassFor(String code) {
        return switch (code) {
            case "a-stem-masc", "a-stem-neut" -> "A_STEM";
            case "a-stem-fem" -> "AA_STEM";
            case "i-stem" -> "I_STEM";
            case "u-stem" -> "U_STEM";
            case "r-stem" -> "R_STEM";
            default -> null;
        };
    }

    private Optional<LexemeGender> fixedGenderFor(String code) {
        return switch (code) {
            case "a-stem-masc" -> Optional.of(LexemeGender.MASCULINE);
            case "a-stem-neut" -> Optional.of(LexemeGender.NEUTER);
            case "a-stem-fem" -> Optional.of(LexemeGender.FEMININE);
            default -> Optional.empty();
        };
    }

    private static LexemeGender parseGender(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case "MASCULINE" -> LexemeGender.MASCULINE;
            case "FEMININE" -> LexemeGender.FEMININE;
            case "NEUTER" -> LexemeGender.NEUTER;
            case "UNSPECIFIED" -> LexemeGender.UNSPECIFIED;
            default -> null;
        };
    }

    /** Для фиксированного рода темы — кандидат другого рода не подходит. */
    private static boolean acceptsGender(Optional<LexemeGender> fixed, LexemeGender candidate) {
        return fixed.isEmpty() || candidate == null || fixed.get() == candidate;
    }
}