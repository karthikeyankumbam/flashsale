package com.flashsale.catalog.product;

import com.flashsale.catalog.api.dto.CreateProductRequest;
import com.flashsale.catalog.api.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Update;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {
    @Mock ProductRepository repo;
    @Mock ProductQueries queries;
    ProductService service;

    @BeforeEach void setUp() { service = new ProductService(repo, queries); }

    @Test
    void creationNormalizesDisplayFieldsAndPreservesExistingPriceUnits() {
        when(repo.insert(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        var req = new CreateProductRequest("PHONE-1", " Phone ", " Phones ", 19999L, "inr",
                null, null, " Details ", null);
        Product saved = service.create(req);
        assertThat(saved.getName()).isEqualTo("Phone");
        assertThat(saved.getCategory()).isEqualTo("Phones");
        assertThat(saved.getCurrency()).isEqualTo("INR");
        assertThat(saved.getPrice()).isEqualTo(19999L);
        assertThat(saved.getDescription()).isEqualTo("Details");
        assertThat(saved.getImages()).isEmpty();
        assertThat(saved.isActive()).isTrue();
        verify(repo, never()).save(any(Product.class));
    }

    @Test
    void duplicateInsertReturnsConflictInsteadOfOverwritingAnotherProduct() {
        when(repo.insert(any(Product.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(() -> service.create(new CreateProductRequest("PHONE-1", "Phone", "Phones",
                1L, "INR", true, null, null, null))).isInstanceOf(DuplicateSkuException.class);
        verify(repo, never()).save(any(Product.class));
    }

    @Test
    void hiddenProductsAreUnavailablePubliclyButCanBePreviewedByAdmin() {
        Product hidden = new Product("PHONE-1", "Phone", "Phones", 19999L, "INR", false, Map.of());
        when(repo.findById("PHONE-1")).thenReturn(Optional.of(hidden));
        assertThatThrownBy(() -> service.getBySku("PHONE-1")).isInstanceOf(ProductNotFoundException.class);
        assertThat(service.getForAdmin("PHONE-1")).isSameAs(hidden);
    }

    @Test
    void oldClientsCannotEraseNewOptionalFieldsOrChangeSkuWhenUpdating() {
        service.update("PHONE-1", new UpdateProductRequest("New name", "Phones", 20000L,
                "INR", null, null, null, null));
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(queries).update(eq("PHONE-1"), update.capture());
        Map<?, ?> fields = (Map<?, ?>) update.getValue().getUpdateObject().get("$set");
        assertThat(fields.keySet()).isEqualTo(Set.of("name", "category", "price", "currency", "updatedAt"));
    }

    @Test
    void explicitEmptyValuesClearOptionalFields() {
        service.update("PHONE-1", new UpdateProductRequest("Name", "Phones", 20000L,
                "INR", null, Map.of(), "", List.of()));
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(queries).update(eq("PHONE-1"), update.capture());
        Map<?, ?> fields = (Map<?, ?>) update.getValue().getUpdateObject().get("$set");
        assertThat(fields.get("description")).isEqualTo("");
        assertThat(fields.get("images")).isEqualTo(List.of());
        assertThat(fields.get("attributes")).isEqualTo(Map.of());
    }

    @Test
    void hidingKeepsProductRecordAndDoesNotOverwriteOtherFields() {
        service.delete("PHONE-1");
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(queries).update(eq("PHONE-1"), update.capture());
        Map<?, ?> fields = (Map<?, ?>) update.getValue().getUpdateObject().get("$set");
        assertThat(fields.get("active")).isEqualTo(false);
        assertThat(fields.keySet()).isEqualTo(Set.of("active", "updatedAt"));
        verifyNoInteractions(repo);
    }

    @Test
    void searchValidatesSortAndKeepsUniqueTieBreaker() {
        assertThat(new ProductSearch(null, null, null, null, null, 1, 10, "price-asc")
                .pageable().getSort().stream().map(order -> order.getProperty()).toList())
                .containsExactly("price", "sku");
        assertThatThrownBy(() -> new ProductSearch(null, null, null, null, null, 0, 10, "raw-db-field").pageable())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
