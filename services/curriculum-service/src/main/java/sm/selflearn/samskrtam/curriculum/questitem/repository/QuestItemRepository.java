package sm.selflearn.samskrtam.curriculum.questitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestItemRepository extends JpaRepository<QuestItem, UUID> {

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
    @Query("delete from QuestItem qi where qi.topicId = :topicId")
    int deleteByTopicId(@Param("topicId") UUID topicId);

    @Modifying
    @Transactional
    @Query("delete from QuestItem qi where qi.topicId = :topicId and qi.itemType = :itemType")
    int deleteByTopicIdAndItemType(@Param("topicId") UUID topicId, @Param("itemType") String itemType);
}
