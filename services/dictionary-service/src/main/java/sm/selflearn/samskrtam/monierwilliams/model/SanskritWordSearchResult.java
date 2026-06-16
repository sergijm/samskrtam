package sm.selflearn.samskrtam.monierwilliams.model;

import jakarta.persistence.*;
import lombok.Data;


public interface SanskritWordSearchResult {

    String getSlp1Spelling();

    String getSlp1Normalized();

    String getIastSpelling();

    Boolean getIsPrimaryHeadword();

    Double getSimilarity();

}
