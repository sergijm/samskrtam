package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchRequestDto.CellDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchResponseDto.GroupDto;
import sm.selflearn.samskrtam.sangraha.model.Gender;
import sm.selflearn.samskrtam.sangraha.model.GrammaticalCase;
import sm.selflearn.samskrtam.sangraha.model.NumberType;
import sm.selflearn.samskrtam.sangraha.model.NominalLemma;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.VerseWordCount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Поиск примеров словоформ по словоизменительному классу (sangraha-service.md §9) для
 * внутреннего эндпоинта POST /sangraha/internal/content/declension-examples.
 * Поиск не фильтрует по Verse.status — идёт напрямую по наличию подходящей
 * verse_word_morphology (см. §9, B1). Ранжирование — по длине стиха в словах (§9).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseWordSearchService {

    private final VerseWordRepository verseWordRepository;

    /**
     * Фиксированное соответствие местоимённых vowelType → lemmaIast (sangraha-service.md §9).
     * Местоимённые парадигмы супплетивны (например, aham → mayā в творительном), сопоставление
     * по окончанию основы не работает — поиск идёт по лемме.
     */
    private static final Map<VowelType, String> PRON_LEMMA_IAST = Map.of(
            VowelType.PRON_AHAM, "asmad",
            VowelType.PRON_TVAM, "yuṣmad",
            VowelType.PRON_TAD, "tad",
            VowelType.PRON_ETAD, "etad",
            VowelType.PRON_IDAM, "idam",
            VowelType.PRON_KIM, "kim",
            VowelType.PRON_YAD, "yad",
            VowelType.PRON_REFLEXIVE, "ātman"
    );

    /**
     * Кандидат на попадание в группу: стих + длина стиха в словах.
     */
    public record Candidate(UUID verseId, long wordCount) {}

    /**
     * Маппинг последней буквы основы → регулярный класс (sangraha-service.md §9):
     * a→A_STEM, ā→AA_STEM, i→I_STEM, ī→II_STEM, u→U_STEM, ū→UU_STEM, ṛ/r→R_STEM.
     * Возвращает null, если основа отсутствует или не оканчивается на гласную регулярного класса.
     */
    public static VowelType classifyVowelType(String stem) {
        if (stem == null || stem.isEmpty()) {
            return null;
        }
        char last = stem.charAt(stem.length() - 1);
        return switch (last) {
            case 'a' -> VowelType.A_STEM;
            case 'ā' -> VowelType.AA_STEM;
            case 'i' -> VowelType.I_STEM;
            case 'ī' -> VowelType.II_STEM;
            case 'u' -> VowelType.U_STEM;
            case 'ū' -> VowelType.UU_STEM;
            case 'ṛ', 'r' -> VowelType.R_STEM;
            default -> null;
        };
    }

    /**
     * vowelType слова (sangraha-service.md §9): приоритет nominal_lemmas — stemClass строки
     * на лемму слова (одна строка на lemma_iast, выбирать не из чего); если леммы нет в
     * таблице (или stemClass в ней null) — прежняя эвристика по последней букве stem.
     * Чистая функция.
     */
    static VowelType resolveVowelType(NominalLemma lemma, String stem) {
        if (lemma != null && lemma.getStemClass() != null) {
            return lemma.getStemClass();
        }
        return classifyVowelType(stem);
    }

    @Transactional(readOnly = true)
    public DeclensionExamplesSearchResponseDto searchExamples(DeclensionExamplesSearchRequestDto request) {
        if (request == null || request.vowelType() == null || request.gender() == null
                || request.cells() == null || request.cells().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "vowelType, gender and non-empty cells are required");
        }
        if (request.limitPerGroup() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limitPerGroup must be >= 1");
        }

        Gender gender = toSangrahaGender(request.gender());
        List<GroupDto> groups = new ArrayList<>(request.cells().size());
        for (CellDto cell : request.cells()) {
            List<UUID> verseIds = searchForCell(request.vowelType(), gender, cell,
                    request.limitPerGroup(), request.maxPhraseWords());
            groups.add(new GroupDto(cell.caseType(), cell.numberType(), verseIds));
        }
        return new DeclensionExamplesSearchResponseDto(groups);
    }

    private List<UUID> searchForCell(VowelType vowelType, Gender gender, CellDto cell,
                                     int limitPerGroup, int maxPhraseWords) {
        GrammaticalCase caseType = toSangrahaCase(cell.caseType());
        NumberType numberType = toSangrahaNumber(cell.numberType());
        if (gender == null || caseType == null || numberType == null) {
            return List.of();
        }

        List<VerseWordCount> counts;
        if (isRegular(vowelType)) {
            counts = verseWordRepository.findVerseWordCountsByVowelType(
                    gender, caseType, numberType, vowelType, maxPhraseWords);
        } else {
            String lemmaIast = PRON_LEMMA_IAST.get(vowelType);
            if (lemmaIast == null) {
                return List.of();
            }
            counts = verseWordRepository.findVerseWordCountsByLemmaIast(
                    gender, caseType, numberType, lemmaIast, maxPhraseWords);
        }

        List<Candidate> candidates = counts.stream()
                .map(count -> new Candidate(count.getVerseId(), count.getWordCount()))
                .toList();
        return rankAndSelect(candidates, limitPerGroup);
    }

    /**
     * Ранжирование и отбор ≤ limitPerGroup (sangraha-service.md §9): кандидаты делятся на
     * основной уровень (wordCount >= 3) и резервный (wordCount < 3); внутри каждого уровня —
     * сортировка по возрастанию wordCount, при равенстве — по verseId (детерминированно).
     * Сначала берутся первые limitPerGroup из основного уровня, недостающее добирается
     * из начала резервного. Чистая функция — тестируется юнитом (B4).
     */
    static List<UUID> rankAndSelect(List<Candidate> candidates, int limitPerGroup) {
        Comparator<Candidate> byWordCountThenVerseId = Comparator
                .comparingLong(Candidate::wordCount)
                .thenComparing(Candidate::verseId);
        List<Candidate> primary = candidates.stream()
                .filter(candidate -> candidate.wordCount() >= 3)
                .sorted(byWordCountThenVerseId)
                .toList();
        List<Candidate> reserve = candidates.stream()
                .filter(candidate -> candidate.wordCount() < 3)
                .sorted(byWordCountThenVerseId)
                .toList();

        List<UUID> result = new ArrayList<>(Math.min(limitPerGroup, candidates.size()));
        for (Candidate candidate : primary) {
            if (result.size() >= limitPerGroup) {
                break;
            }
            result.add(candidate.verseId());
        }
        for (Candidate candidate : reserve) {
            if (result.size() >= limitPerGroup) {
                break;
            }
            result.add(candidate.verseId());
        }
        return result;
    }

    private static boolean isRegular(VowelType vowelType) {
        return vowelType != null && !vowelType.name().startsWith("PRON_");
    }

    private static Gender toSangrahaGender(sm.selflearn.samskrtam.content.model.Gender gender) {
        try {
            return Gender.valueOf(gender.name());
        } catch (IllegalArgumentException e) {
            // content.Gender.UNKNOWN отсутствует в sangraha.Gender — такие строки не матчатся
            return null;
        }
    }

    private static GrammaticalCase toSangrahaCase(sm.selflearn.samskrtam.content.model.CaseType caseType) {
        if (caseType == null) {
            return null;
        }
        return GrammaticalCase.valueOf(caseType.name());
    }

    private static NumberType toSangrahaNumber(sm.selflearn.samskrtam.content.model.NumberType numberType) {
        if (numberType == null) {
            return null;
        }
        return NumberType.valueOf(numberType.name());
    }
}
