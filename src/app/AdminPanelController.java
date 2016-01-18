package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminPanelController implements Initializable {
    @FXML
    public ComboBox hospitalComboBox;

    @FXML
    public Label hospitalLabel;

    @FXML
    public Label yearLabel;

    @FXML
    public ComboBox yearComboBox;

    @FXML
    public Button downloadButton;

    @FXML
    public ListView recordsListView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void handleDownloadAction(ActionEvent event) {

    }
}
