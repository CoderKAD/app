package com.restaurantapp.demo.entity;


import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = { @Index(name = "idx_orders_public_code", columnList = "public_code"), @Index(name = "idx_orders_type", columnList = "type_order"), @Index(name = "idx_orders_status", columnList = "status"), @Index(name = "idx_orders_table", columnList = "table_id"), @Index(name = "idx_orders_created_by", columnList = "created_by"), @Index(name = "idx_orders_created_at", columnList = "created_at") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_code")
    private String publicCode;

    @Enumerated(EnumType.STRING)
    private OrderType typeOrder;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Size(max = 20)
    private String phone;


    // Many-to-One: Table where this order is placed
    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTable restaurantTable;

    // Many-to-One: User who created this order
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Many-to-One: User who last updated this order
    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    // One-to-Many: Items in this order
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

}