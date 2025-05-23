module org.example.chatapplicationfinalexam {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.chatapplicationfinalexam.client to javafx.fxml;
    opens org.example.chatapplicationfinalexam.server to javafx.fxml;
    exports org.example.chatapplicationfinalexam;

}