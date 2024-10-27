package dat.daos.impl;

import dat.config.HibernateConfig;
import dat.dtos.*;
import dat.entities.*;
import dat.enums.StockUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreDAOTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreDAOTest.class);
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
        cleanDatabase();
        setupInitialData();
    }

    private void cleanDatabase() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Delete all entities in correct order
            em.createQuery("DELETE FROM Product").executeUpdate();
            em.createQuery("DELETE FROM Price").executeUpdate();
            em.createQuery("DELETE FROM Stock").executeUpdate();
            em.createQuery("DELETE FROM Timing").executeUpdate();
            em.createQuery("DELETE FROM Category").executeUpdate();
            em.createQuery("DELETE FROM Store").executeUpdate();
            em.createQuery("DELETE FROM Address").executeUpdate();
            em.createQuery("DELETE FROM PostalCode").executeUpdate();
            em.createQuery("DELETE FROM Brand").executeUpdate();

            // Reset sequences
            em.createNativeQuery("ALTER SEQUENCE products_product_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE prices_price_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE stocks_stock_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE timings_timing_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE categories_category_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE stores_store_id_seq RESTART WITH 1").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE address_address_id_seq RESTART WITH 1").executeUpdate();

            em.getTransaction().commit();
            em.clear();
            emf.getCache().evictAll();

            Thread.sleep(100);
        } catch (Exception e) {
            LOGGER.error("Error cleaning database", e);
            throw new RuntimeException("Failed to clean database", e);
        }
    }

    private void setupInitialData() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

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
            em.clear();

            createTestStores();
        } catch (Exception e) {
            LOGGER.error("Error setting up initial data", e);
            throw new RuntimeException("Failed to setup initial data", e);
        }
    }

    private void createTestStores() {
        try (EntityManager em = emf.createEntityManager()) {
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

            storeDAO.create(s1);
            storeDAO.create(s2);
        } catch (Exception e) {
            LOGGER.error("Error creating test stores", e);
            throw new RuntimeException("Failed to create test stores", e);
        }
    }

    @AfterEach
    void cleanupAfterTest() {
        try (EntityManager em = emf.createEntityManager()) {
            em.clear();
            emf.getCache().evictAll();
        }
    }

    @AfterAll
    void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test create store")
    void testCreate() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            Brand bilkaBrand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", "BILKA")
                .getSingleResult();

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

            StoreDTO createdStore = storeDAO.create(s3);

            em.flush();
            em.getTransaction().commit();

            assertThat(createdStore.getId(), is(notNullValue()));
            assertThat(createdStore.getName(), is(s3.getName()));
            assertThat(createdStore.getBrand().getName(), is(bilkaBrandDTO.getName()));
            assertThat(createdStore.getBrand().getDisplayName(), is(bilkaBrandDTO.getDisplayName()));

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(2)
    void testRead() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            StoreDTO store = storeDAO.read(1L);

            em.flush();
            em.getTransaction().commit();

            assertThat(store, is(notNullValue()));
            assertThat(store.getName(), is(s1.getName()));
            assertThat(store.getBrand().getName(), is(s1.getBrand().getName()));
            assertThat(store.getBrand().getDisplayName(), is(s1.getBrand().getDisplayName()));

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(3)
    void testReadAll() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            List<StoreDTO> stores = storeDAO.readAll();

            em.flush();
            em.getTransaction().commit();

            assertThat(stores, hasSize(2));
            assertThat(stores.stream()
                    .map(StoreDTO::getName)
                    .collect(Collectors.toList()),
                containsInAnyOrder("Netto Østerbro", "Føtex Nørrebro"));
            assertThat(stores.stream()
                    .map(store -> store.getBrand().getName())
                    .collect(Collectors.toList()),
                containsInAnyOrder("NETTO", "FOETEX"));

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(4)
    void testUpdate() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            s1.setName("Updated Netto");
            s1.setHasProductsInDb(true);

            StoreDTO updatedStore = storeDAO.update(1L, s1);

            em.flush();
            em.getTransaction().commit();

            assertThat(updatedStore, is(notNullValue()));
            assertThat(updatedStore.getName(), is("Updated Netto"));
            assertThat(updatedStore.hasProductsInDb(), is(true));
            assertThat(updatedStore.getBrand().getName(), is("NETTO"));

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(5)
    void testDelete() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            storeDAO.delete(1L);
            em.flush();

            StoreDTO deletedStore = storeDAO.read(1L);

            em.getTransaction().commit();

            assertThat(deletedStore, is(nullValue()));

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test updateStoreProducts basic functionality")
    void testUpdateStoreProducts() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            // Find store
            Store store = em.find(Store.class, 1L);
            assertThat("Store should exist", store, is(notNullValue()));

            // Create and persist categories first
            Category parentCategory = new Category("Dagligvarer", "Groceries",
                "Dagligvarer", "Groceries");
            Category childCategory = new Category("Mejeri", "Dairy",
                "Dagligvarer>Mejeri", "Groceries>Dairy");

            em.persist(parentCategory);
            em.persist(childCategory);
            em.flush();

            // Create initial product DTOs
            PriceDTO price1 = PriceDTO.builder()
                .originalPrice(new BigDecimal("20.00"))
                .newPrice(new BigDecimal("15.00"))
                .discount(new BigDecimal("5.00"))
                .percentDiscount(new BigDecimal("25.00"))
                .build();

            StockDTO stock1 = StockDTO.builder()
                .quantity(10.0)
                .stockUnit(StockUnit.EACH)
                .build();

            TimingDTO timing1 = TimingDTO.builder()
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .lastUpdated(LocalDateTime.now())
                .build();

            Set<CategoryDTO> categories = new HashSet<>();
            categories.add(CategoryDTO.builder()
                .id(parentCategory.getId())
                .nameDa("Dagligvarer")
                .nameEn("Groceries")
                .pathDa("Dagligvarer")
                .pathEn("Groceries")
                .build());

            categories.add(CategoryDTO.builder()
                .id(childCategory.getId())
                .nameDa("Mejeri")
                .nameEn("Dairy")
                .pathDa("Dagligvarer>Mejeri")
                .pathEn("Groceries>Dairy")
                .build());

            ProductDTO product1 = ProductDTO.builder()
                .productName("Milk")
                .ean("1234567890123")
                .price(price1)
                .stock(stock1)
                .timing(timing1)
                .categories(categories)
                .build();

            // Initial update with single product
            storeDAO.updateStoreProducts(store.getId(), List.of(product1));

            em.flush();
            em.clear();

            // Verify initial state
            Store storeWithInitialProduct = em.find(Store.class, store.getId());
            em.refresh(storeWithInitialProduct);

            assertThat("Store should have one product after initial update",
                storeWithInitialProduct.getProducts(), hasSize(1));

            // Create updated version with modified price and same categories
            PriceDTO updatedPrice = PriceDTO.builder()
                .originalPrice(new BigDecimal("25.00"))
                .newPrice(new BigDecimal("18.00"))
                .discount(new BigDecimal("7.00"))
                .percentDiscount(new BigDecimal("28.00"))
                .build();

            ProductDTO updatedProduct = ProductDTO.builder()
                .productName("Milk")
                .ean("1234567890123")
                .price(updatedPrice)
                .stock(stock1)
                .timing(timing1)
                .categories(categories)
                .build();

            // Perform update
            storeDAO.updateStoreProducts(store.getId(), List.of(updatedProduct));

            em.flush();
            em.clear();

            // Verify final state
            Store finalStore = em.find(Store.class, store.getId());
            em.refresh(finalStore);

            // Get the actual product for verification
            Product persistedProduct = finalStore.getProducts().iterator().next();

            assertAll(
                () -> assertThat("Store should have one product",
                    finalStore.getProducts(), hasSize(1)),
                () -> assertThat("Product EAN should match",
                    persistedProduct.getEan(), is("1234567890123")),
                () -> assertThat("Product name should match",
                    persistedProduct.getProductName(), is("Milk")),
                () -> assertThat("Original price should be updated",
                    persistedProduct.getPrice().getOriginalPrice(),
                    comparesEqualTo(new BigDecimal("25.00"))),
                () -> assertThat("New price should be updated",
                    persistedProduct.getPrice().getNewPrice(),
                    comparesEqualTo(new BigDecimal("18.00"))),
                () -> assertThat("Categories should be preserved",
                    persistedProduct.getCategories(), hasSize(2)),
                () -> {
                    Set<String> categoryPaths = new HashSet<>();
                    for(Category cat : persistedProduct.getCategories()) {
                        categoryPaths.add(cat.getPathDa());
                    }
                    assertThat("Category paths should match", categoryPaths,
                        containsInAnyOrder("Dagligvarer", "Dagligvarer>Mejeri"));
                }
            );

            em.getTransaction().commit();

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.error("Error in testUpdateStoreProducts", e);
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test updateStoreProducts removes obsolete products")
    void testUpdateStoreProductsRemovesObsoleteProducts() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            // Find en butik og opret produkter direkte med korrekt reference til butikken
            Store store = em.find(Store.class, 1L);

            // Opret første produkt
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

            // Opret andet produkt
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

            // Skab produkter og tilknyt dem til butikken
            Product product1 = new Product(productDto1);
            product1.setStore(store);
            Product product2 = new Product(productDto2);
            product2.setStore(store);

            // Gem produkterne
            em.persist(product1);
            em.persist(product2);
            em.flush();
            em.getTransaction().commit();

            // Start ny transaktion for update
            em.getTransaction().begin();

            // Simuler API-data: kun "Milk" findes stadig
            List<ProductDTO> updatedProducts = List.of(productDto1);

            // Kør opdateringsmetoden
            storeDAO.updateStoreProducts(store.getId(), updatedProducts);

            em.flush();
            em.clear();

            // Refresh butikken
            Store updatedStore = em.find(Store.class, store.getId());
            em.refresh(updatedStore);

            em.getTransaction().commit();

            // Assert
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

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test updateStoreProducts adds new products and removes obsolete ones")
    void testUpdateStoreProductsAddsNewAndRemovesObsoleteProducts() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            // Find butik
            Store store = em.find(Store.class, 1L);

            // Opret initialt produkt
            ProductDTO existingProduct = ProductDTO.builder()
                .productName("Milk")
                .ean("1234567890123")
                .price(new PriceDTO(null, new BigDecimal("20.00"), new BigDecimal("15.00"),
                    new BigDecimal("5.00"), new BigDecimal("25.00")))
                .timing(TimingDTO.builder()
                    .startTime(LocalDateTime.now().minusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1))
                    .lastUpdated(LocalDateTime.now())
                    .build())
                .categories(Set.of(new CategoryDTO(null, "Mejeri", "Dairy",
                    "Dagligvarer/Mejeri", "Groceries/Dairy")))
                .build();

            Product product1 = new Product(existingProduct);
            product1.setStore(store);
            em.persist(product1);
            em.flush();
            em.getTransaction().commit();

            // Start ny transaktion for update
            em.getTransaction().begin();

            // Simuler API-response med både eksisterende og nyt produkt
            List<ProductDTO> updatedProducts = List.of(
                existingProduct,
                ProductDTO.builder()
                    .productName("Cheese")
                    .ean("3456789012345")
                    .price(new PriceDTO(null, new BigDecimal("40.00"), new BigDecimal("30.00"),
                        new BigDecimal("10.00"), new BigDecimal("25.00")))
                    .timing(TimingDTO.builder()
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(5))
                        .lastUpdated(LocalDateTime.now())
                        .build())
                    .categories(Set.of(new CategoryDTO(null, "Mejeri", "Dairy",
                        "Dagligvarer/Mejeri", "Groceries/Dairy")))
                    .build()
            );

            // Udfør update
            storeDAO.updateStoreProducts(store.getId(), updatedProducts);

            em.flush();
            em.clear();

            // Refresh butik
            Store updatedStore = em.find(Store.class, store.getId());
            em.refresh(updatedStore);

            em.getTransaction().commit();

            // Assert
            assertAll(
                () -> assertThat("Two products should exist", updatedStore.getProducts(), hasSize(2)),
                () -> assertThat(updatedStore.getProducts().stream()
                        .map(Product::getEan)
                        .collect(Collectors.toList()),
                    containsInAnyOrder("1234567890123", "3456789012345"))
            );

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}