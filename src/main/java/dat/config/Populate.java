package dat.config;

import dat.security.entities.Role;
import dat.security.enums.RoleType;
import dat.entities.Brand;
import dat.services.StoreFetcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class Populate {
    private static final Logger LOGGER = LoggerFactory.getLogger(Populate.class);
    private static final List<String> BRANDS = List.of("NETTO", "BILKA", "FOETEX");
    private static final List<String> DISPLAY_NAMES = List.of("Netto", "Bilka", "Føtex");

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try {
            populateAll(emf);
        } catch (Exception e) {
            LOGGER.error("Error during population", e);
            e.printStackTrace();
        }
    }

    public static void populateAll(EntityManagerFactory emf) {
        try {
            LOGGER.info("Starting database population");

            // Step 1: Initialize brands
            LOGGER.info("Initializing brands...");
            populateBrands(emf);

            // Step 2: Load postal codes and cities
            LOGGER.info("Loading postal codes and cities...");
            loadPostalCodeData(emf);

            // Step 3: Populate roles
            LOGGER.info("Populating roles...");
            populateRoles(emf);

            // Step 4: Fetch and save stores
            LOGGER.info("Fetching and saving stores...");
            fetchAndSaveStores();

            LOGGER.info("Database population completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error during database population", e);
            throw new RuntimeException("Database population failed", e);
        }
    }

    private static void populateBrands(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Check if brands already exist
            Long brandCount = em.createQuery("SELECT COUNT(b) FROM Brand b", Long.class)
                .getSingleResult();

            if (brandCount == 0) {
                LOGGER.info("Creating default brands...");
                for (int i = 0; i < BRANDS.size(); i++) {
                    Brand brand = new Brand();
                    brand.setName(BRANDS.get(i));
                    brand.setDisplayName(DISPLAY_NAMES.get(i));
                    em.persist(brand);
                }
                LOGGER.info("Default brands created successfully");
            } else {
                LOGGER.info("Brands already exist, skipping brand creation");
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            LOGGER.error("Error populating brands", e);
            throw new RuntimeException("Failed to populate brands", e);
        }
    }

    private static void loadPostalCodeData(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Check if postal codes already exist
            Long postalCodeCount = em.createQuery("SELECT COUNT(p) FROM PostalCode p", Long.class)
                .getSingleResult();

            if (postalCodeCount > 0) {
                LOGGER.info("Postal codes already exist, skipping postal code data load");
                em.getTransaction().commit();
                return;
            }

            SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
            ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);

            try (Connection connection = cp.getConnection()) {
                InputStream inputStream = Populate.class.getClassLoader()
                    .getResourceAsStream("data/postal_code_and_city.sql");

                if (inputStream == null) {
                    throw new IllegalArgumentException("postal_code_and_city.sql not found in resources/data directory");
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder sqlStatement = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--") || line.trim().isEmpty()) {
                            continue;
                        }

                        sqlStatement.append(line);
                        if (line.endsWith(";")) {
                            try (var statement = connection.createStatement()) {
                                statement.execute(sqlStatement.toString());
                            } catch (SQLException e) {
                                LOGGER.error("Error executing SQL statement: {}", sqlStatement, e);
                                throw e;
                            }
                            sqlStatement.setLength(0);
                        }
                    }
                }
            }

            em.getTransaction().commit();
            LOGGER.info("Successfully loaded postal code data");
        } catch (Exception e) {
            LOGGER.error("Error loading postal code data", e);
            throw new RuntimeException("Failed to load postal code data", e);
        }
    }

    private static void populateRoles(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Long roleCount = em.createQuery("SELECT COUNT(r) FROM Role r", Long.class)
                .getSingleResult();

            if (roleCount == 0) {
                LOGGER.info("Creating default roles...");
                Role userRole = new Role(RoleType.USER);
                Role adminRole = new Role(RoleType.ADMIN);

                em.persist(userRole);
                em.persist(adminRole);

                LOGGER.info("Default roles created successfully");
            } else {
                LOGGER.info("Roles already exist, skipping role creation");
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            LOGGER.error("Error populating roles", e);
            throw new RuntimeException("Failed to populate roles", e);
        }
    }

    private static void fetchAndSaveStores() {
        try {
            StoreFetcher storeFetcher = StoreFetcher.getInstance();
            var stores = storeFetcher.fetchAndSaveAllStores();
            LOGGER.info("Successfully fetched and saved {} stores", stores.size());

            // Log store distribution by brand
            stores.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    store -> store.getBrand().getDisplayName(),
                    java.util.stream.Collectors.counting()))
                .forEach((brand, count) ->
                    LOGGER.info("{}: {} stores", brand, count));
        } catch (Exception e) {
            LOGGER.error("Error fetching and saving stores", e);
            throw new RuntimeException("Failed to fetch and save stores", e);
        }
    }
}