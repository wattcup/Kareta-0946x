package ru.gr0946x.db.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;


@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_received", nullable = false)
    private boolean isReceived;

    public Message() {
    }

    public Message(User author, Long receiverId, String content) {
        this.author = author;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.isReceived = false;
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean received) {
        isReceived = received;
    }

    @Override
    public String toString() {
        return "Post[" + id + "] " + "at "
                + createdAt.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                + ": " + content
                + " by " + author.getNick();
    }
}