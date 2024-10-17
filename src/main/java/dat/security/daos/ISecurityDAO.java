package dat.security.daos;

import dat.security.entities.User;
import dat.security.enums.RoleType;
import dat.security.exceptions.ValidationException;
import dat.security.token.UserDTO;

public interface ISecurityDAO {
    UserDTO getVerifiedUser(String email, String password) throws ValidationException;
    User createUser(String email, String name, String password, RoleType roleType); // Ensure this method is present
    User addRole(UserDTO userDTO, String newRole);
}
