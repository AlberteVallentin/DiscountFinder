package dat;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.config.Populate;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreRestTest {
    private static Javalin app;
    private static final String BASE_URL = "http://localhost:7777/api";
    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUpAll() {
        HibernateConfig.setTest(true);
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        app = ApplicationConfig.startServer(7777);
        RestAssured.baseURI = BASE_URL;

        // Populate test data
        Populate.populateAll(emf);
    }

    @AfterAll
    static void tearDownAll() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Get all stores (public) - Should return list of stores")
    void testGetAllStores() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].name", notNullValue())
            .body("[0].address", notNullValue())
            .body("[0].brand", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Get specific store (public) - Should return store with id 1")
    void testGetSpecificStore() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("name", notNullValue())
            .body("address", notNullValue())
            .body("brand", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("Get stores by postal code (public) - Should return stores in 2300")
    void testGetStoresByPostalCode() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores/postal_code/2300")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(4)
    @DisplayName("Get specific store - Non-existing ID should return 404")
    void testGetNonExistingStore() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores/999999")
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("warning", equalTo("Store not found with ID: 999999"));
    }

    @Test
    @Order(5)
    @DisplayName("Get stores by invalid postal code - Should return 404")
    void testGetStoresByInvalidPostalCode() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores/postal_code/9999")
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("warning", equalTo("No stores found for postal code: 9999"));
    }

    @Test
    @Order(6)
    @DisplayName("Get stores by postal code - Invalid format should return 400")
    void testGetStoresByInvalidPostalCodeFormat() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/stores/postal_code/invalid")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("warning", equalTo("Invalid postal code format"));
    }
}