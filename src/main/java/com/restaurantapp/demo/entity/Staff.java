package com.restaurantapp.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staffs", indexes = { @Index(name = "idx_staff_user", columnList = "user_id"), @Index(name = "idx_staff_position", columnList = "position") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private Double salary;

    @NotBlank
    private String position;

    private LocalDate dateJoined;

    private LocalDate dateLeft;

    @NotBlank
    @Column(unique = true)
    private String cin;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // One-to-One: Associated user account
    @OneToOne
    @JoinColumn(name = "user_id" ,nullable = true ,unique = true)
    private User user;
}
