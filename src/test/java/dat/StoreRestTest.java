package dat;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.dtos.StoreDTO;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreRestTest {
    private static Javalin app;
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static List<StoreDTO> testStores;
    private static final String BASE_URL = "http://localhost:7070/api/stores";
    private static final Logger logger = LoggerFactory.getLogger(StoreRestTest.class);

    @BeforeAll
    void setUpAll() {
        app = ApplicationConfig.startServer(7070);
        TestPopulators.setEntityManagerFactory(emf);
    }

    @BeforeEach
    void setUp() {
        testStores = new ArrayList<>(TestPopulators.populateStores());
    }

    @AfterEach
    void tearDown() {
        TestPopulators.cleanUpStores();
    }

    @AfterAll
    void tearDownAll() {
        ApplicationConfig.stopServer(app);
    }

    @Test
    void testGetAllStores() {
        given()
            .contentType("application/json")
            .when()
            .get(BASE_URL)
            .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("[0]", hasKey("id"))
            .body("[0]", hasKey("name"))
            .body("[0]", hasKey("brand"))
            .body("[0]", hasKey("address"));
    }

    @Test
    void testGetStoreById() {
        StoreDTO store = testStores.get(0);
        given()
            .contentType("application/json")
            .when()
            .get(BASE_URL + "/" + store.getId())
            .then()
            .statusCode(200)
            .body("id", is(store.getId().intValue()))
            .body("name", is(store.getName()))
            .body("brand.displayName", is(store.getBrand().getDisplayName()))
            .body("address.addressLine", is(store.getAddress().getAddressLine()));
    }

    @Test
    void testGetStoreById_NotFound() {
        String responseBody = given()
            .contentType("application/json")
            .when()
            .get(BASE_URL + "/999999")
            .then()
            .statusCode(404)  // Forvent 404 i stedet for 400
            .extract().body().asString();

        System.out.println("Response body: " + responseBody);

        given()
            .contentType("application/json")
            .when()
            .get(BASE_URL + "/999999")
            .then()
            .statusCode(404)  // Justering af statuskoden
            .body("warning", containsString("Store not found with ID"));
    }

    @Test
    void testGetStoreById_InvalidId() {
        given()
            .contentType("application/json")
            .when()
            .get(BASE_URL + "/invalid")
            .then()
            .statusCode(400)
            .body("warning", notNullValue())
            .body("status", is("400"));
    }

    @Test
    void testCreateStore() {
        StoreDTO newStore = TestPopulators.createTestStoreDTO(
            "9999", "Test Store",
            "Test Street 1", 2800, "Test City",
            12.34, 56.78,
            "NETTO", "Netto"
        );

        StoreDTO created = given()
            .contentType("application/json")
            .body(newStore)
            .when()
            .post(BASE_URL)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is(newStore.getName()))
            .body("brand.displayName", is(newStore.getBrand().getDisplayName()))
            .body("address.addressLine", is(newStore.getAddress().getAddressLine()))
            .extract()
            .as(StoreDTO.class);

        assertThat(created.getId(), notNullValue());
    }

    @Test
    void testCreateStore_InvalidData() {
        StoreDTO invalidStore = new StoreDTO(); // Missing required fields

        given()
            .contentType("application/json")
            .body(invalidStore)
            .when()
            .post(BASE_URL)
            .then()
            .statusCode(400)
            .body("warning", containsString("must be set"));
    }

    @Test
    void testUpdateStore() {
        StoreDTO store = testStores.get(0);
        store.setName("Updated Name");

        given()
            .contentType("application/json")
            .body(store)
            .when()
            .put(BASE_URL + "/" + store.getId())
            .then()
            .statusCode(200)
            .body("id", is(store.getId().intValue()))
            .body("name", is("Updated Name"))
            .body("brand.displayName", is(store.getBrand().getDisplayName()))
            .body("address.addressLine", is(store.getAddress().getAddressLine()));
    }

    @Test
    void testUpdateStore_NotFound() {
        StoreDTO store = testStores.get(0);
        store.setId(999999L);

        given()
            .contentType("application/json")
            .body(store)
            .when()
            .put(BASE_URL + "/999999")
            .then()
            .statusCode(404)  // Forvent 404 i stedet for 400
            .body("warning", containsString("Store not found with ID"));
    }

    @Test
    void testDeleteStore() {
        StoreDTO store = testStores.get(0);

        given()
            .contentType("application/json")
            .when()
            .delete(BASE_URL + "/" + store.getId())
            .then()
            .statusCode(204);

        // Verify store is deleted
        given()
            .contentType("application/json")
            .when()
            .get(BASE_URL + "/" + store.getId())
            .then()
            .statusCode(404)  // Forvent 404 efter sletning
            .body("warning", containsString("Store not found with ID"));
    }

    @Test
    void testDeleteStore_NotFound() {
        given()
            .contentType("application/json")
            .when()
            .delete(BASE_URL + "/999999")
            .then()
            .statusCode(404)  // Forvent 404 i stedet for 400
            .body("warning", containsString("Store not found with ID"));
    }
}
