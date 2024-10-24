package dat.entities;

import dat.dtos.PostalCodeDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "postal_code_and_city")
public class PostalCode {

    @Id
    @Column(name = "postal_code", nullable = false)
    private Integer postalCode;

    @Column(name = "city", nullable = false)
    private String city;

    // Constructor from DTO
    public PostalCode(PostalCodeDTO dto) {
        this.postalCode = dto.getPostalCode();
        this.city = dto.getCity();
    }
}