package org.example.chatapplicationfinalexam.server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.chatapplicationfinalexam.client.ChatClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class ChatServer {

    private static final int PORT = 5000;
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private boolean isRunning = false;
    private ServerSocket serverSocket;

    @FXML
    private TextArea txtServerStatus;

    public void initialize(){
        appendStatus("Server Started, click 'Add client' to start the chat");
    }

    void appendStatus(String msg) {
        Platform.runLater(() -> txtServerStatus.appendText(msg + "\n"));
    }


    @FXML
    void btnAddClientOnAction(ActionEvent event) {
            if (!isRunning){
                startServer();
            }
        openClientWindow();
    }

    private void openClientWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Chat Client");
                stage.setScene(scene);
                ;               stage.setOnCloseRequest(event -> {
                    ChatClient controller = loader.getController();
                    controller.closeConnection();
                });
                stage.show();
                appendStatus("New client window opened");
            } catch (IOException e) {
                appendStatus("Error opening client window");
                e.printStackTrace();
            }
        });
    }

    private void startServer() {
        isRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendStatus("Server started on port " + PORT);
                while(isRunning){
                    Socket clientSocket = serverSocket.accept();
                    appendStatus("New client Added");

                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isRunning){
                    appendStatus("Error starting server: " + e.getMessage());
                }
            }
        }).start();
    }

    private class ClientHandler implements Runnable{
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while(true){
                    out.writeObject("SUBMITNAME");
                    clientName = (String) in.readObject();
                    if (!clientName.trim().isEmpty() && clientName != null){
                        break;
                    }
                    appendStatus("Invalid name, requesting again");
                }

                out.writeObject("NAMEACCEPTED");
                appendStatus("Client " + clientName + " connected");
                broadcast("TEXT " + clientName + " joined the Group chat");

                synchronized (writers){
                    writers.add(out);
                }

                while(true){
                    Object message = in.readObject();
                    if (message == null) break;
                    if (message instanceof String){
                        String text = (String) message;
                        broadcast("TEXT "+text);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                appendStatus("Error connecting to client");
            }finally {
                if (clientName != null){
                    appendStatus("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat BYE");
                }

                synchronized (writers){
                    writers.remove(out);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    appendStatus("Error closing client socket");
                    e.printStackTrace();
                }
            }
        }
        private void broadcast ( String message){
            synchronized (writers){
                for (ObjectOutputStream writer : writers){
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        appendStatus("Error Broadcast the message..");
                    }
                }
            }
        }
    }

}
