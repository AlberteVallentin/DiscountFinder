package dat.entities;

import dat.dtos.PriceDTO;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id", nullable = false)
    private Long id;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "new_price", nullable = false)
    private BigDecimal newPrice;

    @Column(name = "discount", nullable = false)
    private BigDecimal discount;

    @Column(name = "percent_discount", nullable = false)
    private BigDecimal percentDiscount;

    // One-to-One relation with Product (Product owns the relation)
    @OneToOne(mappedBy = "price", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Product product;

    public Price(PriceDTO priceDTO) {
        this.id = priceDTO.getId();
        this.originalPrice = priceDTO.getOriginalPrice();
        this.newPrice = priceDTO.getNewPrice();
        this.discount = priceDTO.getDiscount();
        this.percentDiscount = priceDTO.getPercentDiscount();
    }

}


