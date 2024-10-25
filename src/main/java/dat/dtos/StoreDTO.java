package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import dat.entities.Store;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDTO {
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sallingStoreId;
    private String name;
    private BrandDTO brand;
    private AddressDTO address;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean hasProductsInDb;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private LocalDateTime lastFetched;

    private Set<ProductDTO> products;

    public StoreDTO(Store store) {
        this.id = store.getId();
        this.sallingStoreId = store.getSallingStoreId();
        this.name = store.getName();
        this.brand = new BrandDTO(store.getBrand());
        this.address = new AddressDTO(store.getAddress());
        this.hasProductsInDb = store.hasProductsInDb();
        this.lastFetched = store.getLastFetched();

        if (store.getProducts() != null) {
            this.products = store.getProducts().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toSet());
        }
    }

    public boolean hasProductsInDb() {
        return hasProductsInDb;
    }
}