package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "book_codes")
public class BookCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // Kod değeri

    private boolean used = false; // Kullanıldı mı?

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book; // Bu kod hangi kitap için

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User usedBy; // Kod kullanıldıysa hangi kullanıcı
}

