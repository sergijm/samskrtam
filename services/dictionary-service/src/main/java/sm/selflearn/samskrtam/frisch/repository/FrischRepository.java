package sm.selflearn.samskrtam.frisch.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FrischRepository {

    private static final String GET_LEMMA_JSON_SQL = "SELECT frisch.get_lemma_json(?)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<FrischEntryDto> getLemmaJson(String lemma) {
        String json = jdbcTemplate.queryForObject(
                GET_LEMMA_JSON_SQL,
                (rs, rowNum) -> rs.getString(1),
                lemma);

        if (json == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FrischEntryDto.class));
        } catch (Exception e) {
            log.error("Failed to parse frisch.get_lemma_json result for lemma '{}'", lemma, e);
            throw new IllegalStateException("Failed to parse frisch lemma json", e);
        }
    }
}
