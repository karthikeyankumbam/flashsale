package com.flashsale.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.catalog.product.Product;
import com.flashsale.catalog.product.ProductRepository;
import com.flashsale.catalog.support.TestTokens;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "security.jwt.secret=" + TestTokens.SECRET,
        "security.jwt.issuer=" + TestTokens.ISSUER
})
class CatalogIntegrationTests {
    @Container static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl("catalog_integration"));
    }

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository repo;
    @Autowired MongoTemplate template;
    private static final String ADMIN = "/products/admin/items";

    @BeforeEach
    void clearIsolatedTestProducts() { repo.deleteAll(); }

    private Map<String, Object> product(String sku, String name, String category, long price, boolean active) {
        return new LinkedHashMap<>(Map.of("sku", sku, "name", name, "category", category,
                "price", price, "currency", "INR", "active", active, "description", "Product information",
                "images", List.of("https://example.com/product.jpg"), "attributes", Map.of("color", "Black")));
    }

    private ResponseEntity<String> request(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception { return mapper.readTree(response.getBody()); }

    private void create(String sku, String name, String category, long price, boolean active) {
        assertThat(request(HttpMethod.POST, ADMIN, product(sku, name, category, price, active), TestTokens.admin()).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void ownerCanCreatePreviewEditPublishHideAndRepublishAProduct() throws Exception {
        String token = TestTokens.admin();
        create("PHONE-1", "Phone", "Phones", 19999, false);
        assertThat(json(request(HttpMethod.GET, "/products", null, null)).get("totalElements").asInt()).isZero();
        assertThat(request(HttpMethod.GET, "/products/PHONE-1", null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode preview = json(request(HttpMethod.GET, ADMIN + "/PHONE-1", null, token));
        assertThat(preview.get("description").asText()).isEqualTo("Product information");
        assertThat(preview.get("images").size()).isEqualTo(1);
        assertThat(json(request(HttpMethod.GET, ADMIN + "?visibility=hidden", null, token)).get("totalElements").asInt()).isEqualTo(1);

        Map<String, Object> edit = Map.of("name", "Phone Plus", "category", "Phones", "price", 20999, "currency", "inr");
        JsonNode edited = json(request(HttpMethod.PUT, ADMIN + "/PHONE-1", edit, token));
        assertThat(edited.get("active").asBoolean()).isFalse();
        assertThat(edited.get("description").asText()).isEqualTo("Product information");
        assertThat(edited.get("images").size()).isEqualTo(1);
        assertThat(edited.get("sku").asText()).isEqualTo("PHONE-1");

        assertThat(request(HttpMethod.PUT, ADMIN + "/PHONE-1/visibility", Map.of("active", true), token).getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode published = json(request(HttpMethod.GET, "/products/PHONE-1", null, null));
        assertThat(published.get("name").asText()).isEqualTo("Phone Plus");
        assertThat(published.get("price").asLong()).isEqualTo(20999);
        assertThat(request(HttpMethod.DELETE, "/products/PHONE-1", null, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repo.findById("PHONE-1")).isPresent();
        assertThat(request(HttpMethod.GET, "/products/PHONE-1", null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(HttpMethod.PUT, ADMIN + "/PHONE-1/visibility", Map.of("active", true), token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(HttpMethod.GET, "/products/PHONE-1", null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void customersCanCombineFiltersSortAndReachEveryPage() throws Exception {
        create("P-B", "beta Phone", "Phones", 200, true);
        create("P-A", "Alpha Phone", "phones", 200, true);
        create("P-C", "Gamma Phone", "Phones", 300, true);
        create("A-1", "Phone cover", "Accessories", 100, true);
        create("H-1", "Hidden phone", "Private", 50, false);
        String query = "/products?query=phone&category=PHONES&currency=inr&minPrice=200&maxPrice=300&sort=price-asc&size=1";
        JsonNode first = json(request(HttpMethod.GET, query + "&page=0", null, null));
        JsonNode second = json(request(HttpMethod.GET, query + "&page=1", null, null));
        JsonNode last = json(request(HttpMethod.GET, query + "&page=2", null, null));
        assertThat(first.get("totalElements").asInt()).isEqualTo(3);
        assertThat(first.get("totalPages").asInt()).isEqualTo(3);
        assertThat(first.at("/content/0/sku").asText()).isEqualTo("P-A");
        assertThat(second.at("/content/0/sku").asText()).isEqualTo("P-B");
        assertThat(last.at("/content/0/sku").asText()).isEqualTo("P-C");
        assertThat(last.get("last").asBoolean()).isTrue();
        assertThat(json(request(HttpMethod.GET, query + "&page=3", null, null)).get("content").isEmpty()).isTrue();
        JsonNode alphabetical = json(request(HttpMethod.GET, "/products?category=phones&sort=name-asc", null, null));
        assertThat(alphabetical.at("/content/0/sku").asText()).isEqualTo("P-A");
        assertThat(json(request(HttpMethod.GET, "/products?query=P-B", null, null)).get("totalElements").asInt()).isEqualTo(1);
        assertThat(json(request(HttpMethod.GET, "/products?query=.*", null, null)).get("totalElements").asInt()).isZero();
        JsonNode categories = json(request(HttpMethod.GET, "/products/browse/categories", null, null));
        assertThat(categories.size()).isEqualTo(2);
        assertThat(categories.toString()).doesNotContain("Private");
    }

    @Test
    void existingMongoRecordsWithoutNewFieldsRemainReadableAndEditable() throws Exception {
        template.getCollection("products").insertOne(new Document("_id", "LEGACY")
                .append("name", "Original").append("category", "Phones").append("price", 100L)
                .append("currency", "INR").append("active", true));
        JsonNode original = json(request(HttpMethod.GET, "/products/LEGACY", null, null));
        assertThat(original.get("description").asText()).isEmpty();
        assertThat(original.get("images").isEmpty()).isTrue();
        assertThat(original.get("price").asLong()).isEqualTo(100);
        assertThat(request(HttpMethod.PUT, "/products/LEGACY",
                Map.of("name", "Updated", "category", "Phones", "price", 101, "currency", "INR"),
                TestTokens.admin()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findById("LEGACY").orElseThrow().getName()).isEqualTo("Updated");
    }

    @Test
    void simultaneousDuplicateCreatesNeverOverwriteTheWinner() throws Exception {
        String token = TestTokens.admin();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<ResponseEntity<String>>> results = new ArrayList<>();
            for (String name : List.of("First", "Second")) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for start");
                    return request(HttpMethod.POST, ADMIN, product("DUPLICATE", name, "Phones", 100, true), token);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            String winner = null;
            for (Future<ResponseEntity<String>> result : results) {
                ResponseEntity<String> response = result.get(30, TimeUnit.SECONDS);
                statuses.add(response.getStatusCode().value());
                if (response.getStatusCode() == HttpStatus.CREATED) winner = json(response).get("name").asText();
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(repo.count()).isEqualTo(1);
            assertThat(repo.findById("DUPLICATE").orElseThrow().getName()).isEqualTo(winner);
        }
    }

    @Test
    void unauthorizedWritesNeverChangeDataAndFractionsAreNotSilentlyTruncated() {
        create("PHONE-1", "Original", "Phones", 100, true);
        assertThat(request(HttpMethod.DELETE, "/products/PHONE-1", null, TestTokens.customer()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(request(HttpMethod.GET, ADMIN, null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repo.findById("PHONE-1").orElseThrow().isActive()).isTrue();
        Map<String, Object> invalid = product("FRACTION", "Fraction", "Phones", 100, true);
        invalid.put("price", 10.5);
        assertThat(request(HttpMethod.POST, ADMIN, invalid, TestTokens.admin()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repo.findById("FRACTION")).isEmpty();
        assertThat(request(HttpMethod.PUT, ADMIN + "/MISSING/visibility", Map.of("active", true), TestTokens.admin()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void explicitEmptyDetailFieldsCanBeCleared() throws Exception {
        create("PHONE-1", "Phone", "Phones", 100, true);
        Map<String, Object> edit = product("PHONE-1", "Phone", "Phones", 100, true);
        edit.put("description", "");
        edit.put("images", List.of());
        edit.put("attributes", Map.of());
        JsonNode changed = json(request(HttpMethod.PUT, ADMIN + "/PHONE-1", edit, TestTokens.admin()));
        assertThat(changed.get("description").asText()).isEmpty();
        assertThat(changed.get("images").isEmpty()).isTrue();
        assertThat(changed.get("attributes").isEmpty()).isTrue();
    }
}
