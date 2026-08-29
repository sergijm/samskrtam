package sm.selflearn.samskrtam.curriculum.questgen;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the DECLENSION_MATCH quest type: how many pairs make up a
 * single matching item. Read from {@code curriculum.quest-items.declension-match}.
 * See curriculum-quest-items.md §4.
 */
@ConfigurationProperties(prefix = "curriculum.quest-items.declension-match")
public class DeclensionMatchProperties {

    private int pairsPerItem = 6;

    public int getPairsPerItem() {
        return pairsPerItem;
    }

    public void setPairsPerItem(int pairsPerItem) {
        this.pairsPerItem = pairsPerItem;
    }
}