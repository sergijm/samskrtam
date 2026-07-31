package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.WorkTreeDto;
import sm.selflearn.samskrtam.sangraha.model.Work;

@Service
@RequiredArgsConstructor
public class WorkTreeService {

    private final WorkService workService;
    private final ChapterService chapterService;

    @Transactional(readOnly = true)
    public WorkTreeDto getWorkTreeBySlug(String workSlug) {
        Work work = workService.getWorkBySlug(workSlug);
        return new WorkTreeDto(
                work.getId(),
                work.getSlug(),
                work.getTitleRu(),
                work.getTitleEn(),
                work.getDescriptionRu(),
                work.getDescriptionEn(),
                work.getAuthor(),
                chapterService.getChapterSummaryByWorkId(work.getId())
        );
    }
}