package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.ProductDTO;
import dat.dtos.StoreDTO;
import dat.entities.Product;
import dat.entities.Store;
import dat.entities.Address;
import dat.entities.PostalCode;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StoreDAO implements IDAO<StoreDTO, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreDAO.class);
    private static StoreDAO instance;
    private static EntityManagerFactory emf;

    private StoreDAO() {
    }

    public static StoreDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new StoreDAO();
        }
        return instance;
    }

    @Override
    public StoreDTO read(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Store store = em.find(Store.class, id);
            return store != null ? new StoreDTO(store) : null;
        }
    }

    @Override
    public List<StoreDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery("SELECT s FROM Store s", Store.class);
            return query.getResultList().stream()
                .map(StoreDTO::new)
                .collect(Collectors.toList());
        }
    }

    @Override
    public StoreDTO create(StoreDTO storeDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Find existing store by Salling ID
            Store existingStore = findBySallingId(storeDTO.getSallingStoreId());
            if (existingStore != null) {
                throw new IllegalStateException("Store with Salling ID already exists: " + storeDTO.getSallingStoreId());
            }

            // Find postal code
            PostalCode postalCode = findPostalCode(em, storeDTO.getAddress().getPostalCode().getPostalCode());

            // Create new address
            Address address = new Address(storeDTO.getAddress());
            address.setPostalCode(postalCode);

            // Create new store
            Store store = new Store(storeDTO);
            store.setAddress(address);

            em.persist(store);
            em.getTransaction().commit();

            return new StoreDTO(store);
        } catch (Exception e) {
            throw new PersistenceException("Could not create store: " + e.getMessage(), e);
        }
    }

    @Override
    public StoreDTO update(Long id, StoreDTO storeDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Store existingStore = em.find(Store.class, id);
            if (existingStore == null) {
                throw new EntityNotFoundException("Store not found with id: " + id);
            }

            PostalCode postalCode = findPostalCode(em, storeDTO.getAddress().getPostalCode().getPostalCode());

            Address address = existingStore.getAddress();
            if (address == null) {
                address = new Address(storeDTO.getAddress());
            } else {
                address.updateFromDTO(storeDTO.getAddress());
            }
            address.setPostalCode(postalCode);

            existingStore.updateFromSallingApi(storeDTO);
            existingStore.setAddress(address);

            em.merge(existingStore);
            em.getTransaction().commit();

            return new StoreDTO(existingStore);
        } catch (Exception e) {
            throw new PersistenceException("Could not update store: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Store store = em.find(Store.class, id);
            if (store != null) {
                em.remove(store);
            }
            em.getTransaction().commit();
        }
    }

    @Override
    public boolean validatePrimaryKey(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Store.class, id) != null;
        }
    }

    private PostalCode findPostalCode(EntityManager em, int postalCodeNumber) {
        PostalCode postalCode = em.find(PostalCode.class, postalCodeNumber);
        if (postalCode == null) {
            LOGGER.error("PostalCode {} not found in database", postalCodeNumber);
            throw new IllegalArgumentException("Invalid postal code: " + postalCodeNumber);
        }
        return postalCode;
    }

    public Store findBySallingId(String sallingStoreId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s WHERE s.sallingStoreId = :sallingId", Store.class);
            query.setParameter("sallingId", sallingStoreId);
            List<Store> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public void saveOrUpdateStores(List<StoreDTO> stores) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            for (StoreDTO storeDTO : stores) {
                try {
                    Store existingStore = findBySallingId(storeDTO.getSallingStoreId());
                    PostalCode postalCode = findPostalCode(em,
                        storeDTO.getAddress().getPostalCode().getPostalCode());

                    if (existingStore == null) {
                        Address address = new Address(storeDTO.getAddress());
                        address.setPostalCode(postalCode);

                        Store store = new Store(storeDTO);
                        store.setAddress(address);

                        em.persist(store);
                        LOGGER.debug("Created new store: {}", store.getName());
                    } else {
                        existingStore.updateFromSallingApi(storeDTO);
                        Address address = existingStore.getAddress();
                        address.updateFromDTO(storeDTO.getAddress());
                        address.setPostalCode(postalCode);

                        em.merge(existingStore);
                        LOGGER.debug("Updated existing store: {}", existingStore.getName());
                    }
                } catch (IllegalArgumentException e) {
                    // Log og fortsæt med næste butik
                    LOGGER.error("Error processing store {}: {}", storeDTO.getName(), e.getMessage());
                }
            }

            em.getTransaction().commit();
            LOGGER.info("Successfully processed {} stores", stores.size());
        } catch (Exception e) {
            throw new PersistenceException("Failed to save/update stores: " + e.getMessage(), e);
        }
    }

    public List<StoreDTO> findByPostalCode(Integer postalCode) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s WHERE s.address.postalCode.postalCode = :postalCode",
                Store.class);
            query.setParameter("postalCode", postalCode);
            return query.getResultList().stream()
                .map(StoreDTO::new)
                .collect(Collectors.toList());
        }
    }

    public Store findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s " +
                    "LEFT JOIN FETCH s.products p " +
                    "LEFT JOIN FETCH p.price " +
                    "LEFT JOIN FETCH p.stock " +
                    "LEFT JOIN FETCH p.timing " +
                    "LEFT JOIN FETCH p.categories " +
                    "WHERE s.id = :id", Store.class);
            query.setParameter("id", id);
            return query.getResultStream().findFirst().orElse(null);
        }
    }

    public void updateStoreProducts(Long id, List<ProductDTO> products) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Store store = em.find(Store.class, id);
            if (store == null) {
                throw new IllegalArgumentException("Store not found with ID: " + id);
            }

            LOGGER.info("Updating products for store {} ({}) with Salling ID: {}",
                id, store.getName(), store.getSallingStoreId());

            // Clear existing products but keep references for comparison
            Set<Product> existingProducts = new HashSet<>(store.getProducts());
            store.getProducts().clear();

            // Process each product from the API
            for (ProductDTO dto : products) {
                // Try to find existing product by EAN
                Product existingProduct = existingProducts.stream()
                    .filter(p -> p.getEan().equals(dto.getEan()))
                    .findFirst()
                    .orElse(null);

                if (existingProduct != null) {
                    // Update existing product
                    existingProduct.updateFromDTO(dto);
                    store.getProducts().add(existingProduct);
                    em.merge(existingProduct);
                    LOGGER.debug("Updated existing product: {}", dto.getProductName());
                } else {
                    // Create new product
                    Product newProduct = new Product(dto);
                    newProduct.setStore(store);
                    em.persist(newProduct);
                    store.getProducts().add(newProduct);
                    LOGGER.debug("Created new product: {}", dto.getProductName());
                }
            }

            // Remove products that no longer exist in the API
            Set<String> newEans = products.stream()
                .map(ProductDTO::getEan)
                .collect(Collectors.toSet());

            existingProducts.stream()
                .filter(p -> !newEans.contains(p.getEan()))
                .forEach(p -> {
                    LOGGER.debug("Removing obsolete product: {}", p.getProductName());
                    em.remove(p);
                });

            store.setHasProductsInDb(true);
            store.setLastFetched(LocalDateTime.now());

            em.merge(store);
            em.getTransaction().commit();

            LOGGER.info("Successfully updated {} products for store {} ({})",
                products.size(), id, store.getName());
        } catch (Exception e) {
            LOGGER.error("Error updating products for store {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update store products: " + e.getMessage(), e);
        }
    }
}
