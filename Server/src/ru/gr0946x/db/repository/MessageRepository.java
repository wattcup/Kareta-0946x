package ru.gr0946x.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.db.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT m FROM Message m WHERE (" +
            "(:user2Id = 0L AND m.receiverId = 0L) OR " +
            "(:user2Id <> 0L AND ((m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id)))" +
            ") AND LOWER(m.content) LIKE LOWER(CONCAT('%', :fragment, '%')) ORDER BY m.createdAt ASC")
    List<Message> searchInChat(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id,
            @Param("fragment") String fragment
    );

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isReceived = true WHERE m.author.id = :authorId AND m.receiverId = :receiverId AND m.isReceived = false")
    void markAsRead(@Param("authorId") Long authorId, @Param("receiverId") Long receiverId);
}