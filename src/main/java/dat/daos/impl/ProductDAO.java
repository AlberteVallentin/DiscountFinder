package dat.daos.impl;


import dat.daos.IDAO;
import dat.dtos.ProductDTO;
import dat.entities.Product;
import dat.entities.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

public class ProductDAO implements IDAO<ProductDTO, Long> {
    private static ProductDAO instance;
    private static EntityManagerFactory emf;

    private ProductDAO() {
    }

    public static ProductDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new ProductDAO();
        }
        return instance;
    }

    @Override
    public ProductDTO read(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Product product = em.find(Product.class, id);
            return product != null ? new ProductDTO(product) : null;
        }
    }

    @Override
    public List<ProductDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);
            return query.getResultList().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
        }
    }

    @Override
    public ProductDTO create(ProductDTO productDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product product = new Product(productDTO);
            em.persist(product);
            em.getTransaction().commit();
            return new ProductDTO(product);
        }
    }

    @Override
    public ProductDTO update(Long id, ProductDTO productDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product product = em.find(Product.class, id);
            if (product != null) {
                product.updateFromDTO(productDTO);
                em.merge(product);
            }
            em.getTransaction().commit();
            return product != null ? new ProductDTO(product) : null;
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product product = em.find(Product.class, id);
            if (product != null) {
                product.clearCategories(); // Remove all category associations
                em.remove(product);
            }
            em.getTransaction().commit();
        }
    }

    @Override
    public boolean validatePrimaryKey(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Product.class, id) != null;
        }
    }

    // Additional helper methods
    public List<ProductDTO> findByStore(Long storeId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Product> query = em.createQuery(
                "SELECT p FROM Product p WHERE p.store.id = :storeId", Product.class);
            query.setParameter("storeId", storeId);
            return query.getResultList().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
        }
    }
}
