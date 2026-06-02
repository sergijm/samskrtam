package sm.selflearn.samskrtam.content.model;

import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class DeclensionFormId implements Serializable {
    private UUID declensionStemId;
    private Case caseType;
    private Number numberType;
}
