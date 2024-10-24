package dat.daos.impl;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.*;
import dat.entities.Brand;
import dat.entities.PostalCode;
import dat.entities.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreDAOTest {
    private static EntityManagerFactory emf;
    private static StoreDAO storeDAO;
    private static StoreDTO s1;
    private static StoreDTO s2;

    @BeforeAll
    static void setUpBeforeAll() {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        storeDAO = StoreDAO.getInstance(emf);
    }

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Store").executeUpdate();
            em.createQuery("DELETE FROM Address").executeUpdate();
            em.createQuery("DELETE FROM PostalCode").executeUpdate();
            em.createQuery("DELETE FROM Brand").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE stores_store_id_seq RESTART WITH 1").executeUpdate();

            // Create brands
            Brand netto = new Brand();
            netto.setName("NETTO");
            netto.setDisplayName("Netto");
            em.persist(netto);

            Brand foetex = new Brand();
            foetex.setName("FOETEX");
            foetex.setDisplayName("Føtex");
            em.persist(foetex);

            Brand bilka = new Brand();
            bilka.setName("BILKA");
            bilka.setDisplayName("Bilka");
            em.persist(bilka);

            // Create postal codes
            PostalCode pc2100 = new PostalCode();
            pc2100.setPostalCode(2100);
            pc2100.setCity("København Ø");
            em.persist(pc2100);

            PostalCode pc2200 = new PostalCode();
            pc2200.setPostalCode(2200);
            pc2200.setCity("København N");
            em.persist(pc2200);

            PostalCode pc2300 = new PostalCode();
            pc2300.setPostalCode(2300);
            pc2300.setCity("København S");
            em.persist(pc2300);

            em.getTransaction().commit();
        }

        // Create test data using managed entities
        try (EntityManager em = emf.createEntityManager()) {
            // Get managed brand entities
            Brand nettoBrand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "NETTO")
                .getSingleResult();

            Brand foetexBrand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "FOETEX")
                .getSingleResult();

            PostalCodeDTO postalCode1 = PostalCodeDTO.builder()
                .postalCode(2100)
                .city("København Ø")
                .build();

            PostalCodeDTO postalCode2 = PostalCodeDTO.builder()
                .postalCode(2200)
                .city("København N")
                .build();

            AddressDTO address1 = AddressDTO.builder()
                .addressLine("Østerbrogade 1")
                .postalCode(postalCode1)
                .longitude(12.5683)
                .latitude(55.7317)
                .build();

            AddressDTO address2 = AddressDTO.builder()
                .addressLine("Nørrebrogade 1")
                .postalCode(postalCode2)
                .longitude(12.5633)
                .latitude(55.6897)
                .build();

            BrandDTO nettoBrandDTO = BrandDTO.builder()
                .id(nettoBrand.getId())
                .name(nettoBrand.getName())
                .displayName(nettoBrand.getDisplayName())
                .build();

            BrandDTO foetexBrandDTO = BrandDTO.builder()
                .id(foetexBrand.getId())
                .name(foetexBrand.getName())
                .displayName(foetexBrand.getDisplayName())
                .build();

            s1 = StoreDTO.builder()
                .sallingStoreId("1234")
                .name("Netto Østerbro")
                .brand(nettoBrandDTO)
                .address(address1)
                .hasProductsInDb(false)
                .build();

            s2 = StoreDTO.builder()
                .sallingStoreId("5678")
                .name("Føtex Nørrebro")
                .brand(foetexBrandDTO)
                .address(address2)
                .hasProductsInDb(false)
                .build();

            // Create stores in database
            storeDAO.create(s1);
            storeDAO.create(s2);
        }
    }

    @AfterAll
    void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    @DisplayName("Test create store")
    void testCreate() {
        try (EntityManager em = emf.createEntityManager()) {
            // Get the existing Bilka brand
            Brand bilkaBrand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "BILKA")
                .getSingleResult();

            // Arrange
            PostalCodeDTO postalCode = PostalCodeDTO.builder()
                .postalCode(2300)
                .city("København S")
                .build();

            AddressDTO address = AddressDTO.builder()
                .addressLine("Amagerbrogade 1")
                .postalCode(postalCode)
                .longitude(12.5933)
                .latitude(55.6597)
                .build();

            BrandDTO bilkaBrandDTO = BrandDTO.builder()
                .id(bilkaBrand.getId())
                .name(bilkaBrand.getName())
                .displayName(bilkaBrand.getDisplayName())
                .build();

            StoreDTO s3 = StoreDTO.builder()
                .sallingStoreId("9012")
                .name("Bilka Amager")
                .brand(bilkaBrandDTO)
                .address(address)
                .hasProductsInDb(false)
                .build();

            // Act
            StoreDTO createdStore = storeDAO.create(s3);

            // Assert
            assertThat(createdStore.getId(), is(notNullValue()));
            assertThat(createdStore.getName(), is(s3.getName()));
            assertThat(createdStore.getBrand().getName(), is(bilkaBrandDTO.getName()));
            assertThat(createdStore.getBrand().getDisplayName(), is(bilkaBrandDTO.getDisplayName()));
        }
    }

    @Test
    void testRead() {
        // Act
        StoreDTO store = storeDAO.read(1L);

        // Assert
        assertThat(store, is(notNullValue()));
        assertThat(store.getName(), is(s1.getName()));
        assertThat(store.getBrand().getName(), is(s1.getBrand().getName()));
        assertThat(store.getBrand().getDisplayName(), is(s1.getBrand().getDisplayName()));
    }

    @Test
    void testReadAll() {
        // Act
        List<StoreDTO> stores = storeDAO.readAll();

        // Assert
        assertThat(stores, hasSize(2));
        assertThat(stores.stream()
                .map(StoreDTO::getName)
                .collect(Collectors.toList()),
            containsInAnyOrder("Netto Østerbro", "Føtex Nørrebro"));
        assertThat(stores.stream()
                .map(store -> store.getBrand().getName())
                .collect(Collectors.toList()),
            containsInAnyOrder("NETTO", "FOETEX"));
    }

    @Test
    void testUpdate() {
        // Arrange
        s1.setName("Updated Netto");
        s1.setHasProductsInDb(true);

        // Act
        StoreDTO updatedStore = storeDAO.update(1L, s1);

        // Assert
        assertThat(updatedStore, is(notNullValue()));
        assertThat(updatedStore.getName(), is("Updated Netto"));
        assertThat(updatedStore.hasProductsInDb(), is(true));
        assertThat(updatedStore.getBrand().getName(), is("NETTO"));
    }

    @Test
    void testDelete() {
        // Act
        storeDAO.delete(1L);

        // Assert
        StoreDTO deletedStore = storeDAO.read(1L);
        assertThat(deletedStore, is(nullValue()));
    }

    @Test
    void testFindBySallingId() {
        // Act
        Store store = storeDAO.findBySallingId("1234");

        // Assert
        assertThat(store, is(notNullValue()));
        assertThat(store.getName(), is(s1.getName()));
        assertThat(store.getBrand().getName(), is(s1.getBrand().getName()));
    }

    @Test
    void testStoreAlreadyExists() {
        // Arrange
        BrandDTO nettoBrand = BrandDTO.builder()
            .name("NETTO")
            .displayName("Netto")
            .build();

        StoreDTO duplicate = StoreDTO.builder()
            .sallingStoreId("1234")
            .name("Duplicate Store")
            .brand(nettoBrand)
            .address(s1.getAddress())
            .hasProductsInDb(false)
            .build();

        // Assert
        assertThrows(PersistenceException.class, () -> storeDAO.create(duplicate),
            "Should throw PersistenceException when creating store with existing Salling ID");
    }

    @Test
    void testCreateWithInvalidBrand() {
        // Arrange
        BrandDTO invalidBrand = BrandDTO.builder()
            .name("INVALID")
            .displayName("Invalid Brand")
            .build();

        StoreDTO store = StoreDTO.builder()
            .sallingStoreId("9999")
            .name("Test Store")
            .brand(invalidBrand)
            .address(s1.getAddress())
            .hasProductsInDb(false)
            .build();

        // Assert
        assertThrows(PersistenceException.class, () -> storeDAO.create(store),
            "Should throw PersistenceException when brand doesn't exist");
    }

    @Test
    void testCreateWithInvalidPostalCode() {
        // Arrange
        PostalCodeDTO invalidPostalCode = PostalCodeDTO.builder()
            .postalCode(9999) // Non-existent postal code
            .city("Invalid City")
            .build();

        AddressDTO address = AddressDTO.builder()
            .addressLine("Test Street 1")
            .postalCode(invalidPostalCode)
            .build();

        BrandDTO nettoBrand = BrandDTO.builder()
            .name("NETTO")
            .displayName("Netto")
            .build();

        StoreDTO store = StoreDTO.builder()
            .sallingStoreId("9999")
            .name("Test Store")
            .brand(nettoBrand)
            .address(address)
            .hasProductsInDb(false)
            .build();

        // Assert
        assertThrows(PersistenceException.class, () -> storeDAO.create(store),
            "Should throw PersistenceException when postal code doesn't exist");
    }


    @Test
    void testSaveOrUpdateStores() {
        try (EntityManager em = emf.createEntityManager()) {
            // Get the existing Bilka brand
            Brand bilkaBrand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "BILKA")
                .getSingleResult();

            // Arrange
            s1.setName("Updated Store 1");
            PostalCodeDTO postalCode = PostalCodeDTO.builder()
                .postalCode(2300)
                .city("København S")
                .build();

            AddressDTO address = AddressDTO.builder()
                .addressLine("Amagerbrogade 1")
                .postalCode(postalCode)
                .longitude(12.5933)
                .latitude(55.6597)
                .build();

            BrandDTO bilkaBrandDTO = BrandDTO.builder()
                .id(bilkaBrand.getId())
                .name(bilkaBrand.getName())
                .displayName(bilkaBrand.getDisplayName())
                .build();

            StoreDTO s3 = StoreDTO.builder()
                .sallingStoreId("9012")
                .name("New Store")
                .brand(bilkaBrandDTO)
                .address(address)
                .hasProductsInDb(false)
                .build();

            List<StoreDTO> stores = List.of(s1, s3);

            // Act
            storeDAO.saveOrUpdateStores(stores);

            // Assert
            List<StoreDTO> allStores = storeDAO.readAll();
            assertThat(allStores, hasSize(3));
            assertThat(allStores.stream()
                    .filter(s -> s.getSallingStoreId().equals("1234"))
                    .findFirst()
                    .map(StoreDTO::getName)
                    .orElse(null),
                is("Updated Store 1"));
            assertThat(allStores.stream()
                    .map(StoreDTO::getSallingStoreId)
                    .collect(Collectors.toList()),
                containsInAnyOrder("1234", "5678", "9012"));
        }
    }
}