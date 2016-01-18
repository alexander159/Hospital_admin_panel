package app;

import message.ConfirmBox;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private Stage mainStage;

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
        mainStage.show();
    }

    private void showCloseConfirmMessage() {
        if (ConfirmBox.display("Confirm", "Are you sure you want to exit?")) {
            mainStage.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
