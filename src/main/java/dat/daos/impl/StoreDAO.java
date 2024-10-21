package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.StoreDTO;
import dat.entities.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class StoreDAO implements IDAO<StoreDTO, Long> {

    private static StoreDAO instance;
    private static EntityManagerFactory emf;

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
            TypedQuery<StoreDTO> query = em.createQuery("SELECT new dat.dtos.StoreDTO(s) FROM Store s", StoreDTO.class);
            return query.getResultList();
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
                store.setStoreName(storeDTO.getStoreName());
                store.setAddress(storeDTO.getAddress());
                store.setBrand(storeDTO.getBrand());
                store.setStoreManager(storeDTO.getStoreManager());
                store.setHasProductsInDb(storeDTO.isHasProductsInDb());
                Store mergedStore = em.merge(store);
                em.getTransaction().commit();
                return new StoreDTO(mergedStore);
            }
            em.getTransaction().rollback();
            return null;
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
            Store store = em.find(Store.class, id);
            return store != null;
        }
    }
}
