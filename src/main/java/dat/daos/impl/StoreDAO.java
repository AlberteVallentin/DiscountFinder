package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.StoreDTO;
import dat.entities.Brand;
import dat.entities.Store;
import dat.entities.Address;
import dat.entities.PostalCode;
import dat.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
    public StoreDTO create(StoreDTO storeDTO) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Find eksisterende brand direkte i EntityManager
            Brand brand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                .setParameter("name", storeDTO.getBrand().getName())
                .getSingleResult();

            // Find eller opret PostalCode (eksisterende kode)
            PostalCode postalCode = findOrCreatePostalCode(em,
                storeDTO.getAddress().getPostalCode().getPostalCode(),
                storeDTO.getAddress().getPostalCode().getCity());

            // Opret ny Address
            Address address = new Address(storeDTO.getAddress());
            address.setPostalCode(postalCode);

            // Opret ny Store og sæt relationer
            Store store = new Store(storeDTO);
            store.setBrand(brand);
            store.setAddress(address);

            em.persist(address);
            em.persist(store);

            em.getTransaction().commit();

            return new StoreDTO(store);
        } catch (NoResultException e) {
            throw new ApiException(400, "Brand not found: " + storeDTO.getBrand().getName());
        } catch (Exception e) {
            LOGGER.error("Error creating store", e);
            throw new ApiException(500, "Could not create store: " + e.getMessage());
        }
    }

    @Override
    public StoreDTO update(Long id, StoreDTO storeDTO) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Store existingStore = em.find(Store.class, id);
            if (existingStore == null) {
                throw new ApiException(404, "Store not found with id: " + id);
            }

            // Opdater PostalCode
            PostalCode postalCode = findOrCreatePostalCode(em, storeDTO.getAddress().getPostalCode().getPostalCode(),
                storeDTO.getAddress().getPostalCode().getCity());

            // Opdater eller opret Address
            Address address = existingStore.getAddress();
            if (address == null) {
                address = new Address(storeDTO.getAddress());
            } else {
                address.updateFromDTO(storeDTO.getAddress());
            }
            address.setPostalCode(postalCode);

            // Opdater Store
            existingStore.updateFromSallingApi(storeDTO);
            existingStore.setAddress(address);

            em.merge(existingStore);
            em.getTransaction().commit();

            return new StoreDTO(existingStore);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error updating store", e);
            throw new ApiException(500, "Could not update store: " + e.getMessage());
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

    // Hjælpemetoder
    private PostalCode findOrCreatePostalCode(EntityManager em, int postalCodeNumber, String city) {
        PostalCode postalCode = em.find(PostalCode.class, postalCodeNumber);
        if (postalCode == null) {
            postalCode = new PostalCode();
            postalCode.setPostalCode(postalCodeNumber);
            postalCode.setCity(city);
            em.persist(postalCode);
        } else if (!postalCode.getCity().equals(city)) {
            postalCode.setCity(city);
        }
        return postalCode;
    }

    // Find en butik baseret på Salling Store ID
    public Store findBySallingId(String sallingStoreId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Store> query = em.createQuery(
                "SELECT s FROM Store s WHERE s.sallingStoreId = :sallingId", Store.class);
            query.setParameter("sallingId", sallingStoreId);
            List<Store> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    // Gem eller opdater en liste af butikker fra Salling API
    public void saveOrUpdateStores(List<StoreDTO> stores) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            for (StoreDTO storeDTO : stores) {
                try {
                    Store existingStore = findBySallingId(storeDTO.getSallingStoreId());
                    if (existingStore == null) {
                        // Opret ny butik
                        PostalCode postalCode = findOrCreatePostalCode(em,
                            storeDTO.getAddress().getPostalCode().getPostalCode(),
                            storeDTO.getAddress().getPostalCode().getCity());

                        Address address = new Address(storeDTO.getAddress());
                        address.setPostalCode(postalCode);

                        Store store = new Store(storeDTO);
                        store.setAddress(address);

                        em.persist(store);
                        LOGGER.info("Created new store: {}", store.getName());
                    } else {
                        // Opdater eksisterende butik
                        existingStore.updateFromSallingApi(storeDTO);
                        Address address = existingStore.getAddress();
                        address.updateFromDTO(storeDTO.getAddress());

                        em.merge(existingStore);
                        LOGGER.info("Updated existing store: {}", existingStore.getName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing store: {}", storeDTO.getName(), e);
                }
            }

            em.getTransaction().commit();
            LOGGER.info("Successfully processed {} stores", stores.size());
        } catch (Exception e) {
            LOGGER.error("Error saving/updating stores", e);
            throw new ApiException(500, "Could not save/update stores: " + e.getMessage());
        }
    }


}