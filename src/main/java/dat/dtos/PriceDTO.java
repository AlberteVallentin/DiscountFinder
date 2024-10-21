package dat.dtos;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PriceDTO {

    private Long id;
    private BigDecimal originalPrice;
    private BigDecimal newPrice;
    private BigDecimal discount;
    private BigDecimal percentDiscount;
}