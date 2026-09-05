package com.flashsale.catalog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.catalog.product.*;
import com.flashsale.catalog.security.SecurityConfig;
import com.flashsale.catalog.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProductController.class, AdminProductController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.jwt.secret=" + TestTokens.SECRET,
        "security.jwt.issuer=" + TestTokens.ISSUER
})
class CatalogApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean ProductService service;
    private static final String ADMIN = "/products/admin/items";

    private Map<String, Object> body() {
        return new LinkedHashMap<>(Map.of("sku", "PHONE-1", "name", "Phone", "category", "Phones",
                "price", 19999, "currency", "INR", "description", "A compact phone",
                "images", List.of("https://example.com/phone.jpg")));
    }

    private Product product() {
        return new Product("PHONE-1", "Phone", "Phones", 19999L, "INR", true, Map.of());
    }

    @Test
    void customerCanBrowseWithoutLoginAndPaginationHasAnExplicitContract() throws Exception {
        when(service.search(any(), eq(true))).thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 20), 1));
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("PHONE-1"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.first").value(true)).andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void publicCategoriesAndProductDetailsRemainAccessible() throws Exception {
        when(service.categories()).thenReturn(List.of("Phones"));
        when(service.getBySku("PHONE-1")).thenReturn(product());
        mvc.perform(get("/products/browse/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Phones"));
        mvc.perform(get("/products/PHONE-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("")).andExpect(jsonPath("$.images").isEmpty());
    }

    @Test
    void adminAndCategoriesRemainValidProductSkus() throws Exception {
        when(service.getBySku("admin")).thenReturn(product());
        when(service.getBySku("categories")).thenReturn(product());
        mvc.perform(get("/products/admin")).andExpect(status().isOk());
        mvc.perform(get("/products/categories")).andExpect(status().isOk());
    }

    @Test
    void fractionalPricesAreRejectedInsteadOfTruncated() throws Exception {
        Map<String, Object> payload = body();
        payload.put("price", 10.5);
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/products", "/products/admin/items"})
    void anonymousProductCreationIsRejected(String path) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body())))
                .andExpect(status().isUnauthorized()).andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(service);
    }

    @Test
    void customerCannotReadAdminDataOrWriteViaEitherRoute() throws Exception {
        String token = "Bearer " + TestTokens.customer();
        mvc.perform(get(ADMIN).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN + "/PHONE-1").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post("/products").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(put("/products/PHONE-1").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/products/PHONE-1").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(put(ADMIN + "/PHONE-1/visibility").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/products", "/products/admin/items"})
    void validAdminTokenCanCreateProducts(String path) throws Exception {
        when(service.create(any())).thenReturn(product());
        mvc.perform(post(path).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value("PHONE-1"));
        verify(service).create(any());
    }

    static Stream<Arguments> invalidTokens() {
        Instant future = Instant.now().plusSeconds(300);
        return Stream.of(
                Arguments.of("malformed", "not-a-token"),
                Arguments.of("expired", TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER, "owner", List.of("ADMIN"), Instant.now().minusSeconds(300))),
                Arguments.of("wrong issuer", TestTokens.token(TestTokens.SECRET, "different-auth", "owner", List.of("ADMIN"), future)),
                Arguments.of("wrong signature", TestTokens.token("another-signing-key-at-least-thirty-two-characters", TestTokens.ISSUER, "owner", List.of("ADMIN"), future)),
                Arguments.of("missing expiry", TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER, "owner", List.of("ADMIN"), null)),
                Arguments.of("missing subject", TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER, null, List.of("ADMIN"), future)),
                Arguments.of("invalid roles", TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER, "owner", "ADMIN", future)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    void invalidTokensCannotAccessAdmin(String reason, String token) throws Exception {
        mvc.perform(get(ADMIN).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidProducts() {
        return Stream.of(Arguments.of("price", 0), Arguments.of("price", -1),
                Arguments.of("price", null), Arguments.of("name", " "),
                Arguments.of("currency", "ZZZ"), Arguments.of("sku", "bad/sku"),
                Arguments.of("description", "a".repeat(5001)),
                Arguments.of("images", List.of("javascript:alert(1)")),
                Arguments.of("images", List.of("https://user:password@example.com/p.jpg")),
                Arguments.of("images", Collections.singletonList(null)),
                Arguments.of("images", Collections.nCopies(9, "https://example.com/p.jpg")),
                Arguments.of("attributes", Map.of("$unsafe", "value")));
    }

    @ParameterizedTest(name = "invalid {0}: {index}")
    @MethodSource("invalidProducts")
    void productValidationReturnsFieldErrors(String field, Object value) throws Exception {
        Map<String, Object> payload = body();
        payload.put(field, value);
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        verifyNoInteractions(service);
    }

    @Test
    void invalidUpdatesAndMissingVisibilityAreRejected() throws Exception {
        Map<String, Object> payload = body();
        payload.put("price", -1);
        mvc.perform(put(ADMIN + "/PHONE-1").header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.price").exists());
        mvc.perform(put(ADMIN + "/PHONE-1/visibility").header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.active").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101", "size,abc", "minPrice,-1", "currency,ZZZ", "sort,anything"})
    void invalidBrowseParametersReturn400(String parameter, String value) throws Exception {
        mvc.perform(get("/products").param(parameter, value)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void inconsistentPriceRangeReturns400() throws Exception {
        mvc.perform(get("/products").param("minPrice", "100").param("maxPrice", "50"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("minPrice must not exceed maxPrice"));
        verifyNoInteractions(service);
    }

    @Test
    void missingProductDuplicateSkuAndDatabaseFailureHaveDistinctErrors() throws Exception {
        when(service.getBySku("MISSING")).thenThrow(new ProductNotFoundException("MISSING"));
        mvc.perform(get("/products/MISSING")).andExpect(status().isNotFound());
        when(service.create(any())).thenThrow(new DuplicateSkuException("PHONE-1"));
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors.sku").exists());
        when(service.getBySku("PHONE-1")).thenThrow(new DataAccessResourceFailureException("private connection details"));
        mvc.perform(get("/products/PHONE-1")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Please try again shortly."));
    }

    @Test
    void malformedJsonReturns400WithoutEchoingPayload() throws Exception {
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Invalid request body"));
    }

    @Test
    void adminCanPreviewHiddenProductsAndFilterTheirList() throws Exception {
        Product hidden = product();
        hidden.setActive(false);
        when(service.getForAdmin("PHONE-1")).thenReturn(hidden);
        when(service.search(any(), eq(false))).thenReturn(new PageImpl<>(List.of(hidden)));
        mvc.perform(get(ADMIN + "/PHONE-1").header("Authorization", "Bearer " + TestTokens.admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get(ADMIN).param("visibility", "hidden").header("Authorization", "Bearer " + TestTokens.admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].active").value(false));
        mvc.perform(get(ADMIN).param("visibility", "anything").header("Authorization", "Bearer " + TestTokens.admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletionReturns204AndUsesTheHideOperation() throws Exception {
        mvc.perform(delete(ADMIN + "/PHONE-1").header("Authorization", "Bearer " + TestTokens.admin()))
                .andExpect(status().isNoContent());
        verify(service).delete("PHONE-1");
    }
}
