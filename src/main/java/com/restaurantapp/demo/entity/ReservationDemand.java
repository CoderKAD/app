package com.restaurantapp.demo.entity;




import com.restaurantapp.demo.entity.enums.DemandStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_request", indexes = { @Index(name = "idx_reservation_request_reservation", columnList = "reservation_id"), @Index(name = "idx_reservation_request_user", columnList = "user_id"), @Index(name = "idx_reservation_request_status", columnList = "status"), @Index(name = "idx_reservation_request_created_at", columnList = "created_at") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class ReservationDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private DemandStatus status;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Many-to-One: Reservation this demand is for (CHANGED from @OneToOne to @ManyToOne)
    @ManyToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // Many-to-One:  who made this demand
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
