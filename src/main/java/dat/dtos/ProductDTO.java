package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;

    private String productName;
    private String ean;
    private PriceDTO price;
    private TimingDTO timing;
    private Set<CategoryDTO> categories;
    private StockDTO stock;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.productName = product.getProductName();
        this.ean = product.getEan();

        if (product.getPrice() != null) {
            this.price = new PriceDTO(product.getPrice());
        }

        if (product.getTiming() != null) {
            this.timing = new TimingDTO(product.getTiming());
        }

        if (product.getStock() != null) {
            this.stock = new StockDTO(product.getStock());
        }

        // Initialize categories as empty set
        this.categories = new HashSet<>();

        // Map categories if they exist
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            this.categories = product.getCategories().stream()
                .map(category -> CategoryDTO.fromSallingCategory(
                    category.getNameDa(),
                    category.getNameEn(),
                    category.getPathDa(),
                    category.getPathEn()
                ))
                .collect(Collectors.toSet());
        }
    }

    @Override
    public String toString() {
        return String.format("""
            Product: %s
            Categories: %s
            Price: %.2f kr → %.2f kr (%.0f%% off)
            Stock: %.1f %s
            Valid until: %s
            """,
            productName,
            categories.stream()
                .map(cat -> cat.getNameDa() + " (" + cat.getNameEn() + ")")
                .collect(Collectors.joining(" > ")),
            price.getOriginalPrice(),
            price.getNewPrice(),
            price.getPercentDiscount(),
            stock.getQuantity(),
            stock.getStockUnit(),
            timing.getEndTime()
        );
    }
}