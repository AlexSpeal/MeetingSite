package alexspeal.entities;

import alexspeal.enums.AcceptStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@Entity
@Data
@Table(name="events")
public class EventEntity {

    @Column(name = "description")
    private String description;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AcceptStatus status;
    @Column(name = "start_time")
    private LocalDateTime startTime;
    @Column(name = "duration")
    private Integer duration;
    @Column(name = "possible_days", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<LocalDate> possibleDays;

    public EventEntity(String title, String description, AcceptStatus status,
                       LocalDateTime startTime, Integer duration,
                       List<LocalDate> possibleDays, UserEntity author) {
        super();
        this.title = title;
        this.description = description;
        this.status = status;
        this.startTime = startTime;
        this.duration = duration;
        this.possibleDays = possibleDays;
        this.author = author;
    }

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<EventParticipantEntity> eventParticipants;
}