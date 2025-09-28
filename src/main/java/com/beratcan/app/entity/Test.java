package com.beratcan.app.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    @JsonBackReference
    private Topic topic;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Question> questions = new ArrayList<>();

    @Column(name = "number_of_questions")
    private Integer numberOfQuestions = 0;
}
