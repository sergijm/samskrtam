package sm.selflearn.samskrtam.statistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {


    @Transactional(readOnly = true)
    public Page<UserQuizSessionStatistic> getUserQuizStatistics(UUID userId, Pageable pageable) {
        return Page.empty();
    }
}
