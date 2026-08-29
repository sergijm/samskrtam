package sm.selflearn.samskrtam.cae.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.cae.dto.CaeEntryDto;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CaeRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_BY_IDS =
            "SELECT cae_id, page, homonym_num, entry_variant, headword_plain, headword_accented, "
            + "raw_text, clean_text, gloss, grammar->'partsOfSpeech'->>0 AS grammar_pos "
            + "FROM cologne_cae.entries WHERE cae_id = ANY(?) "
            + "ORDER BY homonym_num NULLS FIRST, cae_id";

    private static final RowMapper<CaeEntryDto> MAPPER = (rs, rowNum) -> CaeEntryDto.builder()
            .id(rs.getLong("cae_id"))
            .page(rs.getObject("page", Integer.class))
            .homonymNum(rs.getObject("homonym_num", Integer.class))
            .entryVariant(rs.getString("entry_variant"))
            .headwordPlain(rs.getString("headword_plain"))
            .headwordAccented(rs.getString("headword_accented"))
            .rawText(rs.getString("raw_text"))
            .cleanText(rs.getString("clean_text"))
            .gloss(rs.getString("gloss"))
            .grammarPos(rs.getString("grammar_pos"))
            .build();

    public List<CaeEntryDto> findByIds(List<Long> ids) {
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
