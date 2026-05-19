package ru.gr0946x.net;

import ru.gr0946x.db.dto.MessageDto;
import ru.gr0946x.db.entity.User;
import ru.gr0946x.db.service.MessageService;
import ru.gr0946x.db.service.UserService;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private String name = null;
    private final UserService userService;
    private final MessageService messageService;
    private String temporaryName = null;
    private User dbUser = null;

    public ConnectedClient(Socket socket, UserService userService, MessageService messageService)
            throws IOException {
        this.userService = userService;
        this.messageService = messageService;
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);

        communicator.setOnDisconnect(this::stop);

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
                        + (temporaryName == null ? "Введите имя:" : "Введите пароль:"));
                return;
            }

            if (temporaryName == null) {
                if (isInUse(data)) {
                    sendData(MessageType.ERROR
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + "Этот пользователь уже подключен к серверу");
                    sendData(MessageType.REQUEST
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + "Введите имя:");
                    return;
                }
                temporaryName = data;
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Введите пароль:");
                return;
            }

            try {
                if (userService.isNickTaken(temporaryName)) {
                    dbUser = userService.login(temporaryName, data);
                } else {
                    dbUser = userService.register(temporaryName, data);
                }
                name = temporaryName;
                sendForAll(MessageType.INFO, "Пользователь " + name + " вошел в чат");
                broadcastUsersList();
            } catch (IllegalArgumentException e) {
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + e.getMessage());
                temporaryName = null;
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

        if (commandOrNick.equalsIgnoreCase("read")) {
            User sender = userService.findByNick(content).orElse(null);
            if (sender != null) {
                messageService.markAsRead(sender.getId(), dbUser.getId());
            }
            return;
        }

        if (commandOrNick.equalsIgnoreCase("history")) {
            showHistory(content);
            return;
        }

        if (commandOrNick.equalsIgnoreCase("find")) {
            String[] searchParts = content.split(":", 2);
            if (searchParts.length == 2) {
                String targetNick = searchParts[0].trim();
                String fragment = searchParts[1].trim();

                if (targetNick.equalsIgnoreCase("all")) {
                    List<MessageDto> found = messageService.searchMessagesInChat(dbUser, 0L, fragment);
                    sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "--- Результаты поиска в общем чате ---");
                    for (MessageDto m : found) {
                        sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + m.authorNick() + ProtocolConstants.AUTHOR_SEPARATOR + m.content());
                    }
                } else {
                    searchHistory(targetNick, fragment);
                }
            } else {
                sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Неверный формат.");
            }
            return;
        }

        User receiver = userService.findByNick(commandOrNick).orElse(null);
        if (receiver == null) {
            sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не найден");
            return;
        }

        messageService.createMessage(dbUser, receiver.getId(), content, false);

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
        synchronized (clients) {
            clients.remove(this);
        }
        if (name != null) {
            broadcastUsersList();
            sendForAll(MessageType.INFO, "Пользователь " + name + " покинул чат");
        }
        communicator.stop();
    }

    private void showHistory(String targetNick) {
        User target = userService.findByNick(targetNick).orElse(null);
        if (target == null) {
            sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не найден");
            return;
        }
        List<MessageDto> history = messageService.getChatHistory(dbUser, target.getId());
        sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "--- История с " + targetNick + " ---");
        for (MessageDto m : history) {
            String status = m.isReceived() ? "[Прочитано]" : "[Не прочитано]";
            sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + m.authorNick() +
                    ProtocolConstants.AUTHOR_SEPARATOR + m.content() + " " + status);
        }
    }

    private void searchHistory(String targetNick, String fragment) {
        User target = userService.findByNick(targetNick).orElse(null);
        if (target == null) {
            sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не найден");
            return;
        }
        List<MessageDto> found = messageService.searchMessagesInChat(dbUser, target.getId(), fragment);
        sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "--- Результаты поиска ---");
        for (MessageDto m : found) {
            sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + m.authorNick() + ProtocolConstants.AUTHOR_SEPARATOR + m.content());
        }
    }

    private void broadcastUsersList() {
        synchronized (clients) {
            for (ConnectedClient target : clients) {
                if (target.name != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConnectedClient c : clients) {
                        if (c.name != null && !c.name.equals(target.name)) {
                            sb.append(c.name).append(",");
                        }
                    }
                    target.sendData(MessageType.USERS_LIST + ProtocolConstants.COMMAND_SEPARATOR + sb.toString());
                }
            }
        }
    }
}
