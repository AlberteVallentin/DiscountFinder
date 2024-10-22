package dat.entities;

import dat.dtos.TimingDTO;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "timings")
public class Timing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timing_id", nullable = false)
    private Long id;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @OneToOne(mappedBy = "timing")
    private Product product;

    public Timing(TimingDTO dto) {
        updateFromDTO(dto);
    }

    public void updateFromDTO(TimingDTO dto) {
        this.startTime = dto.getStartTime();
        this.endTime = dto.getEndTime();
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null &&
            now.isAfter(startTime) && now.isBefore(endTime);
    }
}