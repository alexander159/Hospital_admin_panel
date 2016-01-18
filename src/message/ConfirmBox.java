package message;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfirmBox {
    private static boolean answer;

    public static boolean display(String title, String message) {
        Stage stage = new Stage();

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.setMinWidth(250);
        stage.setMinHeight(150);

        Label label = new Label();
        label.setText(message);

        //create two buttons
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");

        yesButton.setOnAction(e -> {
            answer = true;
            stage.close();
        });
        noButton.setOnAction(e -> {
            answer = false;
            stage.close();
        });

        HBox buttonLayout = new HBox(15);
        buttonLayout.getChildren().addAll(yesButton, noButton);
        buttonLayout.setAlignment(Pos.CENTER);

        VBox boxLayout = new VBox(25);
        boxLayout.getChildren().addAll(label, buttonLayout);
        boxLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(boxLayout);
        stage.setScene(scene);
        stage.showAndWait();

        return answer;
    }
}
