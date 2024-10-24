package dat.entities;

import dat.dtos.BrandDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id", nullable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL)
    private Set<Store> stores = new HashSet<>();

    public Brand(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public Brand(BrandDTO dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.displayName = dto.getDisplayName();
    }

    public void addStore(Store store) {
        stores.add(store);
        store.setBrand(this);
    }

    public void removeStore(Store store) {
        stores.remove(store);
        store.setBrand(null);
    }
}