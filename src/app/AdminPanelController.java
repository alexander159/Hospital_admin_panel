package app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import util.Constants;
import util.DatabaseCredentials;
import util.SimpleDateParser;

import java.net.URL;
import java.sql.*;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.TreeSet;

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

    private LinkedList<String[]> hospitals;   //array contains 'doctor_id_for_staff', 'hospital_name'

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            hospitals = loadHospitals();
            for (int i = 0; i < hospitals.size(); i++) {
                hospitalComboBox.getItems().add(i, hospitals.get(i)[1]);
            }
            yearComboBox.setDisable(true);
        });

        hospitalComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (yearComboBox.isDisable()) {
                yearComboBox.setDisable(false);
            }
            yearComboBox.getSelectionModel().clearSelection();
            yearComboBox.getItems().clear();

            TreeSet<Integer> years = loadYears();
            years.forEach(y -> yearComboBox.getItems().add(y));
        });

        yearComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            //todo load count of records to table
        });


    }

    private LinkedList<String[]> loadHospitals() {
        LinkedList<String[]> result = null;

        String sql = "SELECT doctor_id_for_staff, hospital_name\n" +
                "FROM ck_clinic_staff\n" +
                "WHERE NOT ( hospital_name =  'N/A' )\n" +
                "GROUP BY doctor_id_for_staff\n" +
                "ORDER BY  hospital_name ASC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            result = new LinkedList<>();
            while (rs.next()) {
                result.add(new String[]{rs.getString("doctor_id_for_staff"), rs.getString("hospital_name")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private TreeSet<Integer> loadYears() {
        TreeSet<Integer> result = null;

        String sql = "SELECT admission_date\n" +
                "FROM ck_pharmacy_store_order\n" +
                "WHERE admission_date != 'N/A' AND admission_date != '' AND admission_date NOT LIKE '%.%' AND doctor_id = " +
                hospitals.get(hospitalComboBox.getSelectionModel().getSelectedIndex())[0];

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            result = new TreeSet<>();
            while (rs.next()) {
                if (SimpleDateParser.validateDate(rs.getString("admission_date"), 0)) {
                    result.add(SimpleDateParser.getYear(rs.getString("admission_date"), 0));
                    if (rs.getString("admission_date").length() < 10)
                        System.out.println(rs.getString("admission_date"));
                } else if (SimpleDateParser.validateDate(rs.getString("admission_date"), 1)) {
                    result.add(SimpleDateParser.getYear(rs.getString("admission_date"), 1));
                    if (rs.getString("admission_date").length() < 10)
                        System.out.println(rs.getString("admission_date"));
                } else {
                    System.err.println("invalid date: " + rs.getString("admission_date"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

//    private int loadCountOfRecords(int month, int year){
//        int count = 0;
//
//    }

    private Connection getConnection() throws SQLException {
        // jdbc:mysql://<host>[:<port>]/<database_name>
        return DriverManager.getConnection(
                String.format("jdbc:mysql://%s/%s", DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_URL), Constants.DB_NAME),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_USER),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_PASSWORD));
    }
}
