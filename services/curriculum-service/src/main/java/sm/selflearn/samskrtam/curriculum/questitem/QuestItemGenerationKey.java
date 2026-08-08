package sm.selflearn.samskrtam.curriculum.questitem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Idempotency key of a batch-generated quest item: prevents the generator from
 * recreating the same item for the same (topic, itemType, lexeme, case, number).
 * See curriculum-quest-items.md §1.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quest_item_generation_key", schema = "curriculum")
public class QuestItemGenerationKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quest_item_id", nullable = false)
    private UUID questItemId;

    @Column(name = "generation_key", nullable = false, length = 200)
    private String generationKey;
}