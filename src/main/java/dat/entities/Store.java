package dat.entities;

import dat.dtos.StoreDTO;
import dat.enums.Brand;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "brand", nullable = false)
    @Enumerated(EnumType.STRING)
    private Brand brand;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "address_id", nullable = false)
    private Address address;

    @Column(name = "has_products_in_db")
    private boolean hasProductsInDb;

    @OneToMany(mappedBy = "store", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Product> products = new HashSet<>();

    // Constructor that takes a StoreDTO
    public Store(StoreDTO dto) {
        this.sallingStoreId = dto.getSallingStoreId();
        this.name = dto.getName();
        this.brand = dto.getBrand();
        this.address = new Address(dto.getAddress());
        this.hasProductsInDb = dto.hasProductsInDb();  // Fixed this line
    }

    // Helper method to update store from Salling API data
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
        this.hasProductsInDb = dto.hasProductsInDb();  // Fixed this line
    }

    // Manual getter for hasProductsInDb to avoid Lombok's "is" prefix
    public boolean hasProductsInDb() {
        return hasProductsInDb;
    }

    // Helper method to add products from Salling API
    public void addProductsFromSallingApi(Set<Product> newProducts) {
        this.products.clear();  // Clear existing products
        this.products.addAll(newProducts);
        this.hasProductsInDb = true;
        newProducts.forEach(product -> product.setStore(this));
    }
}