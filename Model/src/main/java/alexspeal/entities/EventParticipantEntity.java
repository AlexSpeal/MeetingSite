package alexspeal.entities;

import alexspeal.enums.AcceptStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_participants")
public class EventParticipantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AcceptStatus status;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "selected_days", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<LocalDate> selectedDays;


    public EventParticipantEntity(EventEntity event, UserEntity user, AcceptStatus status) {
        super();
        this.event = event;
        this.user = user;
        this.status = status;
    }
}
