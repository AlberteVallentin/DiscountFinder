package dat.dtos;

import dat.entities.Stock;
import dat.enums.StockUnit;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDTO {
    private Long id;
    private Double quantity;
    private StockUnit stockUnit;

    public StockDTO(Stock stock) {
        this.id = stock.getId();
        this.quantity = stock.getQuantity();
        this.stockUnit = stock.getStockUnit();
    }

    // Helper static method to create from Salling API format
    public static StockDTO fromSallingApi(int stock, String stockUnit) {
        return StockDTO.builder()
            .quantity((double) stock)
            .stockUnit(stockUnit.equalsIgnoreCase("each") ? StockUnit.EACH : StockUnit.KG)
            .build();
    }

    // Helper method to get integer quantity for 'each' unit
    public Integer getQuantityAsInteger() {
        return stockUnit == StockUnit.EACH ? quantity.intValue() : null;
    }
}