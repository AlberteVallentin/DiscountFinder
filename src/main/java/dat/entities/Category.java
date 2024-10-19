package dat.entities;

import dat.enums.CategoryName;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private CategoryName categoryName;

    public Category(CategoryName categoryName) {
        this.categoryName = categoryName;
    }

    @ManyToMany(mappedBy = "categories", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private Set<Product> products = new HashSet<>();

    // Add or remove products from category
    public void addProduct(Product product) {
        if (product != null) {
            this.products.add(product);
            product.getCategories().add(this);
        }
    }

    public void removeProduct(Product product) {
        if (product != null) {
            this.products.remove(product);
            product.getCategories().remove(this);
        }
    }
}


