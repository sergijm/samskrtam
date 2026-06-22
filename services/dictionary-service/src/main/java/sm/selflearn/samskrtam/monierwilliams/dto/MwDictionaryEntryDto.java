package sm.selflearn.samskrtam.monierwilliams.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MwDictionaryEntryDto {

    // Основная информация
    private String recordId;
    private String key1;           // SLP1 с ударениями
    private String key1Iast;           // Iast
    private String key1Display;    // для отображения пользователю
    private String key2;           // альтернативное написание
    private String homonymNum;     // номер омонима
    private String eCode;          // H1, H1A, H2 и т.д.
    private Integer page;
    private Integer columnNum;
    private Boolean isSupplement;
    private String mainTranslation;


    // Грамматическая информация
    private List<LexicalInfoDto> lexicalInfo;

    // Санскритские слова в статье
    private List<SanskritWordDto> sanskritWords;

    // Гомонимы
    private List<HomonymDto> homonyms;

    // Аббревиатуры
    private List<AbbreviationDto> abbreviations;

    // Литературные источники
    private List<LiterarySourceDto> literarySources;

    // Info-теги
    private List<InfoTagDto> infoTags;

    // Полный текст статьи (для отладки или резервного отображения)
    private String rawBody;

    // Вспомогательные поля для отображения
    private String displayTitle;    // сформированный заголовок

    @Data
    @Builder
    public static class LexicalInfoDto {
        private String lexType;         // hw, hwalt, hwifc и т.д.
        private String genderStandard;  // m:f:n, ind, inh
        private String genderRaw;       // m., f., n.
    }

    @Data
    @Builder
    public static class SanskritWordDto {
        private String slp1Spelling;
        private String iastSpelling;
        private Boolean isPrimaryHeadword;
        private Integer positionOrder;
    }

    @Data
    @Builder
    public static class HomonymDto {
        private String homonymNumber;
        private String homonymText;
        private Integer positionOrder;
    }

    @Data
    @Builder
    public static class AbbreviationDto {
        private String abbrevText;
        private String expansion;
        private String slp1Spelling;
        private Integer positionOrder;
    }

    @Data
    @Builder
    public static class LiterarySourceDto {
        private String sourceRef;
        private Integer positionOrder;
    }

    @Data
    @Builder
    public static class InfoTagDto {
        private String infoType;
        private String infoValue;
        private String verbCp;
        private String verbParse;
        private String westergaardRoot;
        private String westergaardSection;
        private String westergaardSayanaRef;
        private String whitneyRoot;
        private String whitneyPage;
    }
}
