package ru.gr0946x.db.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность, представляющая пользователя социальной сети.
 * <p>
 * Соответствует таблице {@code users} в базе данных. Каждый экземпляр класса
 * отражает одну запись (строку) с данными о зарегистрированном пользователе.
 * <p>
 * Класс предназначен для использования с Spring Data JPA и Hibernate.
 * Не содержит бизнес-логики — только данные и маппинг на реляционную схему.
 *
 * @author Маклецов С. В.
 * @see Entity
 * @see ru.smak.db.repository.UserRepository
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Уникальный идентификатор пользователя (первичный ключ).
     * <p>
     * Генерируется автоматически базой данных при вставке новой записи
     * с использованием стратегии {@link GenerationType#IDENTITY}.
     * <p>
     * Поле не имеет сеттера, так как значение устанавливается только
     * инфраструктурой JPA при сохранении сущности.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Псевдоним (никнейм) пользователя.
     * <p>
     * <b>Ограничения БД:</b>
     * <ul>
     *   <li>Не может быть {@code null} ({@code nullable = false})</li>
     *   <li>Должен быть уникальным в пределах таблицы
     *      ({@code unique = true})</li>
     *   <li>Сравнение при поиске регистронезависимое
     *      (реализуется на уровне репозитория)</li>
     * </ul>
     * Используется для аутентификации и отображения
     * имени пользователя в интерфейсе.
     */
    @Column(name = "nick", nullable = false, unique = true)
    private String nick;

    /**
     * Хэш пароля пользователя.
     * <p>
     * В этом поле хранится результат криптографического хэширования
     * (например, через BCrypt), а не пароль в открытом виде.
     * Это обеспечивает безопасность данных даже при компрометации базы.
     * <p>
     * Поле обязательно для заполнения ({@code nullable = false}).
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Коллекция публикаций, созданных данным пользователем.
     * <p>
     * Аннотация {@code @OneToMany} устанавливает связь «один-ко-многим»
     * с сущностью {@link Message}.
     * <p>
     * {@code mappedBy = "author"} указывает, что связь управляется
     * полем {@code author} в классе {@code Post}, поэтому Hibernate
     * не будет создавать дополнительный внешний ключ.
     * <p>
     * {@code cascade = CascadeType.PERSIST} позволяет сохранять новые посты
     * автоматически при сохранении пользователя, если они добавлены в коллекцию.
     * <p>
     * {@code fetch = FetchType.LAZY} предотвращает загрузку всех постов
     * при чтении пользователя, что критично для производительности.
     */
    @OneToMany(
            mappedBy = "author",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY
    )
    private List<Message> messages = new ArrayList<>();

    /**
     * Конструктор для создания нового пользователя с заданными данными.
     * <p>
     * Используется в сервисном слое при регистрации или тестировании.
     * Поле {@code id} остаётся {@code null} до момента сохранения в БД.
     *
     * @param nick псевдоним пользователя (не {@code null}, уникальный)
     * @param passwordHash хэш пароля (не {@code null})
     */
    public User(String nick, String passwordHash) {
        this.nick = nick;
        this.passwordHash = passwordHash;
    }

    /**
     * Пустой конструктор, требуемый спецификацией JPA.
     * <p>
     * Hibernate использует его для создания экземпляров сущности
     * при загрузке данных из базы данных через рефлексию.
     * Не должен вызываться в бизнес-коде приложения.
     */
    public User() {}

    /**
     * Возвращает уникальный идентификатор пользователя.
     *
     * @return ID пользователя или {@code null}, если сущность ещё не сохранена
     */
    public Long getId() {
        return id;
    }

    /**
     * Возвращает псевдоним пользователя.
     * @return никнейм в виде строки
     */
    public String getNick() {
        return nick;
    }

    /**
     * Устанавливает новое значение псевдонима.
     * <p>
     * Изменение ника должно сопровождаться проверкой на уникальность
     * через {@link ru.smak.db.service.UserService#isNickTaken(String)}.
     * @param nick новый псевдоним
     */
    public void setNick(String nick) {
        this.nick = nick;
    }

    /**
     * Возвращает хэш пароля.
     * @return строка с хэшем пароля
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Обновляет хэш пароля (например, при смене пароля пользователем).
     * <p>
     * <b>Внимание:</b> Метод ожидает на вход уже захэшированное значение.
     * Никогда не передавайте сюда пароль в открытом виде.
     * @param passwordHash новый хэш пароля
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Возвращает список публикаций пользователя.
     * @return коллекция постов (может быть пустой, но не {@code null})
     */
    public List<Message> getPosts() {
        return messages;
    }

    /**
     * Возвращает строковое представление сущности для отладки и логирования.
     * <p>
     * Формат вывода: {@code User ID: nick - passwordHash}.
     * <p>
     * <b>Предупреждение:</b> В продакшене не следует выводить {@code passwordHash}
     * в логах даже в захэшированном виде. Этот метод предназначен только
     * для учебной отладки.
     * @return краткое описание пользователя
     */
    @Override
    public String toString() {
        return "User " + id + ": " + nick + ": " + passwordHash;
    }
}