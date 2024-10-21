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


    @Column(name = "address_line", nullable = false)
    private String addressLine;

    // Many-to-One: Each address is tied to one postal code
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "postal_code", nullable = false)
    private PostalCode postalCode;

    // One-to-One: Each address is tied to one store
    @OneToOne(mappedBy = "address", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, optional = false)
    private Store store;

    // One-to-One: An address can belong to a single user
    @OneToOne(mappedBy = "address", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private User user;

    // Constructor
    public Address(String addressLine, PostalCode postalCode) {
        this.addressLine = addressLine;
        this.postalCode = postalCode;
    }

}

