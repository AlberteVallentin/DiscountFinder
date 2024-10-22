package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.StoreDTO;
import dat.entities.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

public class StoreDAO implements IDAO<StoreDTO, Long> {
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
            Store store = new Store(storeDTO);
            em.persist(store);
            em.getTransaction().commit();
            return new StoreDTO(store);
        }
    }

    @Override
    public StoreDTO update(Long id, StoreDTO storeDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Store store = em.find(Store.class, id);
            if (store != null) {
                store.updateFromSallingApi(storeDTO);
                em.merge(store);
            }
            em.getTransaction().commit();
            return store != null ? new StoreDTO(store) : null;
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

    // Additional helper methods
    public Store findBySallingId(String sallingStoreId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s WHERE s.sallingStoreId = :sallingId", Store.class);
            query.setParameter("sallingId", sallingStoreId);
            List<Store> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public List<StoreDTO> findByPostalCode(int postalCode) {
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

    public List<StoreDTO> findStoresWithProducts() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s WHERE s.hasProductsInDb = true",
                Store.class);
            return query.getResultList().stream()
                .map(StoreDTO::new)
                .collect(Collectors.toList());
        }
    }
}