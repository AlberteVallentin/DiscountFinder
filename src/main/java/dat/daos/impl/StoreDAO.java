package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.CategoryDTO;
import dat.dtos.ProductDTO;
import dat.dtos.StoreDTO;
import dat.entities.*;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            Store store = findById(id);
            return store != null ? new StoreDTO(store, true) : null;  // Include products for single store fetch
        }
    }

    @Override
    public List<StoreDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery("SELECT s FROM Store s", Store.class);
            return query.getResultList().stream()
                .map(store -> new StoreDTO(store, false)) // Don't include products
                .collect(Collectors.toList());
        }
    }

    public Store findByIdWithFavorites(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Store store = em.createQuery(
                    "SELECT DISTINCT s FROM Store s " +
                        "LEFT JOIN FETCH s.products p " +
                        "LEFT JOIN FETCH s.favoredByUsers u " +
                        "LEFT JOIN FETCH p.price " +
                        "LEFT JOIN FETCH p.stock " +
                        "LEFT JOIN FETCH p.timing " +
                        "LEFT JOIN FETCH p.categories " +
                        "WHERE s.id = :id", Store.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (store != null) {
                LOGGER.debug("Found store {} with {} favoredByUsers",
                    store.getId(),
                    store.getFavoredByUsers() != null ? store.getFavoredByUsers().size() : 0);
            }

            return store;
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

    public void updateStoreProducts(Long id, List<ProductDTO> products) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Store store = em.find(Store.class, id);
            if (store == null) {
                throw new IllegalArgumentException("Store not found with ID: " + id);
            }

            LOGGER.info("Updating products for store {} ({}) with Salling ID: {}",
                id, store.getName(), store.getSallingStoreId());

            // Map eksisterende produkter efter EAN for hurtig opslag
            Map<String, Product> existingProductMap = store.getProducts().stream()
                .collect(Collectors.toMap(Product::getEan, p -> p));

            // Find EANs for nye produkter
            Set<String> newProductEans = products.stream()
                .map(ProductDTO::getEan)
                .collect(Collectors.toSet());

            // Fjern produkter der ikke længere findes i API-listen
            List<Product> productsToRemove = store.getProducts().stream()
                .filter(p -> !newProductEans.contains(p.getEan()))
                .collect(Collectors.toList());

            // Fjern gamle produkter
            for (Product product : productsToRemove) {
                product.clearCategories();  // Fjern kategori-relationer først
                store.getProducts().remove(product);
                em.remove(product);
            }

            // Tilføj eller opdater produkter
            for (ProductDTO dto : products) {
                Product product = existingProductMap.get(dto.getEan());

                if (product == null) {
                    // Opret nyt produkt
                    product = new Product(dto);
                    product.setStore(store);
                    store.getProducts().add(product);
                    em.persist(product);
                } else {
                    // Opdater eksisterende produkt
                    product.updateFromDTO(dto);
                }

                // Håndter kategorier
                if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
                    Set<Category> categories = new HashSet<>();

                    for (CategoryDTO catDTO : dto.getCategories()) {
                        // Find eksisterende kategori eller opret ny
                        Category category = em.createQuery(
                                "SELECT c FROM Category c WHERE c.pathDa = :pathDa AND c.pathEn = :pathEn",
                                Category.class)
                            .setParameter("pathDa", catDTO.getPathDa())
                            .setParameter("pathEn", catDTO.getPathEn())
                            .getResultStream()
                            .findFirst()
                            .orElseGet(() -> {
                                Category newCategory = new Category(catDTO);
                                em.persist(newCategory);
                                return newCategory;
                            });

                        categories.add(category);
                    }

                    // Opdater produktets kategorier
                    product.clearCategories();
                    categories.forEach(product::addCategory);
                }

                em.merge(product);
            }

            store.setHasProductsInDb(true);
            store.setLastFetched(LocalDateTime.now());
            em.merge(store);
            em.flush();
            em.getTransaction().commit();

            LOGGER.info("Successfully updated {} products for store {}",
                products.size(), store.getName());
        } catch (Exception e) {
            LOGGER.error("Error updating products for store {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update store products: " + e.getMessage());
        }
    }


    private void updateProductCategories(Product product, ProductDTO dto, EntityManager em) {
        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            // Bevar den eksisterende kategori samling hvis den findes
            if (product.getCategories() == null) {
                product.setCategories(new HashSet<>());
            }

            // Clear eksisterende kategorier
            product.clearCategories();

            for (CategoryDTO categoryDTO : dto.getCategories()) {
                // Find først eksisterende kategori eller opret ny
                TypedQuery<Category> query = em.createQuery(
                        "SELECT c FROM Category c WHERE c.pathDa = :pathDa AND c.pathEn = :pathEn",
                        Category.class)
                    .setParameter("pathDa", categoryDTO.getPathDa())
                    .setParameter("pathEn", categoryDTO.getPathEn());

                Category category = query.getResultStream()
                    .findFirst()
                    .orElseGet(() -> {
                        Category newCategory = new Category(categoryDTO);
                        em.persist(newCategory);
                        return newCategory;
                    });

                // Tilføj relationen begge veje
                product.addCategory(category);
                category.getProducts().add(product);
            }

            // Merge produktet for at sikre ændringerne bliver gemt
            em.merge(product);
        }
    }
    public Store findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT DISTINCT s FROM Store s " +
                    "LEFT JOIN FETCH s.products p " +
                    "LEFT JOIN FETCH p.price " +
                    "LEFT JOIN FETCH p.stock " +
                    "LEFT JOIN FETCH p.timing " +
                    "LEFT JOIN FETCH p.categories " +
                    "WHERE s.id = :id", Store.class);
            query.setParameter("id", id);
            Store store = query.getResultStream().findFirst().orElse(null);

            if (store != null) {
                LOGGER.debug("Found store {} with {} products", store.getName(),
                    store.getProducts().size());
                for (Product p : store.getProducts()) {
                    LOGGER.debug("Product {} has {} categories", p.getProductName(),
                        p.getCategories().size());
                }
            }

            return store;
        }
    }

    private void cleanupUnusedCategories(EntityManager em) {
        List<Category> unusedCategories = em.createQuery(
                "SELECT c FROM Category c WHERE c.products IS EMPTY",
                Category.class)
            .getResultList();

        if (!unusedCategories.isEmpty()) {
            LOGGER.info("Found {} unused categories to remove", unusedCategories.size());
            for (Category category : unusedCategories) {
                LOGGER.debug("Removing unused category: {} ({}) with path: {} / {}",
                    category.getNameDa(),
                    category.getNameEn(),
                    category.getPathDa(),
                    category.getPathEn());
                em.remove(category);
            }
        }
    }
}