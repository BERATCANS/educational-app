package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private List<Question> questions;

}

