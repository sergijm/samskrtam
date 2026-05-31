package sm.selflearn.samskrtam.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users", schema = "users")
public class User {

    @Id
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
