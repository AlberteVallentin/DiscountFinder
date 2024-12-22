package dat.controllers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.entities.Category;
import dat.entities.Store;
import dat.exceptions.ApiException;
import dat.security.token.UserDTO;
import dat.services.ProductFetcher;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class StoreController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreController.class);
    private final StoreDAO storeDAO;
    private final ProductFetcher productFetcher;
    private final ObjectMapper objectMapper;

    public StoreController() {
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
        this.productFetcher = ProductFetcher.getInstance();
        this.objectMapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    public void read(Context ctx) throws ApiException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            LOGGER.info("Fetching store with ID: {}", id);

            UserDTO userDTO = ctx.attribute("user");
            String userEmail = userDTO != null ? userDTO.getEmail() : null;

            Store store = userEmail != null ?
                storeDAO.findByIdWithFavorites(id) :
                storeDAO.findById(id);

            if (store == null) {
                throw new ApiException(404, "Store not found with ID: " + id);
            }

            // Tilføj debug logging her
            LOGGER.info("Store {} update check - hasProductsInDb: {}, lastFetched: {}, needsUpdate: {}",
                id,
                store.hasProductsInDb(),
                store.getLastFetched(),
                store.needsProductUpdate());

            if (store.needsProductUpdate()) {
                LOGGER.info("Fetching fresh products from Salling API for store {} ({})",
                    store.getId(), store.getSallingStoreId());
                try {
                    var products = productFetcher.fetchProductsForStore(store.getSallingStoreId());
                    storeDAO.updateStoreProducts(store.getId(), products);

                    // Refresh store data after product update
                    store = userEmail != null ?
                        storeDAO.findByIdWithFavorites(id) :
                        storeDAO.findById(id);
                } catch (Exception e) {
                    LOGGER.error("Error updating products for store {}: {}", id, e.getMessage());
                }
            } else {
                LOGGER.info("Using cached products for store {} (last update: {})",
                    store.getId(), store.getLastFetched());
            }

            StoreDTO storeDTO = new StoreDTO(store, true, userEmail);
            ctx.json(storeDTO);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error in store read endpoint: ", e);
            throw new ApiException(500, "Internal server error");
        }
    }


    public void readAll(Context ctx) throws ApiException {
        try {
            var stores = storeDAO.readAll();
            LOGGER.info("Retrieved {} stores", stores.size());
            ctx.json(stores);
        } catch (Exception e) {
            LOGGER.error("Error fetching stores", e);
            throw new ApiException(500, "Error fetching stores: " + e.getMessage());
        }
    }

    public void getStoresByPostalCode(Context ctx) throws ApiException {
        try {
            Integer postalCode = Integer.parseInt(ctx.pathParam("postal_code"));
            var stores = storeDAO.findByPostalCode(postalCode);
            LOGGER.info("Found {} stores in postal code {}", stores.size(), postalCode);
            ctx.json(stores);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid postal code format");
        } catch (Exception e) {
            LOGGER.error("Error fetching stores by postal code", e);
            throw new ApiException(500, "Error fetching stores: " + e.getMessage());
        }
    }

}




