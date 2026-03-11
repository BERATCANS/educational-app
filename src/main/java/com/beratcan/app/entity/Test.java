package com.beratcan.app.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@SQLDelete(sql = "UPDATE tests SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    @JsonBackReference
    private Topic topic;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Question> questions = new ArrayList<>();

    @Column(name = "number_of_questions", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer numberOfQuestions = 0;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(nullable = false)
    private boolean isDeleted = false; // Soft Delete mantığı için
}
