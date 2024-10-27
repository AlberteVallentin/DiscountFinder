package dat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.dtos.*;
import dat.enums.StockUnit;
import dat.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductFetcher.class);
    private static final String API_KEY = "77fcfa33-0e12-4dc9-aac6-c5d7cc9be766";
    private static final String FOODWASTE_URL = "https://api.sallinggroup.com/v1/food-waste/%s";

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private static ProductFetcher instance;

    private ProductFetcher() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public static ProductFetcher getInstance() {
        if (instance == null) {
            instance = new ProductFetcher();
        }
        return instance;
    }

    public List<ProductDTO> fetchProductsForStore(String storeId) throws ApiException {
        List<ProductDTO> products = new ArrayList<>();

        try {
            String url = String.format(FOODWASTE_URL, storeId);
            HttpRequest request = buildRequest(url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            validateResponse(response);
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode clearances = rootNode.get("clearances");

            if (clearances == null || !clearances.isArray()) {
                LOGGER.warn("No clearances found for store {}", storeId);
                return products;
            }

            for (JsonNode clearanceNode : clearances) {
                try {
                    ProductDTO product = parseProduct(clearanceNode);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse product in store {}: {}", storeId, e.getMessage());
                }
            }

            LOGGER.info("Successfully fetched {} products for store {}", products.size(), storeId);
            return products;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error fetching products for store {}: {}", storeId, e.getMessage());
            throw new ApiException(500, "Internal error while fetching products: " + e.getMessage());
        }
    }

    private HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Accept", "application/json")
            .GET()
            .build();
    }

    private void validateResponse(HttpResponse<String> response) throws ApiException {
        switch (response.statusCode()) {
            case 200:
                break;
            case 404:
                throw new ApiException(404, "Store not found");
            case 401:
                throw new ApiException(401, "Invalid API key");
            case 429:
                throw new ApiException(429, "Rate limit exceeded");
            default:
                LOGGER.error("Unexpected API response: {} - {}", response.statusCode(), response.body());
                throw new ApiException(response.statusCode(),
                    "Unexpected error from Salling API: " + response.body());
        }
    }

    private String getRequiredText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            throw new IllegalArgumentException("Required field missing: " + field);
        }
        return fieldNode.asText();
    }

    private PriceDTO parsePriceDTO(JsonNode offerNode) {
        return PriceDTO.builder()
            .originalPrice(new BigDecimal(getRequiredText(offerNode, "originalPrice")))
            .newPrice(new BigDecimal(getRequiredText(offerNode, "newPrice")))
            .discount(new BigDecimal(getRequiredText(offerNode, "discount")))
            .percentDiscount(new BigDecimal(getRequiredText(offerNode, "percentDiscount")))
            .build();
    }

    private StockDTO parseStockDTO(JsonNode offerNode) {
        return StockDTO.builder()
            .quantity(offerNode.get("stock").asDouble())
            .stockUnit(offerNode.get("stockUnit").asText().equalsIgnoreCase("kg") ?
                StockUnit.KG : StockUnit.EACH)
            .build();
    }


    private TimingDTO parseTimingDTO(JsonNode offerNode) {
        try {
            String startTimeStr = getRequiredText(offerNode, "startTime");
            String endTimeStr = getRequiredText(offerNode, "endTime");

            // Parse ISO 8601 timestamps til Instant og konverter til LocalDateTime
            LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.parse(startTimeStr),
                ZoneId.systemDefault()
            );

            LocalDateTime endTime = LocalDateTime.ofInstant(
                Instant.parse(endTimeStr),
                ZoneId.systemDefault()
            );

            return TimingDTO.builder()
                .startTime(startTime)
                .endTime(endTime)
                .lastUpdated(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            LOGGER.error("Error parsing dates: {} - {}", offerNode, e.getMessage());
            throw new IllegalArgumentException("Could not parse dates: " + e.getMessage());
        }
    }

    private Set<CategoryDTO> parseCategories(JsonNode productNode) {
        Set<CategoryDTO> categories = new HashSet<>();
        JsonNode categoriesNode = productNode.get("categories");

        if (categoriesNode != null) {
            try {
                // Get the category paths directly from da and en fields
                String pathDa = categoriesNode.get("da").asText();
                String pathEn = categoriesNode.get("en").asText();

                LOGGER.debug("Parsing category paths - DA: {}, EN: {}", pathDa, pathEn);

                // Split on > to get hierarchy
                String[] categoryPathsDa = pathDa.split(">");
                String[] categoryPathsEn = pathEn.split(">");

                if (categoryPathsDa.length != categoryPathsEn.length) {
                    LOGGER.warn("Mismatched category path lengths: DA={}, EN={}", pathDa, pathEn);
                    return categories;
                }

                StringBuilder currentPathDa = new StringBuilder();
                StringBuilder currentPathEn = new StringBuilder();

                // Create a category for each level in the hierarchy
                for (int i = 0; i < categoryPathsDa.length; i++) {
                    String categoryNameDa = categoryPathsDa[i].trim();
                    String categoryNameEn = categoryPathsEn[i].trim();

                    // Build the cumulative path
                    if (currentPathDa.length() > 0) {
                        currentPathDa.append(">");
                        currentPathEn.append(">");
                    }
                    currentPathDa.append(categoryNameDa);
                    currentPathEn.append(categoryNameEn);

                    // Create a category for this level
                    CategoryDTO categoryDTO = CategoryDTO.builder()
                        .nameDa(categoryNameDa)
                        .nameEn(categoryNameEn)
                        .pathDa(currentPathDa.toString())
                        .pathEn(currentPathEn.toString())
                        .build();

                    categories.add(categoryDTO);
                    LOGGER.debug("Added category: {} ({}) with path: {}",
                        categoryNameDa, categoryNameEn, currentPathDa.toString());
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing categories: {}", e.getMessage(), e);
            }
        } else {
            LOGGER.debug("No categories found for product");
        }

        LOGGER.debug("Final categories set size: {}", categories.size());
        return categories;
    }

    private ProductDTO parseProduct(JsonNode clearanceNode) {
        try {
            JsonNode offerNode = clearanceNode.get("offer");
            JsonNode productNode = clearanceNode.get("product");

            if (offerNode == null || productNode == null) {
                throw new IllegalArgumentException("Missing required offer or product data");
            }

            ProductDTO product = ProductDTO.builder()
                .productName(getRequiredText(productNode, "description"))
                .ean(getRequiredText(productNode, "ean"))
                .price(parsePriceDTO(offerNode))
                .stock(parseStockDTO(offerNode))
                .timing(parseTimingDTO(offerNode))
                .categories(parseCategories(productNode))
                .build();

            // Add debug logging
            LOGGER.debug("Parsed product: {} with {} categories",
                product.getProductName(),
                product.getCategories().size());

            if (!product.getCategories().isEmpty()) {
                LOGGER.debug("Categories for {}: {}",
                    product.getProductName(),
                    product.getCategories().stream()
                        .map(c -> c.getNameDa() + " (" + c.getNameEn() + ")")
                        .collect(Collectors.joining(" > ")));
            }

            return product;

        } catch (Exception e) {
            LOGGER.error("Error parsing product: {}", e.getMessage());
            return null;
        }
    }

    private String extractLastCategory(String categoryPath) {
        return categoryPath.substring(categoryPath.lastIndexOf(">") + 1).trim();
    }
}