package dat.daos.impl;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.*;
import dat.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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



    @Test
    @DisplayName("Test updateStoreProducts removes obsolete products")
    void testUpdateStoreProductsRemovesObsoleteProducts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Arrange: Find en butik og opret produkter direkte med korrekt reference til butikken
            Store store = em.find(Store.class, 1L);

            // Opret PriceDTO, TimingDTO og CategoryDTO'er for produktet
            PriceDTO price1 = PriceDTO.builder()
                .originalPrice(new BigDecimal("20.00"))
                .newPrice(new BigDecimal("15.00"))
                .discount(new BigDecimal("5.00"))
                .percentDiscount(new BigDecimal("25.00"))
                .build();
            TimingDTO timing1 = TimingDTO.builder()
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
            CategoryDTO category1 = CategoryDTO.builder()
                .nameDa("Mejeri")
                .nameEn("Dairy")
                .pathDa("Dagligvarer/Mejeri")
                .pathEn("Groceries/Dairy")
                .build();

            ProductDTO productDto1 = ProductDTO.builder()
                .productName("Milk")
                .ean("1234567890123")
                .price(price1)
                .timing(timing1)
                .categories(Set.of(category1))
                .build();

            PriceDTO price2 = PriceDTO.builder()
                .originalPrice(new BigDecimal("15.00"))
                .newPrice(new BigDecimal("10.00"))
                .discount(new BigDecimal("5.00"))
                .percentDiscount(new BigDecimal("33.00"))
                .build();
            TimingDTO timing2 = TimingDTO.builder()
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
            CategoryDTO category2 = CategoryDTO.builder()
                .nameDa("Bageri")
                .nameEn("Bakery")
                .pathDa("Dagligvarer/Bageri")
                .pathEn("Groceries/Bakery")
                .build();

            ProductDTO productDto2 = ProductDTO.builder()
                .productName("Bread")
                .ean("2345678901234")
                .price(price2)
                .timing(timing2)
                .categories(Set.of(category2))
                .build();

            // Skab produkter fra ProductDTO'er og tilknyt dem til butikken
            Product product1 = new Product(productDto1);
            product1.setStore(store); // Sæt butikken på produktet
            Product product2 = new Product(productDto2);
            product2.setStore(store); // Sæt butikken på produktet

            // Gem produkterne i databasen
            em.persist(product1);
            em.persist(product2);

            em.getTransaction().commit();

            // Simuler API-data: kun "Milk" findes stadig
            List<ProductDTO> updatedProducts = List.of(
                ProductDTO.builder()
                    .productName("Milk")
                    .ean("1234567890123")
                    .price(price1)
                    .timing(timing1)
                    .categories(Set.of(category1))
                    .build()
            );

            // Act: Kør opdateringsmetoden for at fjerne forældede produkter
            storeDAO.updateStoreProducts(store.getId(), updatedProducts);

            // Refresh butikken for at sikre, at JPA opdaterer produktlisten i 'store'
            em.getTransaction().begin();
            em.refresh(store);
            em.getTransaction().commit();

            // Assert: Kontrollér, at kun "Milk" er tilbage
            Store updatedStore = em.find(Store.class, store.getId());
            assertAll(
                () -> assertThat("Only one product should remain", updatedStore.getProducts(), hasSize(1)),
                () -> assertThat(updatedStore.getProducts().stream()
                        .map(Product::getEan)
                        .collect(Collectors.toList()),
                    contains("1234567890123")),
                () -> assertThat(updatedStore.getProducts().stream()
                        .map(Product::getEan)
                        .collect(Collectors.toList()),
                    not(contains("2345678901234")))
            );
        }
    }

    @Test
    @DisplayName("Test updateStoreProducts adds new products and removes obsolete ones")
    void testUpdateStoreProductsAddsNewAndRemovesObsoleteProducts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Arrange: Find en butik og opret eksisterende produkter direkte i databasen
            Store store = em.find(Store.class, 1L);

            // Eksisterende produkt (før synkronisering)
            ProductDTO productDto1 = ProductDTO.builder()
                .productName("Milk")
                .ean("1234567890123")
                .price(new PriceDTO(null, new BigDecimal("20.00"), new BigDecimal("15.00"), new BigDecimal("5.00"), new BigDecimal("25.00")))
                .timing(TimingDTO.builder()
                    .startTime(LocalDateTime.now().minusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1))
                    .lastUpdated(LocalDateTime.now())
                    .build())
                .categories(Set.of(new CategoryDTO(null, "Mejeri", "Dairy", "Dagligvarer/Mejeri", "Groceries/Dairy")))
                .build();

            Product product1 = new Product(productDto1);
            product1.setStore(store);
            em.persist(product1);
            em.getTransaction().commit();

            // Act: Simuler en API-respons fra Salling, der indeholder "Milk" og et nyt produkt "Cheese"
            List<ProductDTO> sallingProducts = List.of(
                ProductDTO.builder()
                    .productName("Milk")
                    .ean("1234567890123")
                    .price(new PriceDTO(null, new BigDecimal("20.00"), new BigDecimal("15.00"), new BigDecimal("5.00"), new BigDecimal("25.00")))
                    .timing(TimingDTO.builder()
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .lastUpdated(LocalDateTime.now())
                        .build())
                    .categories(Set.of(new CategoryDTO(null, "Mejeri", "Dairy", "Dagligvarer/Mejeri", "Groceries/Dairy")))
                    .build(),
                ProductDTO.builder()
                    .productName("Cheese")
                    .ean("3456789012345")  // Nyt produkt med unik EAN
                    .price(new PriceDTO(null, new BigDecimal("40.00"), new BigDecimal("30.00"), new BigDecimal("10.00"), new BigDecimal("25.00")))
                    .timing(TimingDTO.builder()
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(5))
                        .lastUpdated(LocalDateTime.now())
                        .build())
                    .categories(Set.of(new CategoryDTO(null, "Mejeri", "Dairy", "Dagligvarer/Mejeri", "Groceries/Dairy")))
                    .build()
            );

            // Opdater databasen for at matche Salling's API-data
            em.getTransaction().begin();
            storeDAO.updateStoreProducts(store.getId(), sallingProducts);
            em.flush();
            em.getTransaction().commit();

            // Refresh for at sikre, at entiteten er opdateret korrekt i persistence-konteksten
            em.refresh(store);

            // Assert: Tjek at "Milk" forbliver, og at "Cheese" tilføjes i databasen
            Store updatedStore = em.find(Store.class, store.getId());
            assertAll(
                () -> assertThat("Two products should remain", updatedStore.getProducts(), hasSize(2)),
                () -> assertThat(updatedStore.getProducts().stream()
                        .map(Product::getEan)
                        .collect(Collectors.toList()),
                    containsInAnyOrder("1234567890123", "3456789012345"))
            );
        }
    }






}