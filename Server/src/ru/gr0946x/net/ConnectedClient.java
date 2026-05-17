package ru.gr0946x.net;

import ru.gr0946x.db.entity.Message;
import ru.gr0946x.db.entity.User;
import ru.gr0946x.db.service.MessageService;
import ru.gr0946x.db.service.UserService;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private String name = null;
    private final UserService userService;
    private final MessageService messageService;
    private String temporaryNick = null;
    private User dbUser = null;

    public ConnectedClient(Socket socket, UserService userService, MessageService messageService)
            throws IOException {
        this.userService = userService;
        this.messageService = messageService;
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
        synchronized (clients) {
            clients.add(this);
        }
    }

    public void start() {
        communicator.start();
        sendData(MessageType.REQUEST
                + ProtocolConstants.COMMAND_SEPARATOR
                + "Введите имя:");
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void parseData(String data) {
        if (name == null) {
            if (data.isBlank()) {
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Поле не может быть пустым");
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + (temporaryNick == null ? "Введите имя:" : "Введите пароль:"));
                return;
            }

            // Этап 1: Получение никнейма
            if (temporaryNick == null) {
                if (isInUse(data)) {
                    sendData(MessageType.ERROR
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + "Этот пользователь уже подключен к серверу");
                    sendData(MessageType.REQUEST
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + "Введите имя:");
                    return;
                }
                temporaryNick = data;
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Введите пароль:");
                return;
            }

            // Этап 2: Получение пароля и проверка в БД
            try {
                if (userService.isNickTaken(temporaryNick)) {
                    // Если ник есть в БД — пытаемся войти
                    dbUser = userService.login(temporaryNick, data);
                } else {
                    // Если ника нет — регистрируем нового пользователя
                    dbUser = userService.register(temporaryNick, data);
                }
                name = temporaryNick;
                sendForAll(MessageType.INFO, "Пользователь " + name + " вошел в чат");
            } catch (IllegalArgumentException e) {
                // Возвращаем ошибку валидации или неверного пароля клиенту
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + e.getMessage());
                temporaryNick = null; // Сбрасываем процесс авторизации
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Введите имя:");
            }
        } else {
            processMessage(data);
        }
    }

    private void processMessage(String data) {
        if (!data.contains(":")) {
            boolean isRead = clients.stream().anyMatch(c -> c != this && c.name != null);
            messageService.createMessage(dbUser, 0L, data, isRead);
            sendForAll(MessageType.MESSAGE, data);
            return;
        }

        String[] parts = data.split(":", 2);
        String commandOrNick = parts[0].trim();
        String content = parts[1].trim();

        if (commandOrNick.equalsIgnoreCase("history")) {
            showHistory(content);
            return;
        }

        if (commandOrNick.equalsIgnoreCase("find")) {
            String[] searchParts = content.split(":", 2);
            if (searchParts.length == 2) {
                searchHistory(searchParts[0].trim(), searchParts[1].trim());
            } else {
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Неверный формат. Используйте: find:пользователь:текст");
            }
            return;
        }

        User receiver = userService.findByNick(commandOrNick).orElse(null);
        if (receiver == null) {
            sendData(MessageType.ERROR
                    + ProtocolConstants.COMMAND_SEPARATOR
                    + "Пользователь не найден");
            return;
        }

        boolean isRead = clients.stream().anyMatch(c -> c.name != null && c.name.equalsIgnoreCase(commandOrNick));
        messageService.createMessage(dbUser, receiver.getId(), content, isRead);

        clients.stream()
                .filter(c -> c.name != null && (c.name.equalsIgnoreCase(commandOrNick) || c.name.equalsIgnoreCase(name)))
                .forEach(client -> client.sendData(MessageType.MESSAGE
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + name + ProtocolConstants.AUTHOR_SEPARATOR
                        + content));
    }

    private void sendForAll(MessageType type, String data) {
        var author = (type == MessageType.MESSAGE) ?
                name + ProtocolConstants.AUTHOR_SEPARATOR :
                "";
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.name != null)
                    .forEach(client -> {
                        client.sendData(type
                                + ProtocolConstants.COMMAND_SEPARATOR
                                + author
                                + data);
                    });
        }
    }

    private boolean isInUse(String name) {
        synchronized (clients) {
            return clients.stream()
                    .anyMatch(c -> c.name != null && c.name.equalsIgnoreCase(name));
        }
    }

    public void stop() {
        communicator.stop();
    }

    private void showHistory(String targetNick) {
        User target = userService.findByNick(targetNick).orElse(null);
        if (target == null) {
            sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не найден");
            return;
        }
        List<Message> history = messageService.getChatHistory(dbUser, target.getId());
        sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "--- История с " + targetNick + " ---");
        for (Message m : history) {
            String authorName = m.getAuthor().getId().equals(dbUser.getId()) ? name : targetNick;
            String status = m.isReceived() ? "[Прочитано]" : "[Не прочитано]";
            sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + authorName + ProtocolConstants.AUTHOR_SEPARATOR + m.getContent() + " " + status);
        }
    }

    private void searchHistory(String targetNick, String fragment) {
        User target = userService.findByNick(targetNick).orElse(null);
        if (target == null) {
            sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не найден");
            return;
        }
        List<Message> found = messageService.searchMessagesInChat(dbUser, target.getId(), fragment);
        sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "--- Результаты поиска ---");
        for (Message m : found) {
            String authorName = m.getAuthor().getId().equals(dbUser.getId()) ? name : targetNick;
            sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + authorName + ProtocolConstants.AUTHOR_SEPARATOR + m.getContent());
        }
    }
}
