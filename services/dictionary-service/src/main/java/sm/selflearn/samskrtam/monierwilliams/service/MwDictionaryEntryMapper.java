package sm.selflearn.samskrtam.monierwilliams.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.monierwilliams.entity.*;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper для преобразования сущностей Monier-Williams словаря в DTO.
 * Содержит только stateless методы маппинга.
 */
@Component
public class MwDictionaryEntryMapper {

    public List<MwDictionaryEntryDto.LexicalInfoDto> mapLexicalInfo(List<MwLexicalInfo> infos) {
        return infos.stream()
                .map(info -> MwDictionaryEntryDto.LexicalInfoDto.builder()
                        .lexType(info.getLexType())
                        .genderStandard(info.getGenderStandard())
                        .genderRaw(info.getGenderRaw())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MwDictionaryEntryDto.SanskritWordDto> mapSanskritWords(List<MwSanskritWord> words) {
        return words.stream()
                .map(word -> MwDictionaryEntryDto.SanskritWordDto.builder()
                        .slp1Spelling(word.getSlp1Spelling())
                        .iastSpelling(word.getIastSpelling())
                        .isPrimaryHeadword(word.getIsPrimaryHeadword())
                        .positionOrder(word.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MwDictionaryEntryDto.HomonymDto> mapHomonyms(List<MwHomonym> homonyms) {
        return homonyms.stream()
                .map(h -> MwDictionaryEntryDto.HomonymDto.builder()
                        .homonymNumber(h.getHomonymNumber())
                        .homonymText(h.getHomonymText())
                        .positionOrder(h.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MwDictionaryEntryDto.AbbreviationDto> mapAbbreviations(List<MwAbbreviation> abbrevs) {
        return abbrevs.stream()
                .map(abbr -> MwDictionaryEntryDto.AbbreviationDto.builder()
                        .abbrevText(abbr.getAbbrevText())
                        .expansion(abbr.getExpansion())
                        .slp1Spelling(abbr.getSlp1Spelling())
                        .positionOrder(abbr.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MwDictionaryEntryDto.LiterarySourceDto> mapLiterarySources(List<MwLiterarySource> sources) {
        return sources.stream()
                .map(src -> MwDictionaryEntryDto.LiterarySourceDto.builder()
                        .sourceRef(src.getSourceRef())
                        .positionOrder(src.getPositionOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MwDictionaryEntryDto.InfoTagDto> mapInfoTags(List<MwInfo> infos) {
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

    public MwWordSearchDto mapToMwWordSearchDto(SanskritWordSearchResult word) {
        return MwWordSearchDto.builder()
                .slp1Spelling(word.getSlp1Spelling())
                .slp1Normalized(word.getSlp1Normalized())
                .iastSpelling(word.getIastSpelling())
                .similarity(word.getSimilarity())
                .build();
    }
}
