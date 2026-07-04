package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;


import java.time.Instant;
import java.util.UUID;

import static sm.selflearn.samskrtam.sangraha.service.VerseAnalysisSaver.getString;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerseAnalysisService {

    private final VerseRepository verseRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final LlmClient llmClient;
    private final VerseAnalysisSaver analysisSaver;

    public void analyze(UUID verseId) {
        Verse verse = verseRepository.findByIdAndDeletedAtIsNull(verseId)
                .orElseThrow(() -> new IllegalArgumentException("Verse not found: " + verseId));

        verse.setStatus(VerseStatus.ANALYZING);
        verse.setUpdatedAt(Instant.now());
        verseRepository.save(verse);

        Chapter chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId())
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + verse.getChapterId()));
        Work work = workRepository.findById(chapter.getWorkId())
                .orElseThrow(() -> new IllegalArgumentException("Work not found: " + chapter.getWorkId()));

        JsonNode llmResponse;
        try {
            llmResponse = llmClient.call(verse);
        } catch (Exception e) {
            log.error("LLM analysis failed for verse {}", verseId, e);
            analysisSaver.markFailed(verse);
            return;
        }

        if (llmResponse == null) {
            log.error("LLM returned null response for verse {}", verseId);
            analysisSaver.markFailed(verse);
            return;
        }

        JsonNode arguments = llmClient.extractToolArguments(llmResponse);
        if (arguments == null) {
            log.error("LLM did not return submit_verse_analysis tool call for verse {}", verseId);
            analysisSaver.markFailed(verse);
            return;
        }

        String textDevanagari = getString(arguments, "textDevanagari");
        String textIast = getString(arguments, "textIast");
        String translationRu = getString(arguments, "translationRu");
        String translationEn = getString(arguments, "translationEn");
        JsonNode sandhiSplitsNode = arguments.get("sandhiSplits");
        JsonNode wordsNode = arguments.get("words");

        if (textDevanagari == null || textIast == null || translationRu == null || translationEn == null
                || sandhiSplitsNode == null || !sandhiSplitsNode.isArray()
                || wordsNode == null || !wordsNode.isArray()) {
            log.error("Invalid tool call arguments for verse {}: missing required fields", verseId);
            analysisSaver.markFailed(verse);
            return;
        }

        String modelName = llmClient.extractModelName(llmResponse);

        try {
            analysisSaver.saveResults(verse, work, chapter,
                    textDevanagari, textIast, translationRu, translationEn,
                    sandhiSplitsNode, wordsNode, llmResponse.toString(), modelName);
        } catch (Exception e) {
            log.error("Failed to save analysis results for verse {}", verseId, e);
            analysisSaver.markFailed(verse);
        }
    }
}