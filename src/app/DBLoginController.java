package app;

import javafx.event.ActionEvent;
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

    }

    @FXML
    public void handleLoginAction(ActionEvent event) throws IOException {
        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        if (checkCredentials()) {
            Parent adminPanel = FXMLLoader.load(getClass().getResource("AdminPanel.fxml"));

            mainStage.hide();
            mainStage.setScene(new Scene(adminPanel));
            mainStage.show();
        }
    }

    private boolean checkCredentials() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // jdbc:mysql://<host>[:<port>]/<database_name>
        try (Connection ignored = DriverManager.getConnection(
                String.format("jdbc:mysql://%s/%s", url.getText(), "novacar9_chikitsa"),
                login.getText(),
                password.getText())) {
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_URL, url.getText());
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_USER, login.getText());
            DatabaseCredentials.getInstance().getCredentials().put(Constants.DB_PASSWORD, password.getText());

            return true;
        } catch (SQLException e) {
            incorrectLoginLabel.setText("An error occurred. Maybe user/password is invalid");
            e.printStackTrace();
        }

        return false;
    }
}