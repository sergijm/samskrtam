package sm.selflearn.samskrtam.sangraha.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerseStatusStartupFixer implements ApplicationRunner {

    private final VerseRepository verseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = verseRepository.resetStatusByCurrentStatus(VerseStatus.ANALYZING, VerseStatus.DRAFT);
        if (updated > 0) {
            log.info("Reset {} verses from ANALYZING to DRAFT on startup", updated);
        }
    }
}