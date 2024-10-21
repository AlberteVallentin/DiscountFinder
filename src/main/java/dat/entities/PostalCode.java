package dat.entities;

import dat.dtos.PostalCodeDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "postal_code_and_city")
public class PostalCode {

    @Id
    @Column(name = "postal_code", nullable = false, unique = true)
    private Integer postalCode;

    @Column(name = "city", nullable = false)
    private String city;

    public PostalCode(PostalCodeDTO postalCodeDTO) {
        this.postalCode = postalCodeDTO.getPostalCode();
        this.city = postalCodeDTO.getCity();
    }


}
