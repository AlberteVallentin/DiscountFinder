package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.*;
import dat.entities.Brand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreFetcher.class);
    private static final String SALLING_API_KEY = System.getenv("SALLING_API_KEY");
    private static final String STORES_URL = "https://api.sallinggroup.com/v2/stores";
    private static final int PER_PAGE = 100;
    private static final List<String> WANTED_BRANDS = List.of("netto", "bilka", "foetex");

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final StoreDAO storeDAO;
    private final EntityManagerFactory emf;
    private final Map<String, Brand> brandCache = new HashMap<>();

    private static StoreFetcher instance;

    private StoreFetcher() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.emf = HibernateConfig.getEntityManagerFactory();
        this.storeDAO = StoreDAO.getInstance(emf);
    }

    public static StoreFetcher getInstance() {
        if (instance == null) {
            instance = new StoreFetcher();
        }
        return instance;
    }

    public List<StoreDTO> fetchAndSaveAllStores() {
        List<StoreDTO> allStores = new ArrayList<>();

        try {
            LOGGER.info("Starting to fetch stores from Salling API");

            // Først sikrer vi at alle brands eksisterer i databasen
            initializeBrands();

            for (String brand : WANTED_BRANDS) {
                LOGGER.info("Fetching stores for brand: {}", brand);
                allStores.addAll(fetchStoresForBrand(brand));
            }

            LOGGER.info("Saving {} stores to database", allStores.size());
            storeDAO.saveOrUpdateStores(allStores);
            return allStores;

        } catch (Exception e) {
            LOGGER.error("Error during store fetch and save operation", e);
            throw new RuntimeException("Failed to fetch and save stores: " + e.getMessage(), e);
        }
    }

    private void initializeBrands() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            for (String brandName : WANTED_BRANDS) {
                String upperBrandName = brandName.toUpperCase();
                String displayName = formatDisplayName(brandName);

                Brand brand = em.createQuery("SELECT b FROM Brand b WHERE b.name = :name", Brand.class)
                    .setParameter("name", upperBrandName)
                    .getResultStream()
                    .findFirst()
                    .orElseGet(() -> {
                        Brand newBrand = new Brand();
                        newBrand.setName(upperBrandName);
                        newBrand.setDisplayName(displayName);
                        em.persist(newBrand);
                        return newBrand;
                    });

                brandCache.put(upperBrandName, brand);
            }

            em.getTransaction().commit();
        }
    }

    private String formatDisplayName(String brandName) {
        if (brandName.equalsIgnoreCase("foetex")) {
            return "Føtex";
        }
        return brandName.substring(0, 1).toUpperCase() + brandName.substring(1).toLowerCase();
    }

    private List<StoreDTO> fetchStoresForBrand(String brand) throws Exception {
        List<StoreDTO> brandStores = new ArrayList<>();
        int page = 1;

        while (true) {
            String urlWithParams = String.format("%s?brand=%s&country=dk&per_page=%d&page=%d",
                STORES_URL, brand, PER_PAGE, page);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlWithParams))
                .header("Authorization", "Bearer " + SALLING_API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.error("Failed to fetch stores. Status: {} Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to fetch stores from Salling API");
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            if (!rootNode.isArray() || rootNode.size() == 0) {
                break;
            }

            for (JsonNode storeNode : rootNode) {
                try {
                    brandStores.add(parseStoreNode(storeNode, brand));
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse store: {}", e.getMessage());
                }
            }

            if (rootNode.size() < PER_PAGE) {
                break;
            }
            page++;
        }

        LOGGER.info("Fetched {} stores for brand: {}", brandStores.size(), brand);
        return brandStores;
    }

    private StoreDTO parseStoreNode(JsonNode storeNode, String brandName) {
        JsonNode addressNode = storeNode.get("address");
        if (addressNode == null) {
            throw new IllegalArgumentException("Store has no address information");
        }

        PostalCodeDTO postalCodeDTO = PostalCodeDTO.builder()
            .postalCode(Integer.parseInt(addressNode.get("zip").asText()))
            .build();

        AddressDTO addressDTO = AddressDTO.builder()
            .addressLine(addressNode.get("street").asText())
            .postalCode(postalCodeDTO)
            .build();

        if (storeNode.has("coordinates") && storeNode.get("coordinates").isArray()) {
            addressDTO.setLongitude(storeNode.get("coordinates").get(0).asDouble());
            addressDTO.setLatitude(storeNode.get("coordinates").get(1).asDouble());
        }

        Brand brand = brandCache.get(brandName.toUpperCase());
        if (brand == null) {
            throw new IllegalStateException("Brand not found in cache: " + brandName);
        }

        BrandDTO brandDTO = new BrandDTO(brand);

        return StoreDTO.builder()
            .sallingStoreId(storeNode.get("id").asText())
            .name(storeNode.get("name").asText())
            .brand(brandDTO)
            .address(addressDTO)
            .hasProductsInDb(false)
            .build();
    }
}