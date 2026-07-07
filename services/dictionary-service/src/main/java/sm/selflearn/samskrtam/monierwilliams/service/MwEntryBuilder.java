package sm.selflearn.samskrtam.monierwilliams.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.entity.*;
import sm.selflearn.samskrtam.monierwilliams.repository.*;

import java.util.List;

/**
 * Собирает полную словарную статью (MwDictionaryEntryDto) из связанных сущностей.
 */
@Component
@RequiredArgsConstructor
public class MwEntryBuilder {

    private final MwSanskritWordRepository sanskritWordRepository;
    private final MwHomonymRepository homonymRepository;
    private final MwAbbreviationRepository abbreviationRepository;
    private final MwLiterarySourceRepository literarySourceRepository;
    private final MwInfoRepository infoRepository;
    private final MwLexicalInfoRepository lexicalInfoRepository;
    private final MwXmlTranslationExtractor xmlTranslationExtractor;
    private final MwDictionaryEntryMapper mapper;

    /**
     * Собирает полную статью из корневой сущности MwEntry.
     */
    public MwDictionaryEntryDto build(MwEntry entry) {
        Integer entryId = entry.getId();

        List<MwSanskritWord> sanskritWords = sanskritWordRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwHomonym> homonyms = homonymRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwAbbreviation> abbreviations = abbreviationRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwLiterarySource> literarySources = literarySourceRepository.findByEntryIdOrderByPositionOrder(entryId);
        List<MwInfo> infoTags = infoRepository.findByEntryId(entryId);
        List<MwLexicalInfo> lexicalInfos = lexicalInfoRepository.findByEntryId(entryId);

        String displayTitle = buildDisplayTitle(entry);
        String mainTranslation = xmlTranslationExtractor.extractTranslation(entry);

        return MwDictionaryEntryDto.builder()
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
                .lexicalInfo(mapper.mapLexicalInfo(lexicalInfos))
                .sanskritWords(mapper.mapSanskritWords(sanskritWords))
                .homonyms(mapper.mapHomonyms(homonyms))
                .abbreviations(mapper.mapAbbreviations(abbreviations))
                .literarySources(mapper.mapLiterarySources(literarySources))
                .infoTags(mapper.mapInfoTags(infoTags))
                .build();
    }

    /**
     * Собирает красивый заголовок для отображения
     */
    private String buildDisplayTitle(MwEntry entry) {
        StringBuilder title = new StringBuilder();

        String displayWord = entry.getKey1();

        if (entry.getKey2() != null && !entry.getKey2().isEmpty()
                && !entry.getKey2().equals(entry.getKey1())) {
            title.append(entry.getKey2());
            title.append(" \u2192 ");
        }

        title.append(displayWord);

        if (entry.getHomonymNum() != null && !entry.getHomonymNum().isEmpty()) {
            title.append(" (Hom. ").append(entry.getHomonymNum()).append(")");
        }

        if (entry.getECode() != null && !entry.getECode().equals("1")
                && !entry.getECode().equals("1A")) {
            title.append(" [").append(entry.getECode()).append("]");
        }

        return title.toString();
    }
}