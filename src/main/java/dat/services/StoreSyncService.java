package dat.services;

import dat.daos.impl.StoreDAO;
import dat.daos.impl.ProductDAO;
import dat.dtos.StoreDTO;
import dat.dtos.ProductDTO;
import dat.entities.Store;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManagerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StoreSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreSyncService.class);
    private final SallingFetcher sallingFetcher;
    private final StoreDAO storeDAO;
    private final ProductDAO productDAO;
    private final ScheduledExecutorService scheduler;

    private static StoreSyncService instance;
    private static EntityManagerFactory emf;

    private StoreSyncService() throws ApiException {
        this.sallingFetcher = SallingFetcher.getInstance();
        this.storeDAO = StoreDAO.getInstance(emf);
        this.productDAO = ProductDAO.getInstance(emf);
        this.scheduler = Executors.newScheduledThreadPool(1);
        startDailySync();
    }

    public static StoreSyncService getInstance(EntityManagerFactory _emf) throws ApiException {
        if (instance == null) {
            emf = _emf;
            instance = new StoreSyncService();
        }
        return instance;
    }

    // Initial sync of all stores
    public void syncAllStores() throws ApiException {
        try {
            List<StoreDTO> sallingStores = sallingFetcher.fetchAllStoresSync();
            for (StoreDTO storeDTO : sallingStores) {
                Store existingStore = storeDAO.findBySallingId(storeDTO.getSallingStoreId());
                if (existingStore == null) {
                    // New store - create it
                    LOGGER.info("Creating new store: {}", storeDTO.getName());
                    storeDAO.create(storeDTO);
                } else {
                    // Existing store - update basic info but preserve hasProductsInDb
                    LOGGER.info("Updating existing store: {}", storeDTO.getName());
                    storeDTO.setHasProductsInDb(existingStore.hasProductsInDb());
                    storeDAO.update(existingStore.getId(), storeDTO);
                }
            }
            LOGGER.info("Successfully synced {} stores from Salling API", sallingStores.size());
        } catch (ApiException e) {
            LOGGER.error("Failed to sync stores from Salling API", e);
            throw e;
        }
    }

    // Fetch products for a specific store
    public void fetchProductsForStore(Long storeId) throws ApiException {
        try {
            StoreDTO storeDTO = storeDAO.read(storeId);
            if (storeDTO == null) {
                throw new ApiException(404, "Store not found with ID: " + storeId);
            }

            List<ProductDTO> products = sallingFetcher.fetchStoreProductsSync(storeDTO.getSallingStoreId());

            // Filter out invalid products
            List<ProductDTO> validProducts = products.stream()
                .filter(this::isValidProduct)
                .collect(Collectors.toList());

            if (validProducts.size() < products.size()) {
                LOGGER.warn("Filtered out {} invalid products for store {}",
                    products.size() - validProducts.size(), storeId);
            }

            updateStoreProducts(storeId, validProducts);

            // Mark store as having products
            storeDTO.setHasProductsInDb(true);
            storeDAO.update(storeId, storeDTO);

            LOGGER.info("Successfully fetched and processed {} valid products for store {}",
                validProducts.size(), storeId);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch products for store " + storeId, e);
            throw new ApiException(500, "Failed to fetch products: " + e.getMessage());
        }
    }

    // Daily sync of products for stores that have hasProductsInDb=true
    private void startDailySync() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime syncTime = now.withHour(3).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(syncTime)) {
            syncTime = syncTime.plusDays(1);
        }

        long initialDelay = Duration.between(now, syncTime).getSeconds();

        scheduler.scheduleAtFixedRate(
            this::syncProductsForActiveStores,
            initialDelay,
            TimeUnit.DAYS.toSeconds(1),
            TimeUnit.SECONDS
        );

        LOGGER.info("Scheduled daily product sync to run at 3 AM");
    }

    private void syncProductsForActiveStores() {
        try {
            List<StoreDTO> activeStores = storeDAO.findStoresWithProducts();
            LOGGER.info("Starting daily sync for {} active stores", activeStores.size());

            for (StoreDTO store : activeStores) {
                try {
                    List<ProductDTO> currentProducts = sallingFetcher.fetchStoreProductsSync(store.getSallingStoreId());
                    updateStoreProducts(store.getId(), currentProducts);
                    LOGGER.info("Successfully synced products for store: {}", store.getId());
                } catch (Exception e) {
                    LOGGER.error("Failed to sync products for store: " + store.getId(), e);
                }
            }

            LOGGER.info("Completed daily sync for all active stores");
        } catch (Exception e) {
            LOGGER.error("Error during daily product sync", e);
        }
    }

    private void updateStoreProducts(Long storeId, List<ProductDTO> newProducts) {
        // Get existing products
        List<ProductDTO> existingProducts = productDAO.findByStore(storeId);

        // Create map of EAN to new products for efficient lookup
        Map<String, ProductDTO> newProductMap = newProducts.stream()
            .filter(p -> p.getEan() != null && !p.getEan().isEmpty())
            .collect(Collectors.toMap(
                ProductDTO::getEan,
                p -> p,
                (existing, replacement) -> {
                    LOGGER.warn("Duplicate EAN found: {}. Using most recent product.", existing.getEan());
                    return replacement;
                }
            ));

        // Delete products that no longer exist
        existingProducts.stream()
            .filter(p -> p.getEan() != null && !p.getEan().isEmpty())
            .filter(p -> !newProductMap.containsKey(p.getEan()))
            .forEach(p -> {
                LOGGER.info("Deleting product with EAN {} as it's no longer in Salling API", p.getEan());
                productDAO.delete(p.getId());
            });

        // Update or create new products
        for (ProductDTO newProduct : newProducts) {
            String ean = newProduct.getEan();
            if (ean == null || ean.isEmpty()) {
                LOGGER.warn("Skipping product without EAN: {}", newProduct.getProductName());
                continue;
            }

            Optional<ProductDTO> existingProduct = existingProducts.stream()
                .filter(p -> ean.equals(p.getEan()))
                .findFirst();

            if (existingProduct.isPresent()) {
                // Update existing product
                LOGGER.debug("Updating product with EAN: {}", ean);
                productDAO.update(existingProduct.get().getId(), newProduct);
            } else {
                // Create new product
                LOGGER.debug("Creating new product with EAN: {}", ean);
                productDAO.create(newProduct);
            }
        }
    }

    // Helper method to validate product data
    private boolean isValidProduct(ProductDTO product) {
        if (product.getEan() == null || product.getEan().isEmpty()) {
            LOGGER.warn("Invalid product data - missing EAN: {}", product.getProductName());
            return false;
        }

        if (product.getProductName() == null || product.getProductName().isEmpty()) {
            LOGGER.warn("Invalid product data - missing name for EAN: {}", product.getEan());
            return false;
        }

        if (product.getPrice() == null) {
            LOGGER.warn("Invalid product data - missing price for EAN: {}", product.getEan());
            return false;
        }

        return true;
    }

    // Cleanup method to be called on application shutdown
    public void shutdown() {
        LOGGER.info("Shutting down StoreSyncService...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                LOGGER.warn("Forced shutdown of scheduler after timeout");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while shutting down scheduler", e);
        }
    }
}