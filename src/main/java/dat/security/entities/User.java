package dat.security.entities;

import dat.entities.Address;
import jakarta.persistence.*;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;
import dat.entities.Store;

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_favorite_stores",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private Set<Store> favoriteStores = new HashSet<>();

    // Constructor to create a new user with a hashed password and role
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
        this.role = role;
        this.favoriteStores = new HashSet<>();
    }

    // Helper method to verify the password using BCrypt
    public boolean verifyPassword(String password) {
        return BCrypt.checkpw(password, this.password);
    }

}

