package sm.selflearn.samskrtam.mw.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.mw.MwHtmlRenderer;
import sm.selflearn.samskrtam.mw.dto.MwEntryDto;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MwRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String COLS =
            "id, entry_id, key1, key2, homonym, entry_no, page_col, body, grammar, clean_text";

    private static final String SQL_BY_K1 =
            "SELECT " + COLS + " FROM cologne_mw.entries WHERE key1 = ? ORDER BY id";

    private static final String SQL_BY_IDS =
            "SELECT " + COLS + " FROM cologne_mw.entries WHERE id = ANY(?) ORDER BY id";

    private static final RowMapper<MwEntryDto> MAPPER = (rs, rn) -> {
        String key1 = rs.getString("key1");
        String key2 = rs.getString("key2");
        String entryNo = rs.getString("entry_no");
        String pageCol = rs.getString("page_col");
        String entryId = rs.getString("entry_id");
        String homonym = rs.getString("homonym");
        String body = rs.getString("body");
        return MwEntryDto.builder()
                .id(rs.getLong("id"))
                .entryId(entryId)
                .key1(key1)
                .key2(key2)
                .homonym(homonym)
                .entryNo(entryNo)
                .pageCol(pageCol)
                .headwordDisplay(key2 != null ? key2 : key1)
                .body(body)
                .grammarJson(rs.getString("grammar"))
                .cleanText(rs.getString("clean_text"))
                .build();
    };

    public List<MwEntryDto> findByK1Slp1(String k1Slp1) {
        return jdbcTemplate.query(SQL_BY_K1, MAPPER, k1Slp1);
    }

    public List<MwEntryDto> findByIds(List<Long> ids) {
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
