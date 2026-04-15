package com.restaurantapp.demo.entity;



import com.restaurantapp.demo.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_username", columnList = "user_name"), @Index(name = "idx_users_email", columnList = "email") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_name", unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(name = "password_hash", nullable = false)
    @NotBlank(message = "Password is required")
    private String passwordHash;

    @Column(unique = true, nullable = false)
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role roles =Role.CUSTOMER;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // One-to-Many: Orders created by this user
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> ordersCreated;

    // One-to-Many: Orders updated by this user
    @OneToMany(mappedBy = "updatedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> ordersUpdated;

    // One-to-Many: Restaurant tables managed by this user
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantTable> restaurantTables;

    // One-to-Many: Reservations created by this user
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservationsCreated;

    // One-to-Many: Reservations updated by this user
    @OneToMany(mappedBy = "updatedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservationsUpdated;



    // One-to-One: Staff profile
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Staff staff;

}
