package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import dat.entities.Address;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;
    private String addressLine;
    private Double longitude;
    private Double latitude;
    private PostalCodeDTO postalCode;

    // Constructor from Entity
    public AddressDTO(Address address) {
        this.id = address.getId();
        this.addressLine = address.getAddressLine();
        this.longitude = address.getLongitude();
        this.latitude = address.getLatitude();
        this.postalCode = new PostalCodeDTO(address.getPostalCode());
    }
}