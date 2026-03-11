package com.beratcan.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLDelete(sql = "UPDATE publishers SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "publishers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_publisher_name", columnNames = "name")
})
@Getter
@Setter
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "publisher", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    private List<Book> books = new ArrayList<>();

    @Column(nullable = false)
    private boolean isDeleted = false; // Soft Delete mantığı için
}
