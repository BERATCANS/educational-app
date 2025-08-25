package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    private List<BookCode> codes;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Topic> topics; // Kitap konuları

    @ManyToMany(mappedBy = "ownedBooks")
    private Set<User> owners; // Kitabı sahiplenen kullanıcılar
}

