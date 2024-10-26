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
        try {
            String url = String.format(FOODWASTE_URL, storeId);
            HttpRequest request = buildRequest(url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            validateResponse(response);
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode clearances = rootNode.get("clearances");

            if (clearances == null || !clearances.isArray()) {
                return new ArrayList<>();
            }

            List<ProductDTO> products = new ArrayList<>();
            for (JsonNode clearanceNode : clearances) {
                ProductDTO product = parseProduct(clearanceNode);
                if (product != null) {
                    products.add(product);
                }
            }

            LOGGER.info("Fetched {} products for store {}", products.size(), storeId);
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
        if (response.statusCode() != 200) {
            switch (response.statusCode()) {
                case 404 -> throw new ApiException(404, "Store not found");
                case 401 -> throw new ApiException(401, "Invalid API key");
                case 429 -> throw new ApiException(429, "Rate limit exceeded");
                default -> throw new ApiException(response.statusCode(),
                    "Unexpected error from Salling API");
            }
        }
    }

    private ProductDTO parseProduct(JsonNode clearanceNode) {
        try {
            JsonNode offerNode = clearanceNode.get("offer");
            JsonNode productNode = clearanceNode.get("product");

            if (offerNode == null || productNode == null) {
                return null;
            }

            return ProductDTO.builder()
                .productName(getRequiredText(productNode, "description"))
                .ean(getRequiredText(productNode, "ean"))
                .price(parsePriceDTO(offerNode))
                .stock(parseStockDTO(offerNode))
                .timing(parseTimingDTO(offerNode))
                .categories(parseCategories(productNode))
                .build();

        } catch (Exception e) {
            return null;
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
        String startTimeStr = getRequiredText(offerNode, "startTime");
        String endTimeStr = getRequiredText(offerNode, "endTime");

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
    }

    private Set<CategoryDTO> parseCategories(JsonNode productNode) {
        Set<CategoryDTO> categories = new HashSet<>();
        JsonNode categoriesNode = productNode.get("categories");

        if (categoriesNode != null) {
            String categoryDa = categoriesNode.get("da").asText();
            String categoryEn = categoriesNode.get("en").asText();

            categories.add(CategoryDTO.fromSallingCategory(
                extractLastCategory(categoryDa),
                extractLastCategory(categoryEn),
                categoryDa,
                categoryEn
            ));
        }

        return categories;
    }

    private String extractLastCategory(String categoryPath) {
        return categoryPath.substring(categoryPath.lastIndexOf(">") + 1).trim();
    }
}