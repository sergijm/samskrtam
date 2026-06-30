package sm.selflearn.samskrtam.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclensionStemDto {
    private UUID id;
    private UUID lessonId;
    private String slug;
}