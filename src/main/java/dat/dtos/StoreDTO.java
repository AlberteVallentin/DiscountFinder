package dat.dtos;

import dat.entities.Store;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StoreDTO {
    private Long id;
    private String storeName;
    private StoreBrandDTO brand;
    private AddressDTO address;
    private Long storeManagerId;
    private boolean hasProductsInDb;
    private String sallingId;
    private Set<ProductDTO> products;

}

