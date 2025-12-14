package com.example.financialcontrol.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subcategories")
@Data
@NoArgsConstructor
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 7)
    private String color;

    @Column(length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Subcategory(String name, Category category, boolean isDefault, User user) {
        this.name = name;
        this.category = category;
        this.isDefault = isDefault;
        this.user = user;
    }

    public Subcategory(String name, String description, String color, String icon, Category category, boolean isDefault, User user) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.category = category;
        this.isDefault = isDefault;
        this.user = user;
    }
}

