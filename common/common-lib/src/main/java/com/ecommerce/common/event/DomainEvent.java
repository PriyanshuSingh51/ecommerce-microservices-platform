package com.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

/** Common envelope shape every domain event follows across services. */
public record DomainEvent<T>(
    String eventId,
    String eventType,
    String aggregateId,
    Instant timestamp,
    T payload
) {
    public static <T> DomainEvent<T> of(String eventType, String aggregateId, T payload) {
        return new DomainEvent<>(UUID.randomUUID().toString(), eventType, aggregateId, Instant.now(), payload);
    }
}
