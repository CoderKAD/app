package com.restaurantapp.demo.entity;

import com.restaurantapp.demo.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_start_at", columnList = "start_at"),
        @Index(name = "idx_reservations_end_at", columnList = "end_at"),
        @Index(name = "idx_reservations_customer_phone", columnList = "customer_phone"),
        @Index(name = "idx_reservations_created_by", columnList = "created_by"),
        @Index(name = "idx_reservations_status", columnList = "status"),
        @Index(name = "idx_reservations_code", columnList = "reservation_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // -------------------------
    // Reservation Info
    // -------------------------

    @Column(name = "reservation_code", unique = true)
    private String reservationCode;

    @Min(1)
    @Max(50)
    @Column(name = "number_of_people", nullable = false)
    private Integer numberOfPeople;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotBlank
    @Size(min = 8, max = 20)
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Email
    @Column(name = "email_customer")
    private String emailCustomer;

    // -------------------------
    // TIME (IMPORTANT FIX)
    // -------------------------

    @NotNull
    @Future
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Min(60)
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;

    // -------------------------
    // STATUS
    // -------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    // -------------------------
    // NOTES
    // -------------------------

    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String notes;

    // -------------------------
    // AUDIT
    // -------------------------

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "buffer_time_minutes")
    private Integer bufferTimeMinutes = 30;

    // -------------------------
    // RELATIONS
    // -------------------------

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @ManyToMany
    @JoinTable(
            name = "reservation_tables",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    private List<RestaurantTable> tables;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    private List<ReservationDemand> demands;
}
