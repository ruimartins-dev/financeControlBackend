package com.example.financialcontrol.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hidden_subcategories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "subcategory_id"})
})
@Data
@NoArgsConstructor
public class HiddenSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private Subcategory subcategory;

    @Column(name = "hidden_at", nullable = false, updatable = false)
    private LocalDateTime hiddenAt;

    @PrePersist
    protected void onCreate() {
        this.hiddenAt = LocalDateTime.now();
    }

    public HiddenSubcategory(User user, Subcategory subcategory) {
        this.user = user;
        this.subcategory = subcategory;
    }
}

