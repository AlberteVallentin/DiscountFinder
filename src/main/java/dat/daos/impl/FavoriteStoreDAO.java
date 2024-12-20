package dat.daos.impl;

import dat.security.entities.User;
import dat.entities.Store;
import dat.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FavoriteStoreDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteStoreDAO.class);
    private static FavoriteStoreDAO instance;
    private static EntityManagerFactory emf;

    private FavoriteStoreDAO() {
    }

    public static FavoriteStoreDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new FavoriteStoreDAO();
        }
        return instance;
    }

    public void addFavoriteStore(String userEmail, Long storeId) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", userEmail)
                .getSingleResult();

            Store store = em.find(Store.class, storeId);
            if (store == null) {
                throw new ApiException(404, "Store not found");
            }

            // Check if already favorited
            boolean alreadyFavorited = user.getFavoriteStores().stream()
                .anyMatch(s -> s.getId().equals(storeId));

            if (!alreadyFavorited) {
                user.getFavoriteStores().add(store);
                em.merge(user);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            throw new ApiException(500, "Error adding favorite store: " + e.getMessage());
        }
    }

    public void removeFavoriteStore(String userEmail, Long storeId) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            User user = em.createQuery("SELECT u FROM User u JOIN FETCH u.favoriteStores WHERE u.email = :email", User.class)
                .setParameter("email", userEmail)
                .getSingleResult();

            Store store = em.find(Store.class, storeId);
            if (store == null) {
                throw new ApiException(404, "Store not found");
            }

            boolean removed = user.getFavoriteStores().removeIf(s -> s.getId().equals(storeId));
            if (removed) {
                em.merge(user);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            throw new ApiException(500, "Error removing favorite store: " + e.getMessage());
        }
    }

    public List<Store> getFavoriteStores(String userEmail) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                    "SELECT s FROM User u JOIN u.favoriteStores s WHERE u.email = :email", Store.class)
                .setParameter("email", userEmail)
                .getResultList();
        } catch (Exception e) {
            throw new ApiException(500, "Error fetching favorite stores: " + e.getMessage());
        }
    }
}