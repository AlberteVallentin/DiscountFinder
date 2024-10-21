package dat.entities;

import dat.dtos.StoreDTO;
import dat.security.entities.User;
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

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private StoreBrand brand;

    // One-to-One: A store has one address
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "address_id", nullable = false)
    private Address address;

    // Many-to-One: Each store has one manager, but a manager can manage multiple stores
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_manager_id")
    private User storeManager;

    // One-to-Many: A store can have many products
    @OneToMany(mappedBy = "store", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<Product> products = new HashSet<>();

    // One-to-Many: A store can have many employees, but an employee can only work in one store
    @OneToMany(mappedBy = "employeeInStore", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<User> employees = new HashSet<>();

    @Column(name = "has_products_in_db")
    private boolean hasProductsInDb;

    @Column(name = "salling_id")
    private String sallingId;

    // Many-to-Many: Stores can be saved by multiple users
    @ManyToMany(mappedBy = "savedStores", fetch = FetchType.LAZY)
    private Set<User> savedByUsers = new HashSet<>();

    // Add an employee to the store
    public void addEmployee(User employee) {
        this.employees.add(employee);
        employee.setEmployeeInStore(this);  // Synchronize on the user's side
    }

    // Remove an employee from the store
    public void removeEmployee(User employee) {
        this.employees.remove(employee);
        employee.removeEmployeeFromStore();  // Synchronize on the user's side
    }

    // Add a store manager
    public void setStoreManager(User manager) {
        if (this.storeManager != null) {
            this.storeManager.getStores().remove(this);  // Remove this store from the previous manager's list
        }
        this.storeManager = manager;
        if (manager != null) {
            manager.getStores().add(this);  // Add this store to the new manager's list
        }
    }

    public void addProduct(Product product) {
        this.products.add(product);
        product.setStore(this);
    }

    public void removeProduct(Product product) {
        this.products.remove(product);
        product.setStore(null);
    }

    public Store(StoreDTO storeDTO) {
        this.id = storeDTO.getId();
        this.storeName = storeDTO.getStoreName();
        this.address = new Address(storeDTO.getAddress());
        if (storeDTO.getBrand() != null) {
            this.brand = new StoreBrand(storeDTO.getBrand());
        }
        if (storeDTO.getStoreManager() != null) {
            this.storeManager = new User(storeDTO.getStoreManager());
        }
        this.hasProductsInDb = storeDTO.isHasProductsInDb();
        this.sallingId = storeDTO.getSallingId();
    }


}
