package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.BrandDTO;
import dat.entities.Brand;
import dat.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class BrandDAO implements IDAO<BrandDTO, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrandDAO.class);
    private static BrandDAO instance;
    private static EntityManagerFactory emf;

    private BrandDAO() {
    }

    public static BrandDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new BrandDAO();
        }
        return instance;
    }

    @Override
    public BrandDTO read(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Brand brand = em.find(Brand.class, id);
            return brand != null ? new BrandDTO(brand) : null;
        }
    }

    @Override
    public List<BrandDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Brand> query = em.createQuery("SELECT b FROM Brand b", Brand.class);
            return query.getResultList().stream()
                .map(BrandDTO::new)
                .collect(Collectors.toList());
        }
    }

    @Override
    public BrandDTO create(BrandDTO brandDTO) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Brand brand = new Brand(brandDTO);
            em.persist(brand);
            em.getTransaction().commit();
            return new BrandDTO(brand);
        } catch (Exception e) {
            throw new ApiException(500, "Could not create brand: " + e.getMessage());
        }
    }

    @Override
    public BrandDTO update(Long id, BrandDTO brandDTO) throws ApiException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Brand brand = em.find(Brand.class, id);
            if (brand == null) {
                throw new ApiException(404, "Brand not found with id: " + id);
            }
            brand.setName(brandDTO.getName());
            brand.setDisplayName(brandDTO.getDisplayName());
            em.merge(brand);
            em.getTransaction().commit();
            return new BrandDTO(brand);
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Brand brand = em.find(Brand.class, id);
            if (brand != null) {
                em.remove(brand);
            }
            em.getTransaction().commit();
        }
    }

    @Override
    public boolean validatePrimaryKey(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Brand.class, id) != null;
        }
    }

    // Helper method to find brand by name
    public Brand findByName(String name) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Brand> query = em.createQuery(
                "SELECT b FROM Brand b WHERE LOWER(b.name) = LOWER(:name)",
                Brand.class);
            query.setParameter("name", name);
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }
    }

    // Helper method to find or create brand
    public Brand findOrCreateBrand(String name, String displayName) {
        try (EntityManager em = emf.createEntityManager()) {
            Brand brand = findByName(name);
            if (brand == null) {
                brand = new Brand(name, displayName);
                em.getTransaction().begin();
                em.persist(brand);
                em.getTransaction().commit();
                LOGGER.info("Created new brand: {}", displayName);
            }
            return brand;
        }
    }
}