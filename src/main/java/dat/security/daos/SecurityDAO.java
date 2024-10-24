package dat.security.daos;

import dat.security.entities.Role;
import dat.security.entities.User;
import dat.security.enums.RoleType;
import dat.security.exceptions.ApiException;
import dat.security.exceptions.ValidationException;
import dat.security.token.UserDTO;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;


/**
 * Purpose: To handle security in the API
 * Author: Thomas Hartmann
 */
public class SecurityDAO implements ISecurityDAO {

    private static ISecurityDAO instance;
    private static EntityManagerFactory emf;

    public SecurityDAO(EntityManagerFactory _emf) {
        emf = _emf;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }


    // Method to get a verified user by email and password
    @Override
    public UserDTO getVerifiedUser(String email, String password) throws ValidationException {
        try (EntityManager em = getEntityManager()) {
            // Find user by email
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getSingleResult();

            if (user == null) {
                throw new EntityNotFoundException("No user found with email: " + email);
            }

            // Verify the password
            if (!user.verifyPassword(password)) {
                throw new ValidationException("Wrong password");
            }

            // Get the RoleType from the associated Role entity
            RoleType userRoleType = user.getRole().getRoleType();

            // Return the UserDTO with email and role (as Role enum)
            return new UserDTO(user.getEmail(), userRoleType);
        } catch (EntityNotFoundException e) {
            throw new ValidationException("No user found with the provided email.");
        } catch (ValidationException e) {
            throw new ValidationException("Invalid password.");
        } catch (Exception e) {
            throw new ValidationException("An error occurred while verifying the user: " + e.getMessage());
        }
    }


    @Override
    public User createUser(String name, String email,  String password, RoleType roleType) {
        try (EntityManager em = getEntityManager()) {
            // Check if the user already exists
            User userEntity = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userEntity != null) {
                throw new EntityExistsException("User with email: " + email + " already exists");
            }

            // If no role is provided, throw an exception
            if (roleType == null) {
                throw new ApiException(400, "Role must be provided when creating a user");
            }

            // Prevent creating a user with the ADMIN role
            if (roleType == RoleType.ADMIN) {
                throw new ApiException(403, "Cannot assign ADMIN role during registration");
            }

            // Find the corresponding Role entity from the database
            Role role = em.createQuery("SELECT r FROM Role r WHERE r.roleType = :roleType", Role.class)
                .setParameter("roleType", roleType)
                .getSingleResult();

            // Create the new user with the specified role
            userEntity = new User(name, email, password, role);
            em.getTransaction().begin();
            em.persist(userEntity);
            em.getTransaction().commit();
            return userEntity;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException(400, e.getMessage());
        }
    }


    // Add or update a user's role (since a user can only have one role)
    @Override
    public User addRole(UserDTO userDTO, String newRole) {
        try (EntityManager em = getEntityManager()) {
            // Fetch the user by email
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", userDTO.getEmail())
                .getSingleResult();

            if (user == null) {
                throw new EntityNotFoundException("No user found with email: " + userDTO.getEmail());
            }

            // Fetch the new role entity based on the role string provided
            Role role = em.createQuery("SELECT r FROM Role r WHERE r.roleType = :roleType", Role.class)
                .setParameter("roleType", RoleType.valueOf(newRole.toUpperCase()))
                .getSingleResult();

            if (role == null) {
                throw new EntityNotFoundException("Role not found: " + newRole);
            }

            em.getTransaction().begin();

            // Update the user's role
            user.setRole(role);

            em.merge(user);
            em.getTransaction().commit();
            return user;
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "Invalid role: " + newRole);
        } catch (EntityNotFoundException e) {
            throw new ApiException(404, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException(500, "An error occurred while adding role: " + e.getMessage());
        }
    }

}
