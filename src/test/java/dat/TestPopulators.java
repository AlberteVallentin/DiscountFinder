package dat;

import dat.entities.*;
import dat.security.entities.Role;
import dat.security.entities.User;
import dat.security.enums.RoleType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.List;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class TestPopulators {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPopulators.class);
    private static final String TEST_USER_EMAIL = "test@test.dk";
    private static final String TEST_ADMIN_EMAIL = "admin@test.dk";
    private static final String TEST_PASSWORD = "test123";

    public static void populateTestData(EntityManagerFactory emf) {
        try {
            LOGGER.info("Starting test data population");
            cleanDatabase(emf);
            createRolesAndUsers(emf);
            populatePostalCodes(emf);
            createBrands(emf);
            createTestStores(emf);
            LOGGER.info("Test data population completed");
        } catch (Exception e) {
            LOGGER.error("Error populating test data", e);
            throw new RuntimeException("Failed to populate test data", e);
        }
    }

    private static void cleanDatabase(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Clear all existing data in the correct order
            em.createQuery("DELETE FROM Product").executeUpdate();
            em.createQuery("DELETE FROM Store").executeUpdate();
            em.createQuery("DELETE FROM Address").executeUpdate();
            em.createQuery("DELETE FROM PostalCode").executeUpdate();
            em.createQuery("DELETE FROM Brand").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.createQuery("DELETE FROM Role").executeUpdate();

            em.getTransaction().commit();
            LOGGER.info("Database cleaned successfully");
        }
    }

    private static void createRolesAndUsers(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Create roles
            Role userRole = new Role(RoleType.USER);
            Role adminRole = new Role(RoleType.ADMIN);
            em.persist(userRole);
            em.persist(adminRole);

            // Create test users
            User testUser = new User("Test User", TEST_USER_EMAIL, TEST_PASSWORD, userRole);
            User adminUser = new User("Admin User", TEST_ADMIN_EMAIL, TEST_PASSWORD, adminRole);
            em.persist(testUser);
            em.persist(adminUser);

            em.getTransaction().commit();
            LOGGER.info("Roles and users created successfully");
        }
    }

    private static void populatePostalCodes(EntityManagerFactory emf) {
        try {
            SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
            ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);

            try (Connection connection = cp.getConnection()) {
                InputStream inputStream = TestPopulators.class.getClassLoader()
                    .getResourceAsStream("data/postal_code_and_city.sql");

                if (inputStream == null) {
                    throw new RuntimeException("postal_code_and_city.sql not found in resources/data directory");
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder sqlStatement = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--") || line.trim().isEmpty()) {
                            continue;
                        }

                        sqlStatement.append(line);
                        if (line.trim().endsWith(";")) {
                            try (var statement = connection.createStatement()) {
                                statement.execute(sqlStatement.toString());
                            }
                            sqlStatement.setLength(0);
                        }
                    }
                }
            }
            LOGGER.info("Postal codes populated successfully");
        } catch (Exception e) {
            LOGGER.error("Error populating postal codes", e);
            throw new RuntimeException("Failed to populate postal codes", e);
        }
    }

    private static void createBrands(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Create brands
            List<Object[]> brandData = List.of(
                new Object[]{"NETTO", "Netto"},
                new Object[]{"FOETEX", "Føtex"},
                new Object[]{"BILKA", "Bilka"}
            );

            for (Object[] data : brandData) {
                Brand brand = new Brand();
                brand.setName((String) data[0]);
                brand.setDisplayName((String) data[1]);
                em.persist(brand);
            }

            em.getTransaction().commit();
            LOGGER.info("Brands created successfully");
        }
    }

    private static void createTestStores(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Get brands
            Brand netto = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "NETTO")
                .getSingleResult();

            Brand foetex = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "FOETEX")
                .getSingleResult();

            // Create test stores using existing postal codes
            createTestStore(em, "Netto Østerbro", "1234", netto, "Østerbrogade 1", 2100, 12.5683, 55.7317);
            createTestStore(em, "Føtex Nørrebro", "5678", foetex, "Nørrebrogade 1", 2200, 12.5633, 55.6897);
            createTestStore(em, "Netto Amager", "9012", netto, "Amagerbrogade 1", 2300, 12.5933, 55.6597);

            em.getTransaction().commit();
            LOGGER.info("Test stores created successfully");
        }
    }

    private static void createTestStore(EntityManager em, String name, String sallingId, Brand brand,
                                        String addressLine, int postalCode, double longitude, double latitude) {
        PostalCode pc = em.find(PostalCode.class, postalCode);
        if (pc == null) {
            throw new RuntimeException("Postal code " + postalCode + " not found");
        }

        Address address = new Address(addressLine, pc, longitude, latitude);
        em.persist(address);

        Store store = new Store();
        store.setSallingStoreId(sallingId);
        store.setName(name);
        store.setBrand(brand);
        store.setAddress(address);
        store.setHasProductsInDb(false);
        em.persist(store);
    }
}