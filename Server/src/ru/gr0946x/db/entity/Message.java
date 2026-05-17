package ru.gr0946x.db.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Сущность публикации (поста) в социальной сети.
 * <p>
 * Отображается на таблицу {@code posts}. Каждая запись привязана к автору
 * через внешний ключ {@code author_id} и содержит текстовое содержимое
 * с меткой времени создания.
 * <p>
 * Класс предназначен для использования с Spring Data JPA и Hibernate.
 *
 * @author Маклецов С. В.
 * @see User
 * @see ru.smak.db.repository.PostRepository
 */
@Entity
@Table(name = "posts")
public class Message {

    /**
     * Уникальный идентификатор публикации.
     * <p>
     * Генерируется автоматически базой данных при вставке новой записи
     * с использованием стратегии {@link GenerationType#IDENTITY}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ссылка на пользователя-автора публикации.
     * <p>
     * Аннотация {@code @ManyToOne} указывает, что множество постов
     * может быть связано с одним пользователем.
     * <p>
     * {@code fetch = FetchType.LAZY} предотвращает загрузку данных автора
     * при чтении поста, что оптимизирует производительность при выборке
     * больших списков публикаций.
     * <p>
     * {@code @JoinColumn} явно задаёт имя столбца внешнего ключа
     * в таблице {@code posts}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Текстовое содержимое публикации.
     * <p>
     * Поле не может быть пустым и хранится в столбце типа TEXT
     * для поддержки длинных сообщений.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Дата и время создания публикации.
     * <p>
     * Значение устанавливается автоматически при создании экземпляра
     * и не изменяется в дальнейшем.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Конструктор по умолчанию, требуемый спецификацией JPA.
     * <p>
     * Используется инфраструктурой Hibernate при загрузке сущностей
     * из базы данных. Не должен вызываться в бизнес-коде приложения.
     */
    public Message() {}

    /**
     * Создаёт новую публикацию с указанным автором и содержимым.
     * <p>
     * Время создания устанавливается автоматически в момент вызова.
     *
     * @param author пользователь-автор публикации (не {@code null})
     * @param content текстовое содержимое поста (не {@code null}, не пустое)
     */
    public Message(User author, String content) {
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Post[" + id + "] " + "at "
                + createdAt.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                + ": " + content
                + " by " + author.getNick();
    }
}