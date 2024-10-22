package dat.dtos;

import dat.entities.Price;
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

    public PriceDTO(Price price) {
        this.id = price.getId();
        this.originalPrice = price.getOriginalPrice();
        this.newPrice = price.getNewPrice();
        this.discount = price.getDiscount();
        this.percentDiscount = price.getPercentDiscount();
    }
}
