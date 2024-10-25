package dat.security.token;

import dat.dtos.StoreDTO;
import dat.security.enums.RoleType;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Purpose: To hold information about a user
 * Author: Thomas Hartmann
 */
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private String name;          // User's name
    private String email;         // User's email
    private RoleType roleType;            // User's role, using the Role enum
    private Set<StoreDTO> stores; // Set of StoreDTOs, applicable for Store Managers
    private String password;      // User's password

    public UserDTO(String email, String password, RoleType roleType) {
        this.email = email;
        this.password = password;
        this.roleType = roleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO dto = (UserDTO) o;
        return Objects.equals(email, dto.email) && roleType == dto.roleType && Objects.equals(stores, dto.stores);
    }
//
//    /**
//     * Constructs a UserDTO with the specified name, email, and role.
//     *
//     * @param name  the name of the user
//     * @param email the email of the user
//     * @param roleType  the role of the user (as an enum)
//     */
//    public UserDTO(String name, String email, RoleType roleType) {
//        this.name = name;
//        this.email = email;
//        this.roleType = roleType;
//    }

    public UserDTO(String email, RoleType roleType) {
        this.email = email;
        this.roleType = roleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, roleType, stores);
    }
}
