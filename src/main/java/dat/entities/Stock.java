package dat.entities;

import dat.dtos.StockDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id", nullable = false)
    private Long id;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_unit_id", nullable = false)
    private StockUnit stockUnit;

    @OneToOne(mappedBy = "stock", fetch = FetchType.EAGER)
    private Product product;

    public Stock(StockDTO stockDTO) {
        this.id = stockDTO.getId();
        this.quantity = stockDTO.getQuantity();
        this.stockUnit = new StockUnit(stockDTO.getStockUnit());
    }

}
