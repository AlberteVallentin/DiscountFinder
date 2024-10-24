package dat.security.entities;

import dat.entities.Address;
import jakarta.persistence.*;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Serial;
import java.io.Serializable;

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

}

