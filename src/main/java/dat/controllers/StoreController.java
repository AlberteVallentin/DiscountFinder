package dat.controllers;

import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StoreController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreController.class);
    private static StoreController instance;
    private final StoreDAO storeDAO;

    private StoreController() {
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    public static StoreController getInstance() {
        if (instance == null) {
            instance = new StoreController();
        }
        return instance;
    }

    public void getAllStores(Context ctx) throws ApiException {
        try {
            List<StoreDTO> stores = storeDAO.readAll();
            LOGGER.info("Retrieved {} stores", stores.size());
            ctx.json(stores);
        } catch (Exception e) {
            LOGGER.error("Error fetching stores", e);
            throw new ApiException(500, "Error fetching stores: " + e.getMessage());
        }
    }

    public void getStoreById(Context ctx) throws ApiException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            StoreDTO store = storeDAO.read(id);
            if (store == null) {
                throw new ApiException(404, "Store not found with ID: " + id);
            }
            ctx.json(store);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid store ID format");
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}