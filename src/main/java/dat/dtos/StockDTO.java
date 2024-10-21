package dat.dtos;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StockDTO {

    private Long id;
    private BigDecimal quantity;
    private StockUnitDTO stockUnit;
}