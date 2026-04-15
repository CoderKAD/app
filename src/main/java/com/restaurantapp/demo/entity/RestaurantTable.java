package com.restaurantapp.demo.entity;


import com.restaurantapp.demo.entity.enums.TableStatus;
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
@Table(name = "restaurant_tables", indexes = { @Index(name = "idx_tables_public_code", columnList = "public_code"), @Index(name = "idx_tables_user", columnList = "user_id"), @Index(name = "idx_tables_active", columnList = "active"), @Index(name = "idx_tables_status", columnList = "status"), @Index(name = "idx_tables_seats", columnList = "seats") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Label is required")
    @Size(min = 2, max = 50, message = "Label must be between 2 and 50 characters")
    private String label;

    private Integer seats;

    @Column(name = "public_code")
    private String publicCode;

    private Boolean active;

    private TableStatus status;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Many-to-One: User who manages this table
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // One-to-Many: Orders placed at this table
    @OneToMany(mappedBy = "restaurantTable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    // Many-to-Many: Reservations for this table
    @ManyToMany(mappedBy = "tables")
    private List<Reservation> reservations;
}

