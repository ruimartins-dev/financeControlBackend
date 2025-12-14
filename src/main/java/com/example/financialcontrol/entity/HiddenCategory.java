package com.example.financialcontrol.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hidden_categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category_id"})
})
@Data
@NoArgsConstructor
public class HiddenCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "hidden_at", nullable = false, updatable = false)
    private LocalDateTime hiddenAt;

    @PrePersist
    protected void onCreate() {
        this.hiddenAt = LocalDateTime.now();
    }

    public HiddenCategory(User user, Category category) {
        this.user = user;
        this.category = category;
    }
}

