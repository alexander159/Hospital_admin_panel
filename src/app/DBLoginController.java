package app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.Constants;
import util.DatabaseCredentials;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DBLoginController implements Initializable {
    @FXML
    public TextField login;

    @FXML
    public PasswordField password;

    @FXML
    public Label loginLabel;

    @FXML
    public Button loginButton;

    @FXML
    public Label incorrectLoginLabel;

    @FXML
    public TextField url;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //At the time of initialize() controls are not yet ready to handle focus.
        Platform.runLater(loginLabel::requestFocus); //unfocus textfields

        loginButton.setOnAction(event -> {
            Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            if (checkMySQLCredentials()) {
                Parent adminPanel = null;
                try {
                    adminPanel = FXMLLoader.load(getClass().getResource("AdminPanel.fxml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mainStage.hide();
                mainStage.setScene(new Scene(adminPanel));
                mainStage.show();
            }
        });
    }

    private boolean checkMySQLCredentials() {
        // jdbc:mysql://<host>[:<port>]/<database_name>
        try (Connection ignored = DriverManager.getConnection(
                String.format("jdbc:mysql://%s/%s", url.getText(), Constants.DB_NAME),
                login.getText(),
                password.getText())) {
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_URL, url.getText());
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_USER, login.getText());
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_PASSWORD, password.getText());

            return true;
        } catch (SQLException e) {
            incorrectLoginLabel.setText(e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
