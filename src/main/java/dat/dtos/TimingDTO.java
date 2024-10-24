package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import dat.entities.Timing;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimingDTO {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime lastUpdated;

    public TimingDTO(Timing timing) {
        this.id = timing.getId();
        this.startTime = timing.getStartTime();
        this.endTime = timing.getEndTime();
        this.lastUpdated = timing.getLastUpdated();
    }
}