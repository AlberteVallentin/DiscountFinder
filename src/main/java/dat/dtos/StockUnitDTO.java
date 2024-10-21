package dat.dtos;

import dat.enums.UnitType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StockUnitDTO {

    private Long id;
    private UnitType unitType;
}

