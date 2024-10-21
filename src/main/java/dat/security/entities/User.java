package dat.security.entities;

import dat.entities.Address;
import dat.entities.Store;
import dat.entities.Product;
import jakarta.persistence.*;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    // Many-to-One: A user has one role
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;


    // One-to-One: A user has one address
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id")
    private Address address;

    // One-to-Many: A store manager can manage multiple stores
    @OneToMany(mappedBy = "storeManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Set<Store> stores = new HashSet<>();

    // Many-to-One: An employee can only work in one store
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store employeeInStore;

    // Many-to-Many: Users can save multiple stores
    @ManyToMany
    @JoinTable(name = "user_saved_stores",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "store_id"))
    private Set<Store> savedStores = new HashSet<>();

    // Many-to-Many: Users can save multiple products
    @ManyToMany
    @JoinTable(name = "user_saved_products",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> savedProducts = new HashSet<>();

    // Constructor to create a new user with a hashed password and role
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
        this.role = role;
    }

    // Helper method to verify the password using BCrypt
    public boolean verifyPassword(String password) {
        return BCrypt.checkpw(password, this.password);
    }

    // Method to add a store to the list of managed stores
    public void addStore(Store store) {
        this.stores.add(store);
        store.setStoreManager(this);  // Synchronize on the store side
    }

    // Method to remove a store from the list of managed stores
    public void removeStore(Store store) {
        this.stores.remove(store);
        store.setStoreManager(null);  // Remove manager reference from the store
    }

    // Method to set the employee's store
    public void setEmployeeInStore(Store store) {
        if (this.employeeInStore != null) {
            removeEmployeeFromStore();  // Remove from the previous store before adding to the new one
        }
        this.employeeInStore = store;
        store.getEmployees().add(this);  // Synchronize on the store side
    }

    // Method to remove the employee from the store
    public void removeEmployeeFromStore() {
        if (this.employeeInStore != null) {
            this.employeeInStore.getEmployees().remove(this);  // Remove from store's employee list
            this.employeeInStore = null;
        }
    }

    // Method to add a saved store to the user's saved stores
    public void addSavedStore(Store store) {
        this.savedStores.add(store);
    }

    // Method to remove a saved store from the user's saved stores
    public void removeSavedStore(Store store) {
        this.savedStores.remove(store);
    }

    // Method to add a product to the list of saved products
    public void addSavedProduct(Product product) {
        this.savedProducts.add(product);
    }

    // Method to remove a product from the list of saved products
    public void removeSavedProduct(Product product) {
        this.savedProducts.remove(product);
    }

    // Method to set or update the user's address
    public void setAddress(Address address) {
        this.address = address;
        if (address != null) {
            address.setUser(this);
        }
    }
}

