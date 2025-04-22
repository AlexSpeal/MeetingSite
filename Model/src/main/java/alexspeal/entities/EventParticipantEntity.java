package alexspeal.entities;

import alexspeal.enums.AcceptStatusParticipant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_participants")
public class EventParticipantEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AcceptStatusParticipant status;
    @OneToMany(mappedBy = "eventParticipant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DayEntity> days;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public EventParticipantEntity(EventEntity event, UserEntity user, AcceptStatusParticipant status) {
        super();
        this.event = event;
        this.user = user;
        this.status = status;
    }
}
