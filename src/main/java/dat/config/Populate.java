package dat.config;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class Populate {
    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        // Populate using SQL file
        try {
            loadSQLData(emf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadSQLData(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Get the DataSource from the EntityManagerFactory
            SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
            ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
            try (Connection connection = cp.getConnection()) {

                // Read the SQL file from resources/data/data.sql
                InputStream inputStream = Populate.class.getClassLoader().getResourceAsStream("data/postal_code_and_city.sql");
                if (inputStream == null) {
                    throw new IllegalArgumentException("data.sql not found in resources/data directory");
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    StringBuilder sqlStatement = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        // Ignore comments in the SQL file
                        if (line.startsWith("--") || line.trim().isEmpty()) {
                            continue;
                        }

                        sqlStatement.append(line);
                        // If we find a semicolon, it indicates the end of a statement
                        if (line.endsWith(";")) {
                            try (var statement = connection.createStatement()) {
                                statement.execute(sqlStatement.toString());
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            // Clear for the next statement
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
}