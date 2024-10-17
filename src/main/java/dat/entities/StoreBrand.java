package dat.entities;

import dat.enums.BrandName;
import jakarta.persistence.*;
import lombok.*;

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
    private BrandName brandName;  // Use the enum here


}

