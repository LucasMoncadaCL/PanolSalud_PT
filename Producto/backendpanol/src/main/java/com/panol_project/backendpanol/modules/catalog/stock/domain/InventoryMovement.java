package com.panol_project.backendpanol.modules.catalog.stock.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "inventory_movement")
public class InventoryMovement {

    @Id
    private String id;

    @Field("implement_id")
    private Integer implementId;

    private MovementAction action;

    private Integer quantity;

    @Field("performed_by")
    private Integer performedBy;

    private Instant timestamp;

    private String notes;

    public InventoryMovement() {
    }

    public InventoryMovement(Integer implementId, MovementAction action, Integer quantity, Integer performedBy, Instant timestamp, String notes) {
        this.implementId = implementId;
        this.action = action;
        this.quantity = quantity;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getImplementId() {
        return implementId;
    }

    public void setImplementId(Integer implementId) {
        this.implementId = implementId;
    }

    public MovementAction getAction() {
        return action;
    }

    public void setAction(MovementAction action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Integer performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
