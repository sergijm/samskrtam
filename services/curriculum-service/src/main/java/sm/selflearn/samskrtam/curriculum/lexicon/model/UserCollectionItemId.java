package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class UserCollectionItemId implements Serializable {
    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

    @Column(name = "lexeme_id", nullable = false)
    private UUID lexemeId;
}