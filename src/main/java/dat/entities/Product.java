package dat.entities;

import dat.dtos.ProductDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ean", "store_id"})
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "ean", nullable = false)
    private String ean;

    // One-to-One relation with Price
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "price_id")
    private Price price;

    // One-to-One relation with Timing
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "timing_id")
    private Timing timing;

    // Many-to-Many relation with Category
    @ManyToMany
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // Many-to-One relation with Store
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // One-to-One relation with Stock
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    public Product(ProductDTO dto) {
        updateFromDTO(dto);
    }

    public void updateFromDTO(ProductDTO dto) {
        this.productName = dto.getProductName();
        this.ean = dto.getEan();

        // Update price if provided
        if (dto.getPrice() != null) {
            if (this.price == null) {
                this.price = new Price(dto.getPrice());
            } else {
                // Update existing price
                this.price.setOriginalPrice(dto.getPrice().getOriginalPrice());
                this.price.setNewPrice(dto.getPrice().getNewPrice());
                this.price.setDiscount(dto.getPrice().getDiscount());
                this.price.setPercentDiscount(dto.getPrice().getPercentDiscount());
            }
        }

        // Update timing if provided
        if (dto.getTiming() != null) {
            if (this.timing == null) {
                this.timing = new Timing(dto.getTiming());
            } else {
                this.timing.updateFromDTO(dto.getTiming());
            }
        }

        // Update stock if provided
        if (dto.getStock() != null) {
            if (this.stock == null) {
                this.stock = new Stock(dto.getStock());
            } else {
                this.stock.updateFromDTO(dto.getStock());
            }
        }
    }

    // Helper methods for managing categories
    public void addCategory(Category category) {
        if (categories == null) {
            categories = new HashSet<>();
        }
        categories.add(category);
        category.getProducts().add(this);
    }

    public void removeCategory(Category category) {
        categories.remove(category);
        category.getProducts().remove(this);
    }

    public void clearCategories() {
        for (Category category : new HashSet<>(categories)) {
            removeCategory(category);
        }
    }

    // Store management
    public void setStore(Store store) {
        this.store = store;
        if (store != null && !store.getProducts().contains(this)) {
            store.getProducts().add(this);
        }
    }

    // Stock management
    public void setStock(Stock stock) {
        this.stock = stock;
        if (stock != null) {
            stock.setProduct(this);
        }
    }
}