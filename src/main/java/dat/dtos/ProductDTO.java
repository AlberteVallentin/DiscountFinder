package dat.dtos;

import dat.entities.Product;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String productName;
    private String imageUrl;
    private PriceDTO price;
    private TimingDTO timing;
    private Set<CategoryDTO> categories;
    private StockDTO stock;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.productName = product.getProductName();
        this.imageUrl = product.getImageUrl();

        if (product.getPrice() != null) {
            this.price = new PriceDTO(product.getPrice());
        }

        if (product.getTiming() != null) {
            this.timing = new TimingDTO(product.getTiming());
        }

        if (product.getStock() != null) {
            this.stock = new StockDTO(product.getStock());
        }

        if (product.getCategories() != null) {
            this.categories = product.getCategories().stream()
                .map(CategoryDTO::new)
                .collect(Collectors.toSet());
        } else {
            this.categories = new HashSet<>();
        }
    }
}