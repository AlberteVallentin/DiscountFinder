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

public class StoreFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreFetcher.class);
    private static final String API_KEY = "77fcfa33-0e12-4dc9-aac6-c5d7cc9be766";
    private static final String STORES_URL = "https://api.sallinggroup.com/v2/stores";
    private static final int MAX_SIZE = 1000; // Set a high value to get all stores

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final StoreDAO storeDAO;

    public StoreFetcher() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.storeDAO = StoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    public List<StoreDTO> fetchAndSaveAllStores() throws ApiException {
        List<StoreDTO> allStores = new ArrayList<>();
        int page = 1; // Start med første side
        int storesFetched = 0;
        int perPage = 100; // Juster dette til, hvor mange du vil hente per side (API'et tillader ofte 100)

        try {
            LOGGER.info("Starting to fetch stores from Salling API");

            while (true) {
                // Build URL with page and per_page parameters
                String urlWithParams = STORES_URL + "?brand=foetex&country=dk&per_page=" + perPage + "&page=" + page;
                LOGGER.info("Fetching stores with URL: {}", urlWithParams);

                // Create request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlWithParams))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

                // Send request and get response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Check response status
                if (response.statusCode() != 200) {
                    LOGGER.error("Failed to fetch stores. Status: {} Body: {}", response.statusCode(), response.body());
                    throw new ApiException(response.statusCode(), "Failed to fetch stores from Salling API: " + response.body());
                }

                // Parse response body
                JsonNode rootNode = objectMapper.readTree(response.body());

                if (!rootNode.isArray() || rootNode.size() == 0) {
                    LOGGER.info("No more stores found, ending pagination");
                    break; // Slut hvis vi ikke modtager flere resultater
                }

                // Parse stores and add to the list
                List<StoreDTO> stores = new ArrayList<>();
                for (JsonNode storeNode : rootNode) {
                    try {
                        StoreDTO store = parseStoreNode(storeNode);
                        if (store != null) {
                            stores.add(store);
                            LOGGER.debug("Parsed store: {}", store.getName());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error parsing store: {}", e.getMessage());
                    }
                }

                allStores.addAll(stores);
                storesFetched += stores.size();
                LOGGER.info("Fetched {} stores from API (Total: {})", stores.size(), storesFetched);

                // Increment page for the next batch of results
                page++;

                // If fewer results than perPage, we are on the last page
                if (stores.size() < perPage) {
                    break; // Vi er på sidste side, hvis vi får færre resultater end perPage
                }
            }

            // Save all stores to the database
            storeDAO.saveOrUpdateStores(allStores);
            LOGGER.info("Saved {} stores to database", allStores.size());

            return allStores;

        } catch (Exception e) {
            LOGGER.error("Error during store fetch and save", e);
            throw new ApiException(500, "Failed to fetch and save stores: " + e.getMessage());
        }
    }



    private StoreDTO parseStoreNode(JsonNode storeNode) throws ApiException {
        try {
            // Parse address
            JsonNode addressNode = storeNode.get("address");
            if (addressNode == null || addressNode.isNull()) {
                throw new ApiException(400, "Store has no address information");
            }

            String street = addressNode.get("street").asText();
            String zipStr = addressNode.get("zip").asText();
            String city = addressNode.get("city").asText();

            int zipCode;
            try {
                zipCode = Integer.parseInt(zipStr);
            } catch (NumberFormatException e) {
                throw new ApiException(400, "Invalid zip code format: " + zipStr);
            }

            // Parse coordinates
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

            String brandStr = storeNode.get("brand").asText().toUpperCase();
            Brand brand;
            switch (brandStr) {
                case "NETTO" -> brand = Brand.NETTO;
                case "FOTEX", "FOETEX", "FØTEX" -> brand = Brand.FOETEX;
                case "BILKA" -> brand = Brand.BILKA;
                default -> {
                    LOGGER.warn("Unknown brand: {}. Skipping this store.", brandStr);
                    return null;
                }
            }


            // Create and return StoreDTO
            return StoreDTO.builder()
                .sallingStoreId(storeNode.get("id").asText())
                .name(storeNode.get("name").asText())
                .brand(brand)
                .address(addressDTO)
                .hasProductsInDb(false)
                .build();

        } catch (Exception e) {
            LOGGER.error("Error parsing store node", e);
            throw new ApiException(500, "Error parsing store data: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        try {
            StoreFetcher fetcher = new StoreFetcher();
            List<StoreDTO> stores = fetcher.fetchAndSaveAllStores();
            System.out.println("Successfully fetched and saved " + stores.size() + " stores");

            // Print summary of stores by brand
            System.out.println("\nStores by brand:");
            stores.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    StoreDTO::getBrand,
                    java.util.stream.Collectors.counting()))
                .forEach((brand, count) ->
                    System.out.printf("%s: %d stores%n", brand, count));

        } catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}