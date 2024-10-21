package dat.dtos;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StoreDTO {
    private Long id;
    private String storeName;
    private String brand;
    private AddressDTO address;
    private Long storeManagerId;
    private boolean hasProductsInDb;
    private String sallingId;
    private Set<ProductDTO> products;
}

