package dat.entities;

import dat.security.entities.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id", nullable = false)
    private Long id;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "street_name_and_number", nullable = false)
    private String streetNameAndNumber;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    // One-to-One: Each address is tied to one store
    @OneToOne(mappedBy = "address", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, optional = false)
    private Store store;

    // One-to-One: An address can belong to a single user
    @OneToOne(mappedBy = "address", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private User user;

    public Address(String city, String streetNameAndNumber, String postalCode) {
        this.city = city;
        this.streetNameAndNumber = streetNameAndNumber;
        this.postalCode = postalCode;
    }
}

