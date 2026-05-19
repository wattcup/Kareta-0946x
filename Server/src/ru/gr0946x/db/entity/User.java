package ru.gr0946x.db.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nick", nullable = false, unique = true)
    private String nick;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(
            mappedBy = "author",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY
    )
    private List<Message> messages = new ArrayList<>();

    public User(String nick, String passwordHash) {
        this.nick = nick;
        this.passwordHash = passwordHash;
    }

    public User() {}

    public Long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return "User " + id + ": " + nick + ": " + passwordHash;
    }
}