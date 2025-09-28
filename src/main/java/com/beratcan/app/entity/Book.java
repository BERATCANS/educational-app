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

    // Hesaplanmış alanlar - veritabanında saklanmaz
    @Transient
    private Integer numberOfTopics;

    @Transient
    private Integer numberOfTests;

    @Transient
    private Integer numberOfQuestions;

    // Hesaplayıcı metodlar
    public int getNumberOfTopics() {
        return topics != null ? topics.size() : 0;
    }

    public int getNumberOfTests() {
        return topics != null ? topics.stream()
                .mapToInt(topic -> topic.getTests() != null ? topic.getTests().size() : 0)
                .sum() : 0;
    }

    public int getNumberOfQuestions() {
        return topics != null ? topics.stream()
                .flatMap(
                        topic -> topic.getTests() != null ? topic.getTests().stream() : java.util.stream.Stream.empty())
                .mapToInt(test -> test.getQuestions() != null ? test.getQuestions().size() : 0)
                .sum() : 0;
    }
}
