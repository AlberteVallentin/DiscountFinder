package dat.entities;

import dat.dtos.StoreDTO;
import dat.security.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false)
    private Long id;

    @Column(name = "salling_store_id", unique = true, nullable = false)
    private String sallingStoreId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "address_id", nullable = false)
    private Address address;

    @Column(name = "has_products_in_db")
    private boolean hasProductsInDb;

    @Column(name = "last_fetched")
    private LocalDateTime lastFetched;

    @OneToMany(mappedBy = "store", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Product> products = new HashSet<>();

    @ManyToMany(mappedBy = "favoriteStores", fetch = FetchType.EAGER)
    private Set<User> favoredByUsers = new HashSet<>();

    public Store(StoreDTO dto) {
        this.sallingStoreId = dto.getSallingStoreId();
        this.name = dto.getName();
        this.brand = new Brand(dto.getBrand());
        this.address = new Address(dto.getAddress());
        this.hasProductsInDb = dto.hasProductsInDb();
        this.lastFetched = dto.getLastFetched();
        this.favoredByUsers = new HashSet<>();
    }

    public void updateFromSallingApi(StoreDTO dto) {
        this.name = dto.getName();
        if (this.address == null) {
            this.address = new Address(dto.getAddress());
        } else {
            this.address.setAddressLine(dto.getAddress().getAddressLine());
            this.address.setPostalCode(new PostalCode(dto.getAddress().getPostalCode()));
            this.address.setLongitude(dto.getAddress().getLongitude());
            this.address.setLatitude(dto.getAddress().getLatitude());
        }
        this.hasProductsInDb = dto.hasProductsInDb();
        this.lastFetched = dto.getLastFetched();
    }

    public boolean hasProductsInDb() {
        return hasProductsInDb;
    }

    public void addProductsFromSallingApi(Set<Product> newProducts) {
        this.products.clear();
        this.products.addAll(newProducts);
        this.hasProductsInDb = true;
        this.lastFetched = LocalDateTime.now();
        newProducts.forEach(product -> product.setStore(this));
    }

    public boolean needsProductUpdate() {
        if (!hasProductsInDb) return true;
        if (lastFetched == null) return true;
        return LocalDateTime.now().minusHours(24).isAfter(lastFetched);
    }
}