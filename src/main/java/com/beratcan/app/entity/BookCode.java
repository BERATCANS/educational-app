package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "book_codes")
public class BookCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Kod değeri

    @Column(nullable = false)
    private boolean used = false; // Kullanıldı mı?

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book; // Bu kod hangi kitap için

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User usedBy; // Kod kullanıldıysa hangi kullanıcı (nullable - kullanılmamışsa null)
}
