package com.restaurantapp.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = { @Index(name = "idx_order_items_order", columnList = "order_id"), @Index(name = "idx_order_items_menu_item", columnList = "menu_item_id"), @Index(name = "idx_order_items_created_at", columnList = "created_at") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Many-to-One: Order this item belongs to
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Many-to-One: Menu item being ordered
    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
}

