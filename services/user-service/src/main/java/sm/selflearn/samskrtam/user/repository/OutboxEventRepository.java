package sm.selflearn.samskrtam.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'PENDING' ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
}
