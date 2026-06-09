package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized; // Import Jacksonized
import sm.selflearn.samskrtam.content.model.Gender; // Assuming Gender is in shared:dictionary-dtos

import java.util.List; // Import List
import java.util.UUID;

@Value
@Builder
@Jacksonized // Add Jacksonized annotation
public class VocabularyWordDto {
    UUID id;
    String wordIast;
    String wordDevanagari;
    String translationEn;
    String translationRu;
    Gender gender;
    String stem;
    String root;
    String explanationRu;
    String explanationEn;
    List<String> tags; // New field
}
