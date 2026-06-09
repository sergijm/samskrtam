package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // New import
import lombok.AllArgsConstructor; // New import

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor // Added for Jackson deserialization
@AllArgsConstructor // Added for Jackson deserialization
public class VocabularyWordDto {
    private UUID id;
    private String wordIast;
    private String wordDevanagari;
    private String translationEn;
    private String translationRu;
    private Gender gender;
    private String stem;
    private String root;
    private String dictionaryEntry;
}
