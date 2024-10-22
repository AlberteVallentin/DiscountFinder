package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.dtos.*;
import dat.enums.Brand;
import dat.enums.StockUnit;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

public class SallingFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SallingFetcher.class);

    // Hardcoded API key - HUSK at ændre dette til environment variable i production!
    private static final String API_KEY = "77fcfa33-0e12-4dc9-aac6-c5d7cc9be766";

    // API endpoints
    private static final String STORES_BASE_URL = "https://api.sallinggroup.com/v2";
    private static final String FOOD_WASTE_BASE_URL = "https://api.sallinggroup.com/v1/food-waste";
    private static final String STORES_URL = STORES_BASE_URL + "/stores";

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static SallingFetcher instance;

    private SallingFetcher() {
        LOGGER.info("Initializing SallingFetcher with API key length: {}", API_KEY.length());
    }

    public static SallingFetcher getInstance() {
        if (instance == null) {
            instance = new SallingFetcher();
        }
        return instance;
    }

    public List<StoreDTO> fetchAllStoresSync() throws ApiException {
        try {
            LOGGER.info("Fetching stores from URL: {}", STORES_URL);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STORES_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

            LOGGER.debug("Sending request with headers: {}", request.headers());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.debug("Received response with status code: {}", response.statusCode());
            LOGGER.debug("Response body: {}", response.body());

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
            String url = FOOD_WASTE_BASE_URL + "/stores/" + storeId + "/clearances";
            LOGGER.info("Fetching products from URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

            LOGGER.debug("Sending request with headers: {}", request.headers());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.debug("Received response with status code: {}", response.statusCode());
            LOGGER.debug("Response body: {}", response.body());

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

            if (!root.isArray()) {
                LOGGER.debug("Root node type: {}", root.getNodeType());
                throw new ApiException(500, "Expected JSON array but got: " + root.getNodeType());
            }

            for (JsonNode storeNode : root) {
                try {
                    String sallingStoreId = storeNode.get("id").asText();
                    String name = storeNode.get("name").asText();
                    String brandStr = storeNode.get("brand").asText();
                    Brand brand = parseBrand(brandStr);

                    // Get address
                    JsonNode addressNode = storeNode.get("address");
                    if (addressNode == null) {
                        LOGGER.warn("Store {} has no address information", sallingStoreId);
                        continue;
                    }

                    String street = addressNode.get("street").asText();
                    String city = addressNode.get("city").asText();
                    String zipCode = addressNode.get("zipCode").asText();

                    // Parse coordinates
                    Double latitude = null;
                    Double longitude = null;
                    if (storeNode.has("latitude") && storeNode.has("longitude")) {
                        latitude = storeNode.get("latitude").asDouble();
                        longitude = storeNode.get("longitude").asDouble();
                    }

                    // Create DTOs
                    PostalCodeDTO postalCodeDTO = PostalCodeDTO.builder()
                        .postalCode(Integer.parseInt(zipCode))
                        .city(city)
                        .build();

                    AddressDTO addressDTO = AddressDTO.builder()
                        .addressLine(street)
                        .postalCode(postalCodeDTO)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();

                    StoreDTO storeDTO = StoreDTO.builder()
                        .sallingStoreId(sallingStoreId)
                        .name(name)
                        .brand(brand)
                        .address(addressDTO)
                        .hasProductsInDb(false)
                        .build();

                    stores.add(storeDTO);
                    LOGGER.debug("Successfully parsed store: {}", name);
                } catch (Exception e) {
                    LOGGER.error("Error processing store node: {}", storeNode, e);
                }
            }

            LOGGER.info("Successfully parsed {} stores", stores.size());
            return stores;
        } catch (Exception e) {
            LOGGER.error("Error parsing stores response", e);
            throw new ApiException(500, "Failed to parse stores response: " + e.getMessage());
        }
    }

    private List<ProductDTO> parseProductsResponse(String json) throws ApiException {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ProductDTO> products = new ArrayList<>();

            if (!root.isArray()) {
                LOGGER.debug("Root node type: {}", root.getNodeType());
                // Hvis root ikke er et array, tjek om det er et objekt med et "clearances" array
                if (root.has("clearances")) {
                    root = root.get("clearances");
                } else {
                    throw new ApiException(500, "Unexpected JSON format: neither root array nor clearances object found");
                }
            }

            for (JsonNode productNode : root) {
                try {
                    // Basis produkt information
                    String ean = productNode.get("ean").asText();
                    String productName = productNode.get("description").asText();

                    // Pris information
                    PriceDTO priceDTO = null;
                    if (productNode.has("offer")) {
                        JsonNode offerNode = productNode.get("offer");
                        priceDTO = PriceDTO.builder()
                            .originalPrice(BigDecimal.valueOf(offerNode.get("originalPrice").asDouble()))
                            .newPrice(BigDecimal.valueOf(offerNode.get("newPrice").asDouble()))
                            .discount(BigDecimal.valueOf(offerNode.get("discount").asDouble()))
                            .percentDiscount(BigDecimal.valueOf(offerNode.get("percentDiscount").asDouble()))
                            .build();
                    }

                    // Timing information
                    TimingDTO timingDTO = null;
                    if (productNode.has("offer")) {
                        JsonNode offerNode = productNode.get("offer");
                        timingDTO = TimingDTO.builder()
                            .startTime(LocalDateTime.parse(offerNode.get("startTime").asText()))
                            .endTime(LocalDateTime.parse(offerNode.get("endTime").asText()))
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    }

                    // Stock information
                    StockDTO stockDTO = null;
                    if (productNode.has("stock")) {
                        JsonNode stockNode = productNode.get("stock");
                        stockDTO = StockDTO.builder()
                            .quantity(stockNode.get("quantity").asDouble())
                            .stockUnit(parseStockUnit(stockNode.get("unit").asText()))
                            .build();
                    }

                    // Kategori information
                    Set<CategoryDTO> categories = new HashSet<>();
                    if (productNode.has("categories") && productNode.get("categories").isArray()) {
                        for (JsonNode categoryNode : productNode.get("categories")) {
                            String pathDa = categoryNode.get("da").asText();
                            String pathEn = categoryNode.get("en").asText();

                            String[] pathPartsDa = pathDa.split("/");
                            String[] pathPartsEn = pathEn.split("/");
                            String nameDa = pathPartsDa[pathPartsDa.length - 1];
                            String nameEn = pathPartsEn[pathPartsEn.length - 1];

                            categories.add(CategoryDTO.fromSallingCategory(nameDa, nameEn, pathDa, pathEn));
                        }
                    }

                    // Byg det komplette ProductDTO
                    ProductDTO productDTO = ProductDTO.builder()
                        .ean(ean)
                        .productName(productName)
                        .price(priceDTO)
                        .timing(timingDTO)
                        .stock(stockDTO)
                        .categories(categories)
                        .build();

                    products.add(productDTO);
                    LOGGER.debug("Successfully parsed product: {}", productName);
                } catch (Exception e) {
                    LOGGER.error("Error processing product node: {}", productNode, e);
                }
            }

            LOGGER.info("Successfully parsed {} products", products.size());
            return products;
        } catch (Exception e) {
            LOGGER.error("Error parsing products response", e);
            throw new ApiException(500, "Failed to parse products response: " + e.getMessage());
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

    private StockUnit parseStockUnit(String unit) {
        return unit.equalsIgnoreCase("each") ? StockUnit.EACH : StockUnit.KG;
    }
}