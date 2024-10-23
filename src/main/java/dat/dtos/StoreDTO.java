package dat.dtos;

import dat.entities.Store;
import dat.entities.Brand;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDTO {
    private Long id;
    private String sallingStoreId;
    private String name;
    private BrandDTO brand;
    private AddressDTO address;
    private boolean hasProductsInDb;
    private Set<ProductDTO> products;

    // Constructor from Entity
    public StoreDTO(Store store) {
        this.id = store.getId();
        this.sallingStoreId = store.getSallingStoreId();
        this.name = store.getName();
        this.brand = new BrandDTO(store.getBrand());
        this.address = new AddressDTO(store.getAddress());
        this.hasProductsInDb = store.hasProductsInDb();  // Fixed this line

        if (store.getProducts() != null) {
            this.products = store.getProducts().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toSet());
        }
    }

    // Manual getter for hasProductsInDb to avoid Lombok's "is" prefix
    public boolean hasProductsInDb() {
        return hasProductsInDb;
    }

    // Helper method to create Address from Salling API data
    public static AddressDTO createAddressFromSallingApi(String street, String zipCode) {
        return AddressDTO.builder()
            .addressLine(street)
            .postalCode(new PostalCodeDTO(Integer.parseInt(zipCode), null))  // City will be populated from existing PostalCode data
            .build();
    }
}