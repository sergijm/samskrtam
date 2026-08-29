package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Предвычисленная статистика стиха (sangraha-service.md §9): длина стиха
 * в словах (wordCount) и грамматический состав (grammarInfo) — distinct-списки
 * частей речи, форм, чисел, падежей и родов, встречающихся в стихе. Одна строка
 * на стих (PK = verse_id), 1:1 с {@link Verse}. Пересчитывается на POST
 * /sangraha/internal/lexicon/lemmas/refresh-statistics
 * ({@code LemmaRefreshService}) — нативный upsert по PK.
 */
@Entity
@Table(name = "verse_statistics", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseStatistics {

    @Id
    @Column(name = "verse_id")
    private UUID verseId;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    /**
     * distinct-списки грамматического набора стиха, JSON вида
     * {@code {"pos": [...], "formType": [...], "numberType": [...], "caseType": [...], "gender": [...], "tuples": [[...],...]}}.
     * Поле {@code tuples} — массив кортежей {@code [stemClass, gender, case, number]} (по слову), для фильтрации через {@code @>}.
     * Индексирован GIN.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grammar_info", columnDefinition = "JSONB", nullable = false)
    private String grammarInfo;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}