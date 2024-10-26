package dat.controllers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.entities.Store;
import dat.exceptions.ApiException;
import dat.services.ProductFetcher;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            Store store = storeDAO.findById(id);
            if (store == null) {
                throw new ApiException(404, "Store not found with ID: " + id);
            }

            if (store.needsProductUpdate()) {
                LOGGER.info("Fetching products for store {} ({})", store.getId(), store.getName());
                try {
                    var products = productFetcher.fetchProductsForStore(store.getSallingStoreId());
                    storeDAO.updateStoreProducts(store.getId(), products);
                    store = storeDAO.findById(id);
                } catch (Exception e) {
                    LOGGER.error("Error fetching products for store {}: {}", id, e.getMessage());
                    throw new ApiException(500, "Error fetching products: " + e.getMessage());
                }
            }

            StoreDTO storeDTO = new StoreDTO(store, true);
            String jsonOutput = objectMapper.writeValueAsString(storeDTO);
            ctx.contentType("application/json").result(jsonOutput);

        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid store ID format");
        } catch (Exception e) {
            throw new ApiException(500, e.getMessage());
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