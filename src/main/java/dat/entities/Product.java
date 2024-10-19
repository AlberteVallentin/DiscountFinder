package dat.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    // Many-to-One: Each product belongs to one store
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "store_id", nullable = false)  // Store is mandatory for Product
    private Store store;

    // One-to-One relation with Price
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "price_id", nullable = false)
    private Price price;

    // One-to-One: A product has one stock
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id", referencedColumnName = "stock_id")
    private Stock stock;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
        name = "product_category",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // Many-to-Many: Users can save multiple products
    @ManyToMany
    @JoinTable(name = "user_saved_products",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> savedProducts = new HashSet<>();

    public void addCategory(Category category) {
        if (category != null) {
            this.categories.add(category);
            category.getProducts().add(this);
        }
    }

    public void removeCategory(Category category) {
        if (category != null) {
            this.categories.remove(category);
            category.getProducts().remove(this);
        }
    }

    public void setStore(Store store) {
        this.store = store;
        if (store != null) {
            store.getProducts().add(this);
        }
    }

    public void addStock(Stock stock) {
        this.stock = stock;
        stock.setProduct(this);  // Synkroniser forholdet
    }

    public void removeStock() {
        if (this.stock != null) {
            this.stock.setProduct(null);  // Fjern produktreferencen fra stock
            this.stock = null;  // Fjern stocken fra produktet
        }
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime.withNano(0);
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime.withNano(0);
    }
}

