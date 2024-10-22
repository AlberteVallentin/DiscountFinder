package dat.controllers;

import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import dat.services.StoreSyncService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StoreSyncController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreSyncController.class);
    private static StoreSyncController instance;
    private final StoreSyncService syncService;
    private final StoreDAO storeDAO;

    private StoreSyncController() throws ApiException {
        this.syncService = StoreSyncService.getInstance(HibernateConfig.getEntityManagerFactory());
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    public static StoreSyncController getInstance() throws ApiException {
        if (instance == null) {
            instance = new StoreSyncController();
        }
        return instance;
    }

    public void syncAllStores(Context ctx) throws ApiException {
        try {
            syncService.syncAllStores();
            // Hent alle butikker efter sync og returner dem
            List<StoreDTO> stores = storeDAO.readAll();
            ctx.status(200).json(stores);
        } catch (ApiException e) {
            LOGGER.error("Failed to sync stores", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during store sync", e);
            throw new ApiException(500, "Failed to sync stores: " + e.getMessage());
        }
    }

    public void syncStoreProducts(Context ctx) throws ApiException {
        try {
            Long storeId = Long.parseLong(ctx.pathParam("id"));
            StoreDTO store = storeDAO.read(storeId);
            if (store == null) {
                throw new ApiException(404, "Store not found with ID: " + storeId);
            }

            syncService.fetchProductsForStore(storeId);
            // Hent den opdaterede butik og returner den
            StoreDTO updatedStore = storeDAO.read(storeId);
            ctx.status(200).json(updatedStore);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid store ID format");
        } catch (ApiException e) {
            LOGGER.error("Failed to sync products for store", e);
            throw e;
        }
    }

    public void getAllStores(Context ctx) throws ApiException {
        try {
            List<StoreDTO> stores = storeDAO.readAll();
            ctx.json(stores);
        } catch (Exception e) {
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
        } catch (NumberFormatException | ApiException e) {
            throw new ApiException(400, "Invalid store ID format");
        }
    }
}