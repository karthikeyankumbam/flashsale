package com.flashsale.payment.service;

import com.flashsale.payment.domain.PaymentEntity;
import com.flashsale.payment.events.PaymentRequestedEvent;
import com.flashsale.payment.repo.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentProcessor {

    private final PaymentRepository repo;

    public PaymentProcessor(PaymentRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public PaymentEntity process(PaymentRequestedEvent req) {
        UUID orderId = UUID.fromString(req.orderId());

        // idempotent: one payment per order
        var existing = repo.findByOrderId(orderId);
        if (existing.isPresent()) return existing.get();

        boolean shouldFail = req.amount() > 100_000;
        if (shouldFail) {
            return repo.save(new PaymentEntity(orderId, req.amount(), req.currency(), "FAILED",
                    "Amount exceeds limit for mock payment"));
        }
        return repo.save(new PaymentEntity(orderId, req.amount(), req.currency(), "SUCCEEDED", null));
    }
}