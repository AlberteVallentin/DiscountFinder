package dat.dtos;

import dat.entities.PostalCode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostalCodeDTO {
    private Integer postalCode;
    private String city;

    // Constructor from Entity
    public PostalCodeDTO(PostalCode postalCode) {
        this.postalCode = postalCode.getPostalCode();
        this.city = postalCode.getCity();
    }
}