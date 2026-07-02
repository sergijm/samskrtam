package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final WorkService workService;

    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByWorkId(UUID workId) {
        workService.getWorkById(workId); // validate work exists
        return chapterRepository.findAllByWorkIdAndDeletedAtIsNullOrderByOrderIndexAsc(workId);
    }

    @Transactional(readOnly = true)
    public Chapter getChapterById(UUID id) {
        return chapterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + id));
    }

    @Transactional
    public Chapter createChapter(UUID workId, Chapter chapter) {
        workService.getWorkById(workId); // validate work exists
        if (chapterRepository.existsByWorkIdAndSlug(workId, chapter.getSlug())) {
            throw new RuntimeException("Chapter with slug '" + chapter.getSlug() + "' already exists in this work");
        }
        chapter.setWorkId(workId);
        return chapterRepository.save(chapter);
    }

    @Transactional
    public Chapter updateChapter(UUID id, Chapter update) {
        Chapter chapter = getChapterById(id);
        chapter.setTitleRu(update.getTitleRu());
        chapter.setTitleEn(update.getTitleEn());
        chapter.setOrderIndex(update.getOrderIndex());
        return chapterRepository.save(chapter);
    }

    @Transactional
    public void deleteChapter(UUID id) {
        Chapter chapter = getChapterById(id);
        chapter.setDeletedAt(Instant.now());
        chapterRepository.save(chapter);
    }
}