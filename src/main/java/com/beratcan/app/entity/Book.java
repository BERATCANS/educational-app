package com.beratcan.app.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String publisher; // örn: Metin Yayınları

    private String coverImageUrl; // Kitap kapak fotoğrafı URL'si

    private boolean paid; // Kitap paralı mı?

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCode> codes = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Topic> topics = new ArrayList<>(); // Kitap konuları

    @ManyToMany(mappedBy = "ownedBooks")
    private Set<User> owners = new HashSet<>(); // Kitabı sahiplenen kullanıcılar
}
