package dat.entities;

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

}
