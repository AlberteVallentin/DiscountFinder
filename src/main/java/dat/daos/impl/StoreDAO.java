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
import java.util.Set;
import java.util.stream.Collectors;

public class StoreDAO implements IDAO<StoreDTO, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreDAO.class);
    private static StoreDAO instance;
    private static EntityManagerFactory emf;

    private StoreDAO() {}

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
            return store != null ? new StoreDTO(store, true) : null;
        }
    }

    @Override
    public List<StoreDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery("SELECT s FROM Store s", Store.class);
            return query.getResultList().stream()
                .map(store -> new StoreDTO(store, false))
                .collect(Collectors.toList());
        }
    }

    @Override
    public StoreDTO create(StoreDTO storeDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Store existingStore = findBySallingId(storeDTO.getSallingStoreId());
            if (existingStore != null) {
                throw new IllegalStateException("Store with Salling ID already exists: " + storeDTO.getSallingStoreId());
            }

            PostalCode postalCode = findPostalCode(em, storeDTO.getAddress().getPostalCode().getPostalCode());
            Address address = new Address(storeDTO.getAddress());
            address.setPostalCode(postalCode);

            Store store = new Store(storeDTO);
            store.setAddress(address);

            em.persist(store);
            em.getTransaction().commit();

            return new StoreDTO(store);
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
                Store existingStore = findBySallingId(storeDTO.getSallingStoreId());
                PostalCode postalCode = findPostalCode(em,
                    storeDTO.getAddress().getPostalCode().getPostalCode());

                if (existingStore == null) {
                    Address address = new Address(storeDTO.getAddress());
                    address.setPostalCode(postalCode);
                    Store store = new Store(storeDTO);
                    store.setAddress(address);
                    em.persist(store);
                } else {
                    existingStore.updateFromSallingApi(storeDTO);
                    Address address = existingStore.getAddress();
                    address.updateFromDTO(storeDTO.getAddress());
                    address.setPostalCode(postalCode);
                    em.merge(existingStore);
                }
            }

            em.getTransaction().commit();
            LOGGER.info("Processed {} stores", stores.size());
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

            Set<Product> existingProducts = new HashSet<>(store.getProducts());
            store.getProducts().clear();

            for (ProductDTO dto : products) {
                Product product = existingProducts.stream()
                    .filter(p -> p.getEan().equals(dto.getEan()))
                    .findFirst()
                    .orElse(new Product(dto));

                if (existingProducts.contains(product)) {
                    product.updateFromDTO(dto);
                }

                if (dto.getCategories() != null) {
                    product.getCategories().clear();
                    for (CategoryDTO categoryDTO : dto.getCategories()) {
                        Category category = findOrCreateCategory(em, categoryDTO);
                        product.addCategory(category);
                    }
                }

                product.setStore(store);
                if (!existingProducts.contains(product)) {
                    em.persist(product);
                }
                store.getProducts().add(product);
            }

            store.setHasProductsInDb(true);
            store.setLastFetched(LocalDateTime.now());

            em.merge(store);
            em.getTransaction().commit();
        }
    }

    private Category findOrCreateCategory(EntityManager em, CategoryDTO dto) {
        TypedQuery<Category> query = em.createQuery(
            "SELECT c FROM Category c WHERE c.nameDa = :nameDa AND c.nameEn = :nameEn",
            Category.class);
        query.setParameter("nameDa", dto.getNameDa());
        query.setParameter("nameEn", dto.getNameEn());

        List<Category> existingCategories = query.getResultList();
        if (!existingCategories.isEmpty()) {
            return existingCategories.get(0);
        }

        Category newCategory = new Category(dto);
        em.persist(newCategory);
        return newCategory;
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
            return query.getResultStream().findFirst().orElse(null);
        }
    }
}