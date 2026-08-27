package sm.selflearn.samskrtam.apte.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.apte.ApteHtmlFormatter;
import sm.selflearn.samskrtam.apte.dto.ApteEntryDto;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ApteRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL =
            "SELECT id, headword_devanagari, body_text, raw_markup, homonym_num "
            + "FROM cologne_apte.entries WHERE k1_slp1 = ? "
            + "ORDER BY homonym_num NULLS FIRST, id";

    private static final String SQL_BY_IDS =
            "SELECT id, headword_devanagari, body_text, raw_markup, homonym_num "
            + "FROM cologne_apte.entries WHERE id = ANY(?) "
            + "ORDER BY homonym_num NULLS FIRST, id";

    private static final RowMapper<ApteEntryDto> MAPPER = (rs, rowNum) -> {
        String rawMarkup = rs.getString("raw_markup");
        return ApteEntryDto.builder()
                .id(rs.getLong("id"))
                .headwordDevanagari(rs.getString("headword_devanagari"))
                .bodyText(rs.getString("body_text"))
                .rawMarkup(rawMarkup)
                .homonymNum(rs.getObject("homonym_num", Integer.class))
                .html(rawMarkup != null ? ApteHtmlFormatter.format(rawMarkup) : null)
                .build();
    };

    public List<ApteEntryDto> findByK1Slp1(String k1Slp1) {
        return jdbcTemplate.query(SQL, MAPPER, k1Slp1);
    }

    public List<ApteEntryDto> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        PreparedStatementSetter setter = (PreparedStatement ps) -> {
            java.sql.Array arr = ps.getConnection().createArrayOf("bigint", ids.toArray());
            ps.setArray(1, arr);
        };
        return jdbcTemplate.query(SQL_BY_IDS, setter, MAPPER);
    }
}
