package dat.config;

import dat.entities.Brand;
import dat.security.entities.Role;
import dat.security.enums.RoleType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class Populate {
    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try {
            loadSQLData(emf);
            populateRoles(emf);
            populateBrands(emf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadSQLData(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
            ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
            try (Connection connection = cp.getConnection()) {

                InputStream inputStream = Populate.class.getClassLoader().getResourceAsStream("data/postal_code_and_city.sql");
                if (inputStream == null) {
                    throw new IllegalArgumentException("data.sql not found in resources/data directory");
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    StringBuilder sqlStatement = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--") || line.trim().isEmpty()) {
                            continue;
                        }

                        sqlStatement.append(line);
                        if (line.endsWith(";")) {
                            try (var statement = connection.createStatement()) {
                                statement.execute(sqlStatement.toString());
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            sqlStatement.setLength(0);
                        }
                    }
                }

                em.getTransaction().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void populateRoles(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Long roleCount = em.createQuery("SELECT COUNT(r) FROM Role r", Long.class).getSingleResult();
            if (roleCount == 0) {
                Role userRole = new Role(RoleType.USER);
                Role adminRole = new Role(RoleType.ADMIN);

                em.persist(userRole);
                em.persist(adminRole);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void populateBrands(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Check if brands already exist
            Long brandCount = em.createQuery("SELECT COUNT(b) FROM Brand b", Long.class).getSingleResult();

            if (brandCount == 0) {
                // Create and persist the three main brands
                Brand netto = new Brand("NETTO", "Netto");
                Brand bilka = new Brand("BILKA", "Bilka");
                Brand foetex = new Brand("FOETEX", "Føtex");

                em.persist(netto);
                em.persist(bilka);
                em.persist(foetex);

                System.out.println("Brands populated successfully.");
            } else {
                System.out.println("Brands already exist in the database.");
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Error populating brands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}