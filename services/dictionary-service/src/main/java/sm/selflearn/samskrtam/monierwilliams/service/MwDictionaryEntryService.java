package sm.selflearn.samskrtam.monierwilliams.service;


import com.wellebee.sanskrit.Sanscript;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.entity.*;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;
import sm.selflearn.samskrtam.monierwilliams.repository.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MwDictionaryEntryService {

    private final MwEntryRepository entryRepository;
    private final MwSanskritWordRepository sanskritWordRepository;
    private final MwHomonymRepository homonymRepository;
    private final MwAbbreviationRepository abbreviationRepository;
    private final MwLiterarySourceRepository literarySourceRepository;
    private final MwInfoRepository infoRepository;
    private final MwLexicalInfoRepository lexicalInfoRepository;
    private final TransliterationService transliterationService;
    private final MwTranslationExtractor translationExtractor;
    private final MwXmlTranslationExtractor xmlTranslationExtractor;


    private final Sanscript sanscript = new Sanscript();

    /**
     * Получить полную статью по record_id
     */
    @Transactional(readOnly = true)
    public MwDictionaryEntryDto getEntryByRecordId(String recordId) {
        MwEntry entry = entryRepository.findByRecordIdFull(recordId)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + recordId));

        return buildFullEntry(entry);
    }

    /**
     * Получить список слов по ключу поиска (неточное совпадение)
     */
    public List<MwWordSearchDto> findWordsByKey1Normalized(String normalizedQuery) {
        List<SanskritWordSearchResult> words = entryRepository.findWordsByKey1NormalizedSimilarity(normalizedQuery);
        return words.stream().map(this::mapToMwWordSearchDto)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Получить статью по ключу поиска (точное совпадение)
     */
    @Transactional(readOnly = true)
    public List<MwDictionaryEntryDto> getEntriesByKey1(String key1) {
        String slp1RemoveStess = transliterationService.slp1RemoveStress(key1);

        List<MwEntry> entries = entryRepository.findByKey1Normalized(slp1RemoveStess);

        List<MwEntry> collect = entries.stream().sorted(Comparator.comparing(MwEntry::getECode))
                .collect(Collectors.toUnmodifiableList());

        return collect.stream()
                .map(this::buildFullEntry)
                .collect(Collectors.toList());
    }

    /**
     * Основной метод сборки полной статьи
     */
    private MwDictionaryEntryDto buildFullEntry(MwEntry entry) {
        Integer entryId = entry.getId();

        // Собираем все данные
        List<MwSanskritWord> sanskritWords = sanskritWordRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwHomonym> homonyms = homonymRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwAbbreviation> abbreviations = abbreviationRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwLiterarySource> literarySources = literarySourceRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwInfo> infoTags = infoRepository.findByEntryId(entryId);
        List<MwLexicalInfo> lexicalInfos = lexicalInfoRepository.findByEntryId(entryId);

        // Формируем заголовок для отображения
        String displayTitle = buildDisplayTitle(entry);

        String mainTranslation = xmlTranslationExtractor.extractTranslation(entry);

        return MwDictionaryEntryDto.builder()
                // Основная информация
                .recordId(entry.getRecordIdFull())
                .key1(entry.getKey1())
                .key1Display(displayTitle)
                .key1Iast(entry.getKey1Iast())
                .key2(entry.getKey2())
                .homonymNum(entry.getHomonymNum())
                .eCode(entry.getECode())
                .page(entry.getPage())
                .columnNum(entry.getColumnNum())
                .isSupplement(entry.getIsSupplement())
                .mainTranslation(mainTranslation)
                .displayTitle(displayTitle)
                .rawBody(entry.getBody())

                // Дочерние данные
                .lexicalInfo(mapLexicalInfo(lexicalInfos))
                .sanskritWords(mapSanskritWords(sanskritWords))
                .homonyms(mapHomonyms(homonyms))
                .abbreviations(mapAbbreviations(abbreviations))
                .literarySources(mapLiterarySources(literarySources))
                .infoTags(mapInfoTags(infoTags))

                .build();
    }

    /**
     * Собирает красивый заголовок для отображения
     */
    private String buildDisplayTitle(MwEntry entry) {
        StringBuilder title = new StringBuilder();

        // Основное слово
        String displayWord = entry.getKey1();

        // Если есть альтернативное написание (key2), добавляем его
        if (entry.getKey2() != null && !entry.getKey2().isEmpty()
                && !entry.getKey2().equals(entry.getKey1())) {
            title.append(entry.getKey2());
            title.append(" → ");
        }

        title.append(displayWord);

        // Добавляем номер омонима, если есть
        if (entry.getHomonymNum() != null && !entry.getHomonymNum().isEmpty()) {
            title.append(" (Hom. ").append(entry.getHomonymNum()).append(")");
        }

        // Добавляем e-code, если это подзапись
        if (entry.getECode() != null && !entry.getECode().equals("1")
                && !entry.getECode().equals("1A")) {
            title.append(" [").append(entry.getECode()).append("]");
        }

        return title.toString();
    }

    // =============================================
    // Методы маппинга сущностей в DTO
    // =============================================

    private List<MwDictionaryEntryDto.LexicalInfoDto> mapLexicalInfo(List<MwLexicalInfo> infos) {
        return infos.stream()
                .map(info -> MwDictionaryEntryDto.LexicalInfoDto.builder()
                        .lexType(info.getLexType())
                        .genderStandard(info.getGenderStandard())
                        .genderRaw(info.getGenderRaw())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MwDictionaryEntryDto.SanskritWordDto> mapSanskritWords(List<MwSanskritWord> words) {
        return words.stream()
                .map(word -> MwDictionaryEntryDto.SanskritWordDto.builder()
                        .slp1Spelling(word.getSlp1Spelling())
                        .iastSpelling(word.getIastSpelling())
                        .isPrimaryHeadword(word.getIsPrimaryHeadword())
                        .positionOrder(word.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MwDictionaryEntryDto.HomonymDto> mapHomonyms(List<MwHomonym> homonyms) {
        return homonyms.stream()
                .map(h -> MwDictionaryEntryDto.HomonymDto.builder()
                        .homonymNumber(h.getHomonymNumber())
                        .homonymText(h.getHomonymText())
                        .positionOrder(h.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MwDictionaryEntryDto.AbbreviationDto> mapAbbreviations(List<MwAbbreviation> abbrevs) {
        return abbrevs.stream()
                .map(abbr -> MwDictionaryEntryDto.AbbreviationDto.builder()
                        .abbrevText(abbr.getAbbrevText())
                        .expansion(abbr.getExpansion())
                        .slp1Spelling(abbr.getSlp1Spelling())
                        .positionOrder(abbr.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MwDictionaryEntryDto.LiterarySourceDto> mapLiterarySources(List<MwLiterarySource> sources) {
        return sources.stream()
                .map(src -> MwDictionaryEntryDto.LiterarySourceDto.builder()
                        .sourceRef(src.getSourceRef())
                        .positionOrder(src.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MwDictionaryEntryDto.InfoTagDto> mapInfoTags(List<MwInfo> infos) {
        return infos.stream()
                .map(info -> MwDictionaryEntryDto.InfoTagDto.builder()
                        .infoType(info.getInfoType())
                        .infoValue(info.getInfoValue())
                        .verbCp(info.getVerbCp())
                        .verbParse(info.getVerbParse())
                        .westergaardRoot(info.getWestergaardRoot())
                        .westergaardSection(info.getWestergaardSection())
                        .westergaardSayanaRef(info.getWestergaardSayanaRef())
                        .whitneyRoot(info.getWhitneyRoot())
                        .whitneyPage(info.getWhitneyPage())
                        .build())
                .collect(Collectors.toList());
    }

    private MwWordSearchDto mapToMwWordSearchDto(SanskritWordSearchResult mwSanskritWord) {
        // Fetch the main headword (key1) from the associated MwEntry

        return MwWordSearchDto.builder()
                .slp1Spelling(mwSanskritWord.getSlp1Spelling())
                .slp1Normalized(mwSanskritWord.getSlp1Normalized())
                .iastSpelling(mwSanskritWord.getIastSpelling())
                .similarity(mwSanskritWord.getSimilarity())
                .build();
    }

}
