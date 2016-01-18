package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import message.ConfirmBox;

public class Main extends Application {
    private Stage mainStage;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        mainStage = primaryStage;

        Parent root = FXMLLoader.load(getClass().getResource("DBLogin.fxml"));

        mainStage.setTitle("Admin panel");
        mainStage.setOnCloseRequest(e -> {
            e.consume();
            showCloseConfirmMessage();
        });

        mainStage.setScene(new Scene(root));
        mainStage.setResizable(false);
        mainStage.show();
    }

    private void showCloseConfirmMessage() {
        if (ConfirmBox.display("Confirm", "Are you sure you want to exit?")) {
            mainStage.close();
        }
    }
}
