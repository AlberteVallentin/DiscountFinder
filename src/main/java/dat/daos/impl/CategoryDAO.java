package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.CategoryDTO;
import dat.entities.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryDAO implements IDAO<CategoryDTO, Long> {
    private static CategoryDAO instance;
    private static EntityManagerFactory emf;

    private CategoryDAO() {
    }

    public static CategoryDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new CategoryDAO();
        }
        return instance;
    }

    @Override
    public CategoryDTO read(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Category category = em.find(Category.class, id);
            return category != null ? new CategoryDTO(category) : null;
        }
    }

    @Override
    public List<CategoryDTO> readAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Category> query = em.createQuery("SELECT c FROM Category c", Category.class);
            return query.getResultList().stream()
                .map(CategoryDTO::new)
                .collect(Collectors.toList());
        }
    }

    @Override
    public CategoryDTO create(CategoryDTO categoryDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Category category = new Category(categoryDTO);
            em.persist(category);
            em.getTransaction().commit();
            return new CategoryDTO(category);
        }
    }

    @Override
    public CategoryDTO update(Long id, CategoryDTO categoryDTO) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Category category = em.find(Category.class, id);
            if (category != null) {
                category.setNameDa(categoryDTO.getNameDa());
                category.setNameEn(categoryDTO.getNameEn());
                category.setPathDa(categoryDTO.getPathDa());
                category.setPathEn(categoryDTO.getPathEn());
                em.merge(category);
            }
            em.getTransaction().commit();
            return category != null ? new CategoryDTO(category) : null;
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Category category = em.find(Category.class, id);
            if (category != null) {
                // Remove the category from all products first
                category.getProducts().forEach(product -> product.getCategories().remove(category));
                em.remove(category);
            }
            em.getTransaction().commit();
        }
    }

    @Override
    public boolean validatePrimaryKey(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Category.class, id) != null;
        }
    }



    public Category findOrCreateByPath(String pathDa, String pathEn) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Category> query = em.createQuery(
                "SELECT c FROM Category c WHERE c.pathDa = :pathDa AND c.pathEn = :pathEn",
                Category.class);
            query.setParameter("pathDa", pathDa);
            query.setParameter("pathEn", pathEn);

            List<Category> results = query.getResultList();
            if (!results.isEmpty()) {
                return results.get(0);
            }

            // If not found, create new category
            em.getTransaction().begin();
            String[] pathPartsDa = pathDa.split(">");
            String[] pathPartsEn = pathEn.split(">");
            String nameDa = pathPartsDa[pathPartsDa.length - 1].trim();
            String nameEn = pathPartsEn[pathPartsEn.length - 1].trim();

            Category newCategory = new Category(nameDa, nameEn, pathDa, pathEn);
            em.persist(newCategory);
            em.getTransaction().commit();
            return newCategory;
        }
    }
}
