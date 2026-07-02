package sm.selflearn.samskrtam.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordAnswerHistory {
    private UUID wordId;
    private String word;
    private UUID lessonId;
    private List<AnswerHistoryEntry> entries;
    private int page;
    private int size;
    private int total;
}
