package dat.services;

import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class StoreSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreSyncService.class);
    private final SallingFetcher sallingFetcher;
    private final StoreDAO storeDAO;

    private static StoreSyncService instance;
    private static EntityManagerFactory emf;

    private StoreSyncService() {
        this.sallingFetcher = SallingFetcher.getInstance();
        this.storeDAO = StoreDAO.getInstance(emf);
    }

    public static StoreSyncService getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new StoreSyncService();
        }
        return instance;
    }

    public void syncAllStores() throws ApiException {
        try {
            LOGGER.info("Starting store sync process");
            List<StoreDTO> sallingStores = sallingFetcher.fetchAllStoresSync();
            LOGGER.info("Fetched {} stores from Salling API", sallingStores.size());

            storeDAO.saveOrUpdateStores(sallingStores);
            LOGGER.info("Successfully synced all stores");
        } catch (Exception e) {
            LOGGER.error("Error during store sync", e);
            throw new ApiException(500, "Failed to sync stores: " + e.getMessage());
        }
    }

    public void fetchProductsForStore(Long storeId) throws ApiException {
        try {
            StoreDTO store = storeDAO.read(storeId);
            if (store == null) {
                throw new ApiException(404, "Store not found with ID: " + storeId);
            }

            // Her kan vi tilføje produkt synkronisering senere
            // Vi skal først implementere ProductDAO og den tilhørende logik

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error fetching products for store " + storeId, e);
            throw new ApiException(500, "Failed to fetch products: " + e.getMessage());
        }
    }
}