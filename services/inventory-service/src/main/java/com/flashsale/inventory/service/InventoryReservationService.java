package com.flashsale.inventory.service;

import com.flashsale.inventory.events.OrderCreatedEvent;
import com.flashsale.inventory.inventory.InventoryRepository;
import com.flashsale.inventory.reservation.ReservationEntity;
import com.flashsale.inventory.reservation.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryReservationService {

    private final InventoryRepository inventoryRepo;
    private final ReservationRepository reservationRepo;

    public InventoryReservationService(InventoryRepository inventoryRepo, ReservationRepository reservationRepo) {
        this.inventoryRepo = inventoryRepo;
        this.reservationRepo = reservationRepo;
    }

    @Transactional
    public ReservationEntity reserve(OrderCreatedEvent event) {
        UUID orderId = UUID.fromString(event.orderId());

        // idempotency at consumer side:
        var existing = reservationRepo.findByOrderId(orderId);
        if (existing.isPresent()) return existing.get();

        // reserve each item (simple version)
        for (OrderCreatedEvent.Item item : event.items()) {
            var inv = inventoryRepo.findById(item.sku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found in inventory: " + item.sku()));
            inv.reserve(item.qty());
            inventoryRepo.save(inv);
        }

        ReservationEntity res = new ReservationEntity(orderId, "RESERVED");
        return reservationRepo.save(res);
    }

    @Transactional
    public ReservationEntity reject(OrderCreatedEvent event, String reason) {
        UUID orderId = UUID.fromString(event.orderId());
        var existing = reservationRepo.findByOrderId(orderId);
        if (existing.isPresent()) return existing.get();
        return reservationRepo.save(new ReservationEntity(orderId, "REJECTED"));
    }
}