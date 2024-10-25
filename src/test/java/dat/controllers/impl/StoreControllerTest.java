//package dat.controllers.impl;
//
//import dat.config.ApplicationConfig;
//import dat.config.HibernateConfig;
//import dat.dtos.StoreDTO;
//import dat.entities.Store;
//import dat.security.controllers.SecurityController;
//import dat.security.daos.SecurityDAO;
//import dat.security.exceptions.ValidationException;
//import dat.security.token.UserDTO;
//import io.javalin.Javalin;
//import io.restassured.common.mapper.TypeRef;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.EntityManagerFactory;
//import org.junit.jupiter.api.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.*;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class StoreControllerTest {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(StoreControllerTest.class);
//    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
//    private static final SecurityController securityController = SecurityController.getInstance();
//    private static final SecurityDAO securityDAO = new SecurityDAO(emf);
//    private static Javalin app;
//    private static Store[] stores;
//    private static Store netto, foetex, bilka;
//    private static UserDTO userDTO, adminDTO;
//    private static String userToken, adminToken;
//    private static final String BASE_URL = "http://localhost:7070/api";
//
//    @BeforeAll
//    void setUpAll() {
//        HibernateConfig.setTest(true);
//
//        // Start api
//        app = ApplicationConfig.startServer(7070);
//    }
//
//    @BeforeEach
//    void setUp() {
//        // Først opretter vi brugere
//        LOGGER.info("Populating database with users");
//        UserDTO[] users = Populator.populateUsers(emf);
//        userDTO = users[0];
//        adminDTO = users[1];
//
//        try {
//            // Så verificerer vi brugerne
//            UserDTO verifiedUser = securityDAO.getVerifiedUser(userDTO.getEmail(), "test123");
//            UserDTO verifiedAdmin = securityDAO.getVerifiedUser(adminDTO.getEmail(), "admin123");
//            userToken = "Bearer " + securityController.createToken(verifiedUser);
//            adminToken = "Bearer " + securityController.createToken(verifiedAdmin);
//
//            // Til sidst opretter vi butikkerne
//            LOGGER.info("Populating database with stores");
//            stores = Populator.populateStores(emf);
//            netto = stores[0];
//            foetex = stores[1];
//            bilka = stores[2];
//        } catch (ValidationException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @AfterEach
//    void tearDown() {
//        try (EntityManager em = emf.createEntityManager()) {
//            em.getTransaction().begin();
//            // Først sletter vi butikker og relaterede entiteter
//            em.createQuery("DELETE FROM Store").executeUpdate();
//            em.createQuery("DELETE FROM Address").executeUpdate();
//            em.createQuery("DELETE FROM PostalCode").executeUpdate();
//            em.createQuery("DELETE FROM Brand").executeUpdate();
//
//            // Så sletter vi brugere og roller
//            em.createQuery("DELETE FROM User").executeUpdate();
//            em.createQuery("DELETE FROM Role").executeUpdate();
//            em.getTransaction().commit();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @AfterAll
//    void tearDownAll() {
//        ApplicationConfig.stopServer(app);
//    }
//
//    @Test
//    void getAllStores() {
//        List<StoreDTO> fetchedStores =
//            given()
//                .when()
//                .header("Authorization", userToken)
//                .get(BASE_URL + "/stores")
//                .then()
//                .statusCode(200)
//                .body("size()", is(3))
//                .log().all()
//                .extract()
//                .as(new TypeRef<List<StoreDTO>>() {});
//
//        assertThat(fetchedStores.size(), is(3));
//        assertThat(fetchedStores.stream().map(StoreDTO::getName).toList(),
//            containsInAnyOrder(netto.getName(), foetex.getName(), bilka.getName()));
//    }
//
//    @Test
//    void getStoreById() {
//        StoreDTO fetchedStore =
//            given()
//                .when()
//                .header("Authorization", userToken)
//                .get(BASE_URL + "/stores/" + netto.getId())
//                .then()
//                .statusCode(200)
//                .extract()
//                .as(StoreDTO.class);
//
//        assertThat(fetchedStore.getName(), is(netto.getName()));
//    }
//
//    @Test
//    void getStoresByPostalCodeWithoutToken() {
//        int postalCode = netto.getAddress().getPostalCode().getPostalCode();
//        given()
//            .when()
//            .get(BASE_URL + "/stores/postal_code/" + postalCode)
//            .then()
//            .statusCode(401);
//    }
//
//    @Test
//    void getStoresByPostalCodeWithUserToken() {
//        int postalCode = netto.getAddress().getPostalCode().getPostalCode();
//        List<StoreDTO> stores = given()
//            .header("Authorization", userToken)
//            .when()
//            .get(BASE_URL + "/stores/postal_code/" + postalCode)
//            .then()
//            .statusCode(200)
//            .extract()
//            .as(new TypeRef<List<StoreDTO>>() {});
//
//        assertThat(stores, is(not(empty())));
//        stores.forEach(store ->
//            assertThat(store.getAddress().getPostalCode().getPostalCode(), is(postalCode)));
//    }
//
//    @Test
//    void createStoreWithoutToken() {
//        given()
//            .when()
//            .post(BASE_URL + "/stores")
//            .then()
//            .statusCode(401);
//    }
//
//    @Test
//    void createStoreWithUserToken() {
//        given()
//            .header("Authorization", userToken)
//            .when()
//            .post(BASE_URL + "/stores")
//            .then()
//            .statusCode(401);
//    }
//
//    @Test
//    void getNonExistentStore() {
//        given()
//            .when()
//            .get(BASE_URL + "/stores/999999")
//            .then()
//            .statusCode(404);
//    }
//}
