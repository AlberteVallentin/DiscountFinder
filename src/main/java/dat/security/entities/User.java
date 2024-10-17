package dat.security.entities;

import dat.entities.Store;
import dat.security.enums.RoleType;
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

    // Role association
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // One-to-Many: A store manager can manage multiple stores
    @OneToMany(mappedBy = "storeManager", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Store> stores = new HashSet<>();

    // Many-to-One: An employee can only work in one store
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store employeeInStore;

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
}

