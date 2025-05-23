package org.example.chatapplicationfinalexam.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;

public class ChatClient {

    @FXML
    private Button btnSend;

    @FXML
    private ListView<Object> messageView;

    @FXML
    private TextField txtMessage;

    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private String clientName;
    private boolean nameAccepted = false;

    public void initialize() {
        messageView.setCellFactory(listView -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String) {
                    setText((String) item);
                    setGraphic(null);
                }
            }
        });
        try {
            Socket socket = new Socket("localhost", 5000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(() -> listenForMessages());
            thread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void promptForName(){
        Platform.runLater(()-> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your name");
            dialog.setHeaderText("Please enter your name");

            dialog.showAndWait().ifPresent(name -> {
                clientName = name.trim();
                if (clientName.isEmpty()) {
                    messageView.getItems().add("Name can't be empty, Please Enter Your Name");
                    promptForName();
                } else {
                    try {
                        outputStream.writeObject(clientName);
                        outputStream.flush();
                        dialog.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageView.getItems().add("Error sending name, Please try again");
                    }
                }
            });
        });
    }
    public void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        String date = LocalDate.now().toString();
        String time = LocalTime.now().toString();
        try {
            while(true){
                Object message = inputStream.readObject();
                if (message == null) break;
                if (message instanceof String){
                    String text = (String) message;
                    if (text.startsWith("SUBMITNAME")){
                        if (!nameAccepted){
                            promptForName();
                        }
                    }else if (text.startsWith("NAMEACCEPTED")){
                        nameAccepted = true;
                        Platform.runLater(() -> messageView.getItems().add(date));
                        Platform.runLater(() -> messageView.getItems().add("Connected as " + clientName));
                    }else if (text.startsWith("TEXT")){
                        Platform.runLater(() -> messageView.getItems().add(time));
                        Platform.runLater(() -> messageView.getItems().add(text.substring(5)));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> messageView.getItems().add("Disconnected" + e.getMessage()));
        }finally {
            closeConnection();
        }
    }

    @FXML
    void btnSendOnAction(ActionEvent event) {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty()) return;
        try {
            outputStream.writeObject(msg);
            outputStream.flush();
            txtMessage.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
