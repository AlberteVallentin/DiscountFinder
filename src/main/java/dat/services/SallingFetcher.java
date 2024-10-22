package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.dtos.*;
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
import java.util.concurrent.CompletableFuture;

public class SallingFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SallingFetcher.class);
    private static final String API_KEY = System.getenv("SALLING_API_KEY");
    private static final String STORES_URL = "https://api.sallinggroup.com/v2/stores";
    private static final String FOOD_WASTE_URL = "https://api.sallinggroup.com/v1/food-waste";

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static SallingFetcher instance;

    private SallingFetcher() throws ApiException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            LOGGER.error("SALLING_API_KEY environment variable not set");
            throw new ApiException(500, "SALLING_API_KEY environment variable not set");
        }
    }

    public static SallingFetcher getInstance() throws ApiException {
        if (instance == null) {
            instance = new SallingFetcher();
        }
        return instance;
    }

    public List<StoreDTO> fetchAllStoresSync() throws ApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STORES_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.error("Failed to fetch stores. Status: {}. Body: {}", response.statusCode(), response.body());
                throw new ApiException(response.statusCode(), "Failed to fetch stores: " + response.body());
            }

            return parseStoresResponse(response.body());
        } catch (Exception e) {
            LOGGER.error("Error fetching stores from Salling API", e);
            throw new ApiException(500, "Failed to fetch stores: " + e.getMessage());
        }
    }

    public List<ProductDTO> fetchStoreProductsSync(String storeId) throws ApiException {
        try {
            String url = FOOD_WASTE_URL + "/stores/" + storeId + "/products";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.error("Failed to fetch products. Status: {}. Body: {}", response.statusCode(), response.body());
                throw new ApiException(response.statusCode(), "Failed to fetch products: " + response.body());
            }

            return parseProductsResponse(response.body());
        } catch (Exception e) {
            LOGGER.error("Error fetching products for store: " + storeId, e);
            throw new ApiException(500, "Failed to fetch products: " + e.getMessage());
        }
    }

    private List<StoreDTO> parseStoresResponse(String json) throws ApiException {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<StoreDTO> stores = new ArrayList<>();

            for (JsonNode storeNode : root) {
                // Extract basic store info
                String sallingStoreId = storeNode.get("id").asText();
                String name = storeNode.get("name").asText();
                Brand brand = parseBrand(storeNode.get("brand").asText());

                // Extract address info
                JsonNode addressNode = storeNode.get("address");
                String street = addressNode.get("street").asText();
                String city = addressNode.get("city").asText();
                String zipCode = addressNode.get("zipCode").asText();
                Double latitude = addressNode.get("latitude").asDouble();
                Double longitude = addressNode.get("longitude").asDouble();

                // Create PostalCodeDTO
                PostalCodeDTO postalCodeDTO = PostalCodeDTO.builder()
                    .postalCode(Integer.parseInt(zipCode))
                    .city(city)
                    .build();

                // Create AddressDTO
                AddressDTO addressDTO = AddressDTO.builder()
                    .addressLine(street)
                    .postalCode(postalCodeDTO)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

                // Create StoreDTO
                StoreDTO storeDTO = StoreDTO.builder()
                    .sallingStoreId(sallingStoreId)
                    .name(name)
                    .brand(brand)
                    .address(addressDTO)
                    .hasProductsInDb(false)
                    .build();

                stores.add(storeDTO);
            }

            return stores;
        } catch (Exception e) {
            LOGGER.error("Error parsing stores response", e);
            throw new ApiException(500, "Failed to parse stores response: " + e.getMessage());
        }
    }

    private Brand parseBrand(String brandString) throws ApiException {
        return switch (brandString.toLowerCase()) {
            case "bilka" -> Brand.BILKA;
            case "føtex", "fotex" -> Brand.FOETEX;
            case "netto" -> Brand.NETTO;
            default -> throw new ApiException(400, "Unknown brand: " + brandString);
        };
    }

    private List<ProductDTO> parseProductsResponse(String json) throws ApiException {
        try {
            // TODO: Implement product parsing once we have the exact response format
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Error parsing products response", e);
            throw new ApiException(500, "Failed to parse products response: " + e.getMessage());
        }
    }
}