package com.example.oms.orderservice.common.idempotency;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_commands")
public class ProcessedCommand {

    @Id
    private UUID commandId;

    private UUID orderId;

    private Instant processedAt;

    public ProcessedCommand() {
    }

    public ProcessedCommand(UUID commandId, UUID orderId) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.processedAt = Instant.now();
    }

    public UUID getCommandId() {
        return commandId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
