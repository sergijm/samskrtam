package sm.selflearn.samskrtam.curriculum.questitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestItemRepository extends JpaRepository<QuestItem, UUID> {

    /**
     * Selects one quest item per (progress_tag, item_type, answer_mode) group
     * using a window function — the first random row per partition.
     * Used when no progress_tag collection is requested (full lesson).
     *
     * @param limit 0 = no limit (returns all distinct groups)
     */
    @Query(value = """
            SELECT * FROM (
              SELECT *,
                row_number() OVER (PARTITION BY progress_tag, item_type, answer_mode ORDER BY random()) AS rn
              FROM curriculum.quest_item
              WHERE topic_id = :topicId
                AND (:itemType IS NULL OR item_type = :itemType)
                AND (:answerMode IS NULL OR answer_mode = :answerMode)
                AND progress_tag IS NOT NULL
            ) sub WHERE sub.rn = 1
            ORDER BY random()
            LIMIT NULLIF(:limit, 0)
            """, nativeQuery = true)
    List<QuestItem> selectByTopic(
            @Param("topicId") UUID topicId,
            @Param("itemType") String itemType,
            @Param("answerMode") AnswerMode answerMode,
            @Param("limit") int limit);

    /**
     * Same as {@link #selectByTopic} but filters by a collection of progress tags.
     * Used for grammar-set and status-set selection (progressTagSetId).
     */
    @Query(value = """
            SELECT * FROM (
              SELECT *,
                row_number() OVER (PARTITION BY progress_tag, item_type, answer_mode ORDER BY random()) AS rn
              FROM curriculum.quest_item
              WHERE topic_id = :topicId
                AND progress_tag = ANY(:progressTags)
                AND (:itemType IS NULL OR item_type = :itemType)
                AND (:answerMode IS NULL OR answer_mode = :answerMode)
                AND progress_tag IS NOT NULL
            ) sub WHERE sub.rn = 1
            ORDER BY random()
            LIMIT NULLIF(:limit, 0)
            """, nativeQuery = true)
    List<QuestItem> selectByTopicAndProgressTags(
            @Param("topicId") UUID topicId,
            @Param("progressTags") String[] progressTags,
            @Param("itemType") String itemType,
            @Param("answerMode") AnswerMode answerMode,
            @Param("limit") int limit);

    /**
     * Random sample of ready-made quest items for a topic/type. Non-deterministic
     * ordering across calls (PostgreSQL {@code random()}) — see
     * curriculum-quest-items.md §5 (fetch for a session start).
     */
    @Query(value = "SELECT * FROM curriculum.quest_item "
            + "WHERE topic_id = :topicId AND item_type = :itemType "
            + "ORDER BY random() LIMIT :limit", nativeQuery = true)
    List<QuestItem> findRandomByTopicIdAndItemType(
            @Param("topicId") UUID topicId,
            @Param("itemType") String itemType,
            @Param("limit") int limit);

    /**
     * Random sample of ready-made quest items for a topic across all materialized
     * item types (random ordering via {@code ORDER BY random()}). Used by session
     * composition: the caller asks for {@code count} items per topic without
     * picking an item type — types are mixed proportionally to the pool.
     */
    @Query(value = "SELECT * FROM curriculum.quest_item "
            + "WHERE topic_id = :topicId "
            + "ORDER BY random() LIMIT :limit", nativeQuery = true)
    List<QuestItem> findRandomByTopicId(
            @Param("topicId") UUID topicId,
            @Param("limit") int limit);

    /**
     * All ready-made quest items of a topic (across item types). Used by quiz-service
     * for progress-aware selection (due/new/reserve) on the full pool before asking
     * curriculum-service to compose a session.
     */
    @Query("select qi from QuestItem qi where qi.topicId = :topicId")
    List<QuestItem> findByTopicId(@Param("topicId") UUID topicId);

    /**
     * Removes the already materialized items of a topic/type before regeneration.
     * The related {@code quest_item_generation_key} rows are removed by the DB-level
     * ON DELETE CASCADE on {@code quest_item_generation_key.quest_item_id}.
     *
     * @return number of removed quest items
     */
    @Modifying
    @Transactional
    @Query("delete from QuestItem qi where qi.topicId = :topicId and qi.itemType = :itemType")
    int deleteByTopicIdAndItemType(@Param("topicId") UUID topicId, @Param("itemType") String itemType);

    /**
     * Clears the whole {@code quest_item} table before a full regeneration. The
     * related {@code quest_item_generation_key} rows are removed by the DB-level
     * ON DELETE CASCADE on {@code quest_item_generation_key.quest_item_id}.
     *
     * @return number of removed quest items
     */
    @Modifying
    @Transactional
    @Query("delete from QuestItem qi")
    int deleteAllQuestItems();

    /**
     * Number of distinct {@code progress_tag}s of a single topic (rows without
     * a tag are ignored).
     */
    @Query("select count(distinct qi.progressTag) from QuestItem qi where qi.topicId = :topicId and qi.progressTag is not null")
    long countDistinctProgressTagByTopicId(@Param("topicId") UUID topicId);
}
