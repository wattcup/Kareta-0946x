package ru.gr0946x.ui;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SwingUi implements Ui {

    private final List<Consumer<String>> listeners = new ArrayList<>();
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    public void start() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Мессенджер Карета");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(650, 600);
            frame.setLayout(new BorderLayout());

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

            listModel = new DefaultListModel<>();
            userList = new JList<>(listModel);
            userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane listScroll = new JScrollPane(userList);
            listScroll.setPreferredSize(new Dimension(150, 0));
            listScroll.setBorder(BorderFactory.createTitledBorder("В сети:"));
            frame.add(listScroll, BorderLayout.EAST);

            userList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.isBlank()) {
                        notifyListeners("history:" + selectedUser.trim());
                    }
                }
            });

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton historyButton = new JButton("История чата");
            JButton searchButton = new JButton("Поиск по чату");
            topPanel.add(historyButton);
            topPanel.add(searchButton);
            frame.add(topPanel, BorderLayout.NORTH);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            inputField = new JTextField();
            JButton sendButton = new JButton("Отправить");

            bottomPanel.add(inputField, BorderLayout.CENTER);
            bottomPanel.add(sendButton, BorderLayout.EAST);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            sendButton.addActionListener(this::handleInput);
            inputField.addActionListener(this::handleInput);

            historyButton.addActionListener(e -> handleHistoryRequest());
            searchButton.addActionListener(e -> handleSearchRequest());

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void handleInput(ActionEvent e) {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null && !selectedUser.isEmpty()) {
                notifyListeners(selectedUser + ":" + text);
                userList.clearSelection();
            } else {
                notifyListeners(text);
            }
            inputField.setText("");
        }
    }

    private void handleHistoryRequest() {
        String target = JOptionPane.showInputDialog(frame, "Введите имя пользователя для загрузки истории:");
        if (target != null && !target.isBlank()) {
            notifyListeners("history:" + target.trim());
        }
    }

    private void handleSearchRequest() {
        String target = JOptionPane.showInputDialog(frame, "Введите имя собеседника:");
        if (target == null || target.isBlank()) return;

        String fragment = JOptionPane.showInputDialog(frame, "Введите текст для поиска:");
        if (fragment != null && !fragment.isBlank()) {
            notifyListeners("find:" + target.trim() + ":" + fragment.trim());
        }
    }

    private void notifyListeners(String text) {
        for (Consumer<String> listener : listeners) {
            listener.accept(text);
        }
    }

    @Override
    public void showInfo(String data, MessageType type) {
        SwingUtilities.invokeLater(() -> {
            switch (type) {
                case MESSAGE -> {
                    String[] message = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
                    if (message.length == 2) {
                        chatArea.append(message[0] + " написал: \n");
                        chatArea.append(message[1] + "\n\n");
                    } else {
                        chatArea.append(data + "\n\n");
                    }
                }
                case ERROR -> {
                    chatArea.append("[ОШИБКА]: " + data + "\n\n");
                }
                default -> {
                    chatArea.append("[ИНФО]: " + data + "\n\n");
                }
                case USERS_LIST -> {
                    listModel.clear();
                    if (!data.isBlank()) {
                        for (String user : data.split(",")) {
                            if (!user.isBlank()) listModel.addElement(user);
                        }
                    }
                }
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }
}