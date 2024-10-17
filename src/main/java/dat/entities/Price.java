package dat.entities;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "prices")
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

    // Getters and Setters
}
