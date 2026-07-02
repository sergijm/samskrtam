package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class VerseAnalyzeController {

    private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;
    private final VerseWordRepository verseWordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;

    @PostMapping("/verses/{verseId}/analyze")
    public ResponseEntity<Void> analyzeVerse(@PathVariable UUID verseId) {
        // stub: placeholder для LLM-анализа через tool calling (§5 sangraha-service.md)
        // 1. Читаем Verse, проверяем статус
        // 2. Ставим ANALYZING
        // 3. Вызываем LLM с one tool (submit_verse_analysis)
        // 4. Валидируем tool_calls[0].function.arguments
        // 5. В транзакции: обновляем Verse.textDevanagari/textIast, пишем VerseAnalysis (перезапись),
        //    пересоздаём VerseWord[], Verse.status → ANALYZED
        // 6. Пишем OutboxEvent(VERSE_VOCABULARY_EXTRACTED) в той же транзакции
        // 7. При ошибке → status = FAILED
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}