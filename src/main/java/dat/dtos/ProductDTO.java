package dat.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductDTO {

    private Long id;
    private String productName;
    private Long storeId;
    private PriceDTO price;
    private StockDTO stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime lastUpdated;
    private Set<CategoryDTO> categories;
}