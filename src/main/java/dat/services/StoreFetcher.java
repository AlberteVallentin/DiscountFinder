package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.AddressDTO;
import dat.dtos.PostalCodeDTO;
import dat.dtos.StoreDTO;
import dat.enums.Brand;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class StoreFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreFetcher.class);
    private static final String API_KEY = "77fcfa33-0e12-4dc9-aac6-c5d7cc9be766";
    private static final String STORES_URL = "https://api.sallinggroup.com/v2/stores";
    private static final int PER_PAGE = 100; // Number of stores to fetch per request

    // Brand name mapping for various possible store brand names
    private static final Map<String, Brand> BRAND_MAPPING = new HashMap<>();
    static {
        // Netto variations
        BRAND_MAPPING.put("NETTO", Brand.NETTO);
        BRAND_MAPPING.put("DØGNNETTO", Brand.NETTO);
        BRAND_MAPPING.put("DOGNNETTO", Brand.NETTO);

        // Føtex variations
        BRAND_MAPPING.put("FØTEX", Brand.FOETEX);
        BRAND_MAPPING.put("FOTEX", Brand.FOETEX);
        BRAND_MAPPING.put("FOETEX", Brand.FOETEX);
        BRAND_MAPPING.put("FØTEX FOOD", Brand.FOETEX);
        BRAND_MAPPING.put("FOTEX FOOD", Brand.FOETEX);

        // Bilka variations
        BRAND_MAPPING.put("BILKA", Brand.BILKA);
        BRAND_MAPPING.put("BILKA TOGO", Brand.BILKA);
        BRAND_MAPPING.put("BILKA TO GO", Brand.BILKA);
    }

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final StoreDAO storeDAO;

    public StoreFetcher() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    /**
     * Fetches all stores from the Salling API and saves them to the database
     * @return List of fetched store DTOs
     * @throws ApiException if there's an error fetching or saving stores
     */
    public List<StoreDTO> fetchAndSaveAllStores() throws ApiException {
        List<StoreDTO> allStores = new ArrayList<>();
        int page = 1;
        int totalStoresFetched = 0;

        try {
            LOGGER.info("Starting to fetch stores from Salling API");

            while (true) {
                // Build URL with pagination parameters
                String urlWithParams = String.format("%s?per_page=%d&page=%d", STORES_URL, PER_PAGE, page);
                LOGGER.info("Fetching stores from URL: {}", urlWithParams);

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
                    LOGGER.error("Failed to fetch stores. Status: {} Body: {}", response.statusCode(), response.body());
                    throw new ApiException(response.statusCode(), "Failed to fetch stores from Salling API: " + response.body());
                }

                // Parse response
                JsonNode rootNode = objectMapper.readTree(response.body());

                // Check if we have any results
                if (!rootNode.isArray() || rootNode.size() == 0) {
                    LOGGER.info("No more stores to fetch. Total stores fetched: {}", totalStoresFetched);
                    break;
                }

                // Process stores from current page
                List<StoreDTO> pageStores = new ArrayList<>();
                for (JsonNode storeNode : rootNode) {
                    try {
                        StoreDTO store = parseStoreNode(storeNode);
                        if (store != null) {
                            pageStores.add(store);
                            LOGGER.debug("Successfully parsed store: {}", store.getName());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse store: {}", e.getMessage());
                    }
                }

                // Add stores from this page to total
                allStores.addAll(pageStores);
                totalStoresFetched += pageStores.size();
                LOGGER.info("Fetched {} stores from page {} (Total: {})", pageStores.size(), page, totalStoresFetched);

                // Check if we should continue to next page
                if (pageStores.size() < PER_PAGE) {
                    LOGGER.info("Reached last page of results");
                    break;
                }

                page++;
            }

            // Save all stores to database
            LOGGER.info("Saving {} stores to database", allStores.size());
            storeDAO.saveOrUpdateStores(allStores);
            LOGGER.info("Successfully saved all stores to database");

            return allStores;

        } catch (Exception e) {
            LOGGER.error("Error during store fetch and save operation", e);
            throw new ApiException(500, "Failed to fetch and save stores: " + e.getMessage());
        }
    }

    /**
     * Maps a brand string to the corresponding Brand enum value.
     * @param brandStr The brand string from the API
     * @return The corresponding Brand enum value
     * @throws ApiException if the brand cannot be mapped
     */
    private Brand mapBrandString(String brandStr) throws ApiException {
        String normalizedBrand = brandStr.trim().toUpperCase();
        Brand mappedBrand = BRAND_MAPPING.get(normalizedBrand);

        if (mappedBrand != null) {
            return mappedBrand;
        }

        LOGGER.warn("Unknown brand encountered: {}. This brand will be skipped.", brandStr);
        throw new ApiException(400, "Unsupported brand: " + brandStr);
    }

    /**
     * Parses a JSON node containing store information into a StoreDTO
     * @param storeNode JSON node containing store data
     * @return StoreDTO object, or null if the store should be skipped
     * @throws ApiException if there's an error parsing the store data
     */
    private StoreDTO parseStoreNode(JsonNode storeNode) throws ApiException {
        try {
            // Parse address
            JsonNode addressNode = storeNode.get("address");
            if (addressNode == null || addressNode.isNull()) {
                throw new ApiException(400, "Store has no address information");
            }

            // Extract and validate required fields
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

            // Try to map the brand
            Brand brand;
            try {
                brand = mapBrandString(storeNode.get("brand").asText());
            } catch (ApiException e) {
                LOGGER.info("Skipping store '{}' due to unsupported brand", name);
                return null;
            }

            // Create and return complete StoreDTO
            return StoreDTO.builder()
                .sallingStoreId(storeId)
                .name(name)
                .brand(brand)
                .address(addressDTO)
                .hasProductsInDb(false)
                .build();

        } catch (Exception e) {
            LOGGER.error("Error parsing store node: {}", e.getMessage());
            throw new ApiException(500, "Error parsing store data: " + e.getMessage());
        }
    }

    /**
     * Main method for testing the StoreFetcher
     */
    public static void main(String[] args) {
        try {
            StoreFetcher fetcher = new StoreFetcher();
            List<StoreDTO> stores = fetcher.fetchAndSaveAllStores();

            // Print summary of results
            System.out.println("\nFetch Summary:");
            System.out.println("Total stores fetched: " + stores.size());

            // Group and count stores by brand
            System.out.println("\nStores by brand:");
            stores.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    StoreDTO::getBrand,
                    java.util.stream.Collectors.counting()))
                .forEach((brand, count) ->
                    System.out.printf("%s: %d stores%n", brand, count));

        } catch (ApiException e) {
            System.err.println("Error during store fetch: " + e.getMessage());
        }
    }
}