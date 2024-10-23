package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.config.HibernateConfig;
import dat.daos.impl.BrandDAO;
import dat.daos.impl.StoreDAO;
import dat.dtos.*;
import dat.entities.Brand;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class StoreFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreFetcher.class);
    private static final String API_KEY = "77fcfa33-0e12-4dc9-aac6-c5d7cc9be766";
    private static final String STORES_URL = "https://api.sallinggroup.com/v2/stores";
    private static final int PER_PAGE = 100;

    // Liste af brands vi vil hente
    private static final List<String> WANTED_BRANDS = List.of("netto", "bilka", "foetex");

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final StoreDAO storeDAO;
    private final BrandDAO brandDAO;

    private static StoreFetcher instance;

    private StoreFetcher() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
        this.brandDAO = BrandDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    public static StoreFetcher getInstance() {
        if (instance == null) {
            instance = new StoreFetcher();
        }
        return instance;
    }

    public List<StoreDTO> fetchAndSaveAllStores() throws ApiException {
        List<StoreDTO> allStores = new ArrayList<>();

        try {
            LOGGER.info("Starting to fetch stores from Salling API");

            // Hent butikker for hvert ønsket brand
            for (String brand : WANTED_BRANDS) {
                LOGGER.info("Fetching stores for brand: {}", brand);
                allStores.addAll(fetchStoresForBrand(brand));
            }

            // Gem alle butikker i databasen
            LOGGER.info("Saving {} stores to database", allStores.size());
            storeDAO.saveOrUpdateStores(allStores);
            LOGGER.info("Successfully saved all stores to database");

            return allStores;

        } catch (Exception e) {
            LOGGER.error("Error during store fetch and save operation", e);
            throw new ApiException(500, "Failed to fetch and save stores: " + e.getMessage());
        }
    }

    private List<StoreDTO> fetchStoresForBrand(String brand) throws Exception {
        List<StoreDTO> brandStores = new ArrayList<>();
        int page = 1;

        while (true) {
            // Byg URL med brand og pagination parametre
            String urlWithParams = String.format("%s?brand=%s&per_page=%d&page=%d",
                STORES_URL, brand, PER_PAGE, page);

            LOGGER.debug("Fetching from URL: {}", urlWithParams);

            // Create and execute request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlWithParams))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != 200) {
                LOGGER.error("Failed to fetch stores. Status: {} Body: {}",
                    response.statusCode(), response.body());
                throw new ApiException(response.statusCode(),
                    "Failed to fetch stores from Salling API: " + response.body());
            }

            // Parse response
            JsonNode rootNode = objectMapper.readTree(response.body());

            // Check if we have any results
            if (!rootNode.isArray() || rootNode.size() == 0) {
                LOGGER.info("No more stores to fetch for brand: {}", brand);
                break;
            }

            // Process stores from current page
            for (JsonNode storeNode : rootNode) {
                try {
                    StoreDTO store = parseStoreNode(storeNode);
                    if (store != null) {
                        brandStores.add(store);
                        LOGGER.debug("Successfully parsed store: {}", store.getName());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse store: {}", e.getMessage());
                }
            }

            // Check if we should continue to next page
            if (rootNode.size() < PER_PAGE) {
                LOGGER.info("Reached last page for brand: {}", brand);
                break;
            }

            page++;
        }

        LOGGER.info("Fetched {} stores for brand: {}", brandStores.size(), brand);
        return brandStores;
    }

    private StoreDTO parseStoreNode(JsonNode storeNode) throws ApiException {
        try {
            // Parse address
            JsonNode addressNode = storeNode.get("address");
            if (addressNode == null || addressNode.isNull()) {
                throw new ApiException(400, "Store has no address information");
            }

            String storeId = storeNode.get("id").asText();
            String name = storeNode.get("name").asText();
            String street = addressNode.get("street").asText();
            String zipStr = addressNode.get("zip").asText();
            String city = addressNode.get("city").asText();

            // Parse zip code
            int zipCode;
            try {
                zipCode = Integer.parseInt(zipStr);
            } catch (NumberFormatException e) {
                throw new ApiException(400, "Invalid zip code format: " + zipStr);
            }

            // Parse coordinates if available
            Double longitude = null;
            Double latitude = null;
            JsonNode coordinatesNode = storeNode.get("coordinates");
            if (coordinatesNode != null && !coordinatesNode.isNull() && coordinatesNode.isArray()) {
                longitude = coordinatesNode.get(0).asDouble();
                latitude = coordinatesNode.get(1).asDouble();
            }

            // Create DTOs
            PostalCodeDTO postalCodeDTO = PostalCodeDTO.builder()
                .postalCode(zipCode)
                .city(city)
                .build();

            AddressDTO addressDTO = AddressDTO.builder()
                .addressLine(street)
                .postalCode(postalCodeDTO)
                .longitude(longitude)
                .latitude(latitude)
                .build();

            // Handle brand
            String brandName = storeNode.get("brand").asText().trim().toUpperCase();
            String displayName = storeNode.get("brand").asText().trim();

            // Check if 'oe' should be replaced with 'ø'
            if (displayName.toLowerCase().contains("oe")) {
                displayName = displayName.replaceAll("(?i)oe", "ø"); // Replaces both 'oe' and 'OE' with 'ø'
            }

            // Ensure the first letter is capitalized and the rest is lowercase
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1).toLowerCase();

            // Find or create the brand
            Brand brand = brandDAO.findOrCreateBrand(brandName, displayName);
            BrandDTO brandDTO = new BrandDTO(brand);

            return StoreDTO.builder()
                .sallingStoreId(storeId)
                .name(name)
                .brand(brandDTO)
                .address(addressDTO)
                .hasProductsInDb(false)
                .build();

        } catch (Exception e) {
            LOGGER.error("Error parsing store node: {}", e.getMessage());
            throw new ApiException(500, "Error parsing store data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            StoreFetcher fetcher = StoreFetcher.getInstance();
            List<StoreDTO> stores = fetcher.fetchAndSaveAllStores();

            System.out.println("\nFetch Summary:");
            System.out.println("Total stores fetched: " + stores.size());

            System.out.println("\nStores by brand:");
            stores.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    store -> store.getBrand().getDisplayName(),
                    java.util.stream.Collectors.counting()))
                .forEach((brand, count) ->
                    System.out.printf("%s: %d stores%n", brand, count));

        } catch (ApiException e) {
            System.err.println("Error during store fetch: " + e.getMessage());
        }
    }
}