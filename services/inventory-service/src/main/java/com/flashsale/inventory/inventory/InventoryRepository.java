package com.flashsale.inventory.inventory;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryEntity i where i.sku = :sku")
    Optional<InventoryEntity> findBySkuForUpdate(@Param("sku") String sku);
}