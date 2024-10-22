package dat.entities;

import dat.dtos.AddressDTO;
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

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "postal_code", nullable = false)
    private PostalCode postalCode;

    @OneToOne(mappedBy = "address", fetch = FetchType.EAGER)
    private Store store;

    // Constructor
    public Address(String addressLine, PostalCode postalCode) {
        this.addressLine = addressLine;
        this.postalCode = postalCode;
    }

    // Constructor with coordinates
    public Address(String addressLine, PostalCode postalCode, Double longitude, Double latitude) {
        this.addressLine = addressLine;
        this.postalCode = postalCode;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    // Constructor from DTO
    public Address(AddressDTO dto) {
        this.id = dto.getId();
        this.addressLine = dto.getAddressLine();
        this.longitude = dto.getLongitude();
        this.latitude = dto.getLatitude();
        if (dto.getPostalCode() != null) {
            this.postalCode = new PostalCode(dto.getPostalCode());
        }
    }

    // Update method from DTO
    public void updateFromDTO(AddressDTO dto) {
        this.addressLine = dto.getAddressLine();
        this.longitude = dto.getLongitude();
        this.latitude = dto.getLatitude();
        if (dto.getPostalCode() != null) {
            if (this.postalCode == null) {
                this.postalCode = new PostalCode(dto.getPostalCode());
            } else {
                // Update existing postal code
                this.postalCode.setPostalCode(dto.getPostalCode().getPostalCode());
                this.postalCode.setCity(dto.getPostalCode().getCity());
            }
        }
    }
}