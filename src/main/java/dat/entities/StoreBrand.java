package dat.entities;

import dat.dtos.StoreBrandDTO;
import dat.enums.Brand;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "store_brands")

public class StoreBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand_name", nullable = false)
    private Brand brand;

    // One-to-Many: A store brand can have multiple stores
    @OneToMany(mappedBy = "brand", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private Set<Store> stores = new HashSet<>();


    public StoreBrand(Brand brand) {
        this.brand = brand;
    }

    public StoreBrand(StoreBrandDTO storeBrandDTO) {
        this.id = storeBrandDTO.getId();
        this.brand = storeBrandDTO.getBrand();
    }


}


