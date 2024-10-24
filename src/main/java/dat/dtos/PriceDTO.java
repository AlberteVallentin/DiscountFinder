package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
