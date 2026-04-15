package com.restaurantapp.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.*;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories_menu", indexes = { @Index(name = "idx_categories_menu_name", columnList = "category_name"), @Index(name = "idx_categories_menu_active", columnList = "active") })
@EntityListeners(AuditingEntityListener.class)
public class CategoryMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank
    @Column(name = "category_name" ,unique = true)
    private String categoryName;

    @NotNull
    @Column(name = "sort_order" , unique = true)
    @Positive(message = "sortOrder must be a positive number (greater than 0)")
    private Integer sortOrder;


    @NotNull
    private Boolean active;

    @CreatedDate
    @Column( name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column( name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    private List<MenuItem> menuItems;

}
