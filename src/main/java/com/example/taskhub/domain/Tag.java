package com.example.taskhub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
