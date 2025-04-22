package alexspeal.entities;

import alexspeal.enums.AcceptStatusEvent;
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

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Entity
@Data
@Table(name="events")
public class EventEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AcceptStatusEvent status;

    @Column(name = "description")
    private String description;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title")
    private String title;
    @Column(name = "is_personal")
    private Boolean isPersonal;
    @Column(name = "start_time")
    private LocalDateTime startTime;
    @Column(name = "duration")
    private Integer duration;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventParticipantEntity> eventParticipants;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    public EventEntity(String title, String description, AcceptStatusEvent status,
                       LocalDateTime startTime, Integer duration, UserEntity author, boolean isPersonal) {
        super();
        this.title = title;
        this.description = description;
        this.status = status;
        this.startTime = startTime;
        this.duration = duration;
        this.author = author;
        this.isPersonal = isPersonal;
        this.createdAt = LocalDateTime.now();
    }
}