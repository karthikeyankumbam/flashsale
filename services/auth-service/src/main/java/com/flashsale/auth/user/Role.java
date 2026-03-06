package com.flashsale.auth.user;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected Role() {}

    public Role(String name) { this.name = name; }

    public java.util.UUID getId() { return id; }
    public String getName() { return name; }
}