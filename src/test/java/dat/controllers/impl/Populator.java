package dat.controllers.impl;

import dat.entities.*;
import dat.enums.StockUnit;
import dat.security.entities.Role;
import dat.security.entities.User;
import dat.security.enums.RoleType;
import dat.security.token.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Populator {

    public static UserDTO[] populateUsers(EntityManagerFactory emf) {
        User user, admin;
        Role userRole, adminRole;

        // Create roles first
        userRole = new Role(RoleType.USER);
        adminRole = new Role(RoleType.ADMIN);

        // Create users with all required fields
        user = new User("User", "testuser@test.dk", "test123", userRole);
        admin = new User("Admin", "adminuser@test.dk", "admin123", adminRole);

        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(userRole);
            em.persist(adminRole);
            em.persist(user);
            em.persist(admin);
            em.getTransaction().commit();
        }

        UserDTO userDTO = new UserDTO(user.getEmail(), user.getPassword(), RoleType.USER);
        UserDTO adminDTO = new UserDTO(admin.getEmail(), user.getPassword(), RoleType.ADMIN);
        return new UserDTO[]{userDTO, adminDTO};
    }

    public static Store[] populateStores(EntityManagerFactory emf) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Create brands
            Brand nettoBrand = new Brand("NETTO", "Netto");
            Brand foetexBrand = new Brand("FOETEX", "Føtex");
            Brand bilkaBrand = new Brand("BILKA", "Bilka");
            em.persist(nettoBrand);
            em.persist(foetexBrand);
            em.persist(bilkaBrand);

            // Create postal codes
            PostalCode pc2100 = createPostalCode(2100, "København Ø");
            PostalCode pc2200 = createPostalCode(2200, "København N");
            PostalCode pc2300 = createPostalCode(2300, "København S");
            em.persist(pc2100);
            em.persist(pc2200);
            em.persist(pc2300);

            // Create stores with addresses
            Store netto = new Store();
            netto.setSallingStoreId("netto-1234");
            netto.setName("Netto Østerbro");
            netto.setBrand(nettoBrand);
            netto.setAddress(createAddress("Østerbrogade 123", pc2100, 12.5683, 55.7317));
            netto.setHasProductsInDb(false);
            nettoBrand.addStore(netto);

            Store foetex = new Store();
            foetex.setSallingStoreId("foetex-5678");
            foetex.setName("Føtex Nørrebro");
            foetex.setBrand(foetexBrand);
            foetex.setAddress(createAddress("Nørrebrogade 456", pc2200, 12.5633, 55.6897));
            foetex.setHasProductsInDb(false);
            foetexBrand.addStore(foetex);

            Store bilka = new Store();
            bilka.setSallingStoreId("bilka-9012");
            bilka.setName("Bilka Amager");
            bilka.setBrand(bilkaBrand);
            bilka.setAddress(createAddress("Amagerbrogade 789", pc2300, 12.5933, 55.6597));
            bilka.setHasProductsInDb(false);
            bilkaBrand.addStore(bilka);

            em.persist(netto);
            em.persist(foetex);
            em.persist(bilka);

            em.getTransaction().commit();
            return new Store[]{netto, foetex, bilka};
        }
    }

    private static PostalCode createPostalCode(int code, String city) {
        PostalCode postalCode = new PostalCode();
        postalCode.setPostalCode(code);
        postalCode.setCity(city);
        return postalCode;
    }

    private static Address createAddress(String addressLine, PostalCode postalCode,
                                         double longitude, double latitude) {
        Address address = new Address();
        address.setAddressLine(addressLine);
        address.setPostalCode(postalCode);
        address.setLongitude(longitude);
        address.setLatitude(latitude);
        return address;
    }
}

//