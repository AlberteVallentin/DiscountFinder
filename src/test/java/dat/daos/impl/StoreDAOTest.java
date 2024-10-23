package dat.daos.impl;

import dat.config.HibernateConfig;
import dat.dtos.AddressDTO;
import dat.dtos.BrandDTO;
import dat.dtos.PostalCodeDTO;
import dat.dtos.StoreDTO;
import dat.entities.Brand;
import dat.entities.Store;
import dat.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreDAOTest {

    private static EntityManagerFactory emf;
    private static StoreDAO storeDAO;
    private static BrandDAO brandDAO;
    private static StoreDTO s1;
    private static StoreDTO s2;
    private static BrandDTO nettoBrand;
    private static BrandDTO foetexBrand;

    @BeforeAll
    static void setUpBeforeAll() {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        storeDAO = StoreDAO.getInstance(emf);
        brandDAO = BrandDAO.getInstance(emf);
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
            em.createNativeQuery("ALTER SEQUENCE brands_brand_id_seq RESTART WITH 1").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create brands first
        try {
            Brand netto = brandDAO.findOrCreateBrand("NETTO", "Netto");
            Brand foetex = brandDAO.findOrCreateBrand("FOETEX", "Føtex");
            nettoBrand = new BrandDTO(netto);
            foetexBrand = new BrandDTO(foetex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize test data
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

        s1 = StoreDTO.builder()
            .sallingStoreId("1234")
            .name("Netto Østerbro")
            .brand(nettoBrand)
            .address(address1)
            .hasProductsInDb(false)
            .build();

        s2 = StoreDTO.builder()
            .sallingStoreId("5678")
            .name("Føtex Nørrebro")
            .brand(foetexBrand)
            .address(address2)
            .hasProductsInDb(false)
            .build();

        // Create stores in database
        try {
            storeDAO.create(s1);
            storeDAO.create(s2);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Test create store")
    void testCreate() throws ApiException {
        // Create a new store with Bilka brand
        Brand bilka = brandDAO.findOrCreateBrand("BILKA", "Bilka");
        BrandDTO bilkaBrand = new BrandDTO(bilka);

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

        StoreDTO s3 = StoreDTO.builder()
            .sallingStoreId("9012")
            .name("Bilka Amager")
            .brand(bilkaBrand)
            .address(address)
            .hasProductsInDb(false)
            .build();

        // Create the store
        StoreDTO createdStore = storeDAO.create(s3);

        // Verify the store was created
        assertNotNull(createdStore.getId());
        assertEquals(s3.getName(), createdStore.getName());
        assertEquals(s3.getBrand().getName(), createdStore.getBrand().getName());
    }

    @Test
    void testRead() {
        // Read existing store
        StoreDTO store = storeDAO.read(1L);

        // Verify the store was read correctly
        assertNotNull(store);
        assertEquals(s1.getName(), store.getName());
        assertEquals(s1.getBrand().getName(), store.getBrand().getName());
    }

    @Test
    void testReadAll() {
        // Read all stores
        List<StoreDTO> stores = storeDAO.readAll();

        // Verify correct number of stores
        assertEquals(2, stores.size());
        // Verify store contents
        assertTrue(stores.stream().anyMatch(s -> s.getName().equals("Netto Østerbro")));
        assertTrue(stores.stream().anyMatch(s -> s.getName().equals("Føtex Nørrebro")));
    }

    @Test
    void testUpdate() throws ApiException {
        // Update store
        s1.setName("Updated Netto");
        s1.setHasProductsInDb(true);

        StoreDTO updatedStore = storeDAO.update(1L, s1);

        // Verify the update
        assertNotNull(updatedStore);
        assertEquals("Updated Netto", updatedStore.getName());
        assertTrue(updatedStore.hasProductsInDb());
    }

    @Test
    void testDelete() {
        // Delete store
        storeDAO.delete(1L);

        // Verify the deletion
        StoreDTO deletedStore = storeDAO.read(1L);
        assertNull(deletedStore);
    }

    @Test
    void testFindBySallingId() {
        // Find store by Salling ID
        Store store = storeDAO.findBySallingId("1234");

        // Verify the store was found
        assertNotNull(store);
        assertEquals(s1.getName(), store.getName());
        assertEquals(s1.getBrand().getName(), store.getBrand().getName());
    }

    @Test
    void testStoreAlreadyExists() {
        // Try to create a store with existing Salling ID
        StoreDTO duplicate = StoreDTO.builder()
            .sallingStoreId("1234")
            .name("Duplicate Store")
            .brand(nettoBrand)
            .address(s1.getAddress())
            .hasProductsInDb(false)
            .build();

        // Verify that creating a duplicate store throws an exception
        assertThrows(ApiException.class, () -> storeDAO.create(duplicate));
    }
}