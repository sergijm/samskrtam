package sm.selflearn.samskrtam.apte.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.apte.dto.ApteEntryDto;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ApteRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL =
            "SELECT id, headword_devanagari, body_text, raw_markup, homonym_num "
            + "FROM cologne_apte.entries WHERE k1_slp1 = ? "
            + "ORDER BY homonym_num NULLS FIRST, id";

    private static final RowMapper<ApteEntryDto> MAPPER = (rs, rowNum) -> ApteEntryDto.builder()
            .id(rs.getLong("id"))
            .headwordDevanagari(rs.getString("headword_devanagari"))
            .bodyText(rs.getString("body_text"))
            .rawMarkup(rs.getString("raw_markup"))
            .homonymNum(rs.getObject("homonym_num", Integer.class))
            .build();

    public List<ApteEntryDto> findByK1Slp1(String k1Slp1) {
        return jdbcTemplate.query(SQL, MAPPER, k1Slp1);
    }
}
