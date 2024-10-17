package dat.entities;

import dat.security.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false)
    private Long id;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    // Many-to-One: Each store has one manager, but a manager can manage multiple stores
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_manager_id")
    private User storeManager;

    // One-to-Many: A store can have many employees, but an employee can only work in one store
    @OneToMany(mappedBy = "employeeInStore", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<User> employees = new HashSet<>();

    // Constructor, getters, and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    // Getter and setter for storeManager
    public User getStoreManager() {
        return storeManager;
    }

    public void setStoreManager(User storeManager) {
        this.storeManager = storeManager;
    }

    // Getter and setter for employees
    public Set<User> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<User> employees) {
        this.employees = employees;
    }

    // Add an employee to the store
    public void addEmployee(User employee) {
        this.employees.add(employee);
        employee.setEmployeeInStore(this);  // Synkroniser på brugerens side
    }

    // Remove an employee from the store
    public void removeEmployee(User employee) {
        this.employees.remove(employee);
        employee.removeEmployeeFromStore();  // Synkroniser på brugerens side
    }
}

