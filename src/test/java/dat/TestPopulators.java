package dat;

import dat.daos.impl.BrandDAO;
import dat.daos.impl.StoreDAO;
import dat.dtos.*;
import dat.entities.Brand;
import dat.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestPopulators {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPopulators.class);
    private static EntityManagerFactory emf;
    private static StoreDAO storeDAO;
    private static BrandDAO brandDAO;

    public static void setEntityManagerFactory(EntityManagerFactory _emf) {
        emf = _emf;
        storeDAO = StoreDAO.getInstance(emf);
        brandDAO = BrandDAO.getInstance(emf);
    }

    private static void populateBrands() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Check if brands already exist
            Long brandCount = em.createQuery("SELECT COUNT(b) FROM Brand b", Long.class)
                .getSingleResult();

            if (brandCount == 0) {
                // Create the three main brands
                Brand netto = new Brand("NETTO", "Netto");
                Brand bilka = new Brand("BILKA", "Bilka");
                Brand foetex = new Brand("FOETEX", "Føtex");

                em.persist(netto);
                em.persist(bilka);
                em.persist(foetex);

                LOGGER.info("Created test brands");
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            LOGGER.error("Error populating brands", e);
            throw new RuntimeException("Could not populate brands", e);
        }
    }

    public static List<StoreDTO> populateStores() {
        List<StoreDTO> stores = new ArrayList<>();

        try {
            // Ensure brands exist first
            populateBrands();

            // Create test stores
            stores.add(createAndPersistTestStore(
                "1234", "Netto Østerbro",
                "Østerbrogade 1", 2100, "København Ø",
                12.5683, 55.7317,
                "NETTO", "Netto"
            ));

            stores.add(createAndPersistTestStore(
                "5678", "Føtex Nørrebro",
                "Nørrebrogade 1", 2200, "København N",
                12.5633, 55.6897,
                "FOETEX", "Føtex"
            ));

            stores.add(createAndPersistTestStore(
                "9012", "Bilka Amager",
                "Amagerbrogade 1", 2300, "København S",
                12.5933, 55.6597,
                "BILKA", "Bilka"
            ));

            LOGGER.info("Populated {} test stores", stores.size());
            return stores;
        } catch (Exception e) {
            LOGGER.error("Error populating stores", e);
            throw new RuntimeException("Could not populate stores", e);
        }
    }

    public static StoreDTO createTestStoreDTO(
        String sallingId, String name,
        String addressLine, int postalCode, String city,
        double longitude, double latitude,
        String brandName, String brandDisplayName
    ) {
        PostalCodeDTO postalCodeDTO = PostalCodeDTO.builder()
            .postalCode(postalCode)
            .city(city)
            .build();

        AddressDTO addressDTO = AddressDTO.builder()
            .addressLine(addressLine)
            .postalCode(postalCodeDTO)
            .longitude(longitude)
            .latitude(latitude)
            .build();

        BrandDTO brandDTO = BrandDTO.builder()
            .name(brandName)
            .displayName(brandDisplayName)
            .build();

        return StoreDTO.builder()
            .sallingStoreId(sallingId)
            .name(name)
            .brand(brandDTO)
            .address(addressDTO)
            .hasProductsInDb(false)
            .build();
    }

    private static StoreDTO createAndPersistTestStore(
        String sallingId, String name,
        String addressLine, int postalCode, String city,
        double longitude, double latitude,
        String brandName, String brandDisplayName
    ) throws ApiException {
        // Create DTO
        StoreDTO storeDTO = createTestStoreDTO(
            sallingId, name,
            addressLine, postalCode, city,
            longitude, latitude,
            brandName, brandDisplayName
        );

        // Persist using DAO
        return storeDAO.create(storeDTO);
    }

    public static void cleanUpStores() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Delete in correct order to maintain referential integrity
            em.createQuery("DELETE FROM Store").executeUpdate();
            em.createQuery("DELETE FROM Address").executeUpdate();
            em.createQuery("DELETE FROM PostalCode").executeUpdate();
            em.createQuery("DELETE FROM Brand").executeUpdate();

            em.getTransaction().commit();
            LOGGER.info("Cleaned up all test data");
        } catch (Exception e) {
            LOGGER.error("Error cleaning up stores", e);
            throw new RuntimeException("Could not clean up stores", e);
        }
    }
}