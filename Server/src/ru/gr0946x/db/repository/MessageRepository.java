package ru.gr0946x.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.db.entity.Message;
import ru.gr0946x.db.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT m FROM Message m WHERE ((m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id)) AND LOWER(m.content) " +
            "LIKE LOWER(CONCAT('%', :fragment, '%')) ORDER BY m.createdAt ASC")
    List<Message> searchInChat(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id,
            @Param("fragment") String fragment
    );
}