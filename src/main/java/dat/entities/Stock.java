package dat.entities;

import dat.dtos.StockDTO;
import dat.enums.StockUnit;
import jakarta.persistence.*;
import lombok.*;

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
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_unit", nullable = false)
    private StockUnit stockUnit;

    @OneToOne(mappedBy = "stock")
    private Product product;

    public Stock(StockDTO dto) {
        updateFromDTO(dto);
    }

    public void updateFromDTO(StockDTO dto) {
        this.quantity = dto.getQuantity();
        this.stockUnit = dto.getStockUnit();
    }

    // Helper method to handle integer stock for 'each' unit
    public void setQuantityForEach(int quantity) {
        this.quantity = (double) quantity;
        this.stockUnit = StockUnit.EACH;
    }

    // Helper method to set stock for kg
    public void setQuantityForKg(double quantity) {
        this.quantity = quantity;
        this.stockUnit = StockUnit.KG;
    }

    // Helper method to get integer quantity for 'each' unit
    public Integer getQuantityAsInteger() {
        return stockUnit == StockUnit.EACH ? quantity.intValue() : null;
    }
}