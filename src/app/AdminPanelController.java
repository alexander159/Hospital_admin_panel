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

import java.net.URL;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminPanelController implements Initializable {
    private static final String[] timestampFormats = {"dd/MM/yyyy", "yyyy-MM-dd"};
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
    private Map<String, String> hospitals;   //key = 'doctor_id_for_staff', value = 'hospital_name (doctor_id_for_staff)'

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            hospitals = loadHospitals();
            hospitals.forEach((key, value) -> hospitalComboBox.getItems().add(value));
            yearComboBox.setDisable(true);
        });
    }

    @FXML
    public void handleDownloadAction() {

    }

    @FXML
    public void handleHospitalComboBoxAction() {
        hospitalComboBox.getSelectionModel().getSelectedItem();

        if (yearComboBox.isDisable()) {
            yearComboBox.setDisable(false);
        }
        yearComboBox.getSelectionModel().clearSelection();
        yearComboBox.getItems().clear();

        TreeSet<Integer> years = loadYears();
        years.forEach(integer -> yearComboBox.getItems().add(integer));
    }

    private LinkedHashMap<String, String> loadHospitals() {
        LinkedHashMap<String, String> result = null;

        String sql = "SELECT doctor_id_for_staff, hospital_name\n" +
                "FROM ck_clinic_staff\n" +
                "WHERE NOT ( hospital_name =  'N/A' )\n" +
                "GROUP BY doctor_id_for_staff\n" +
                "ORDER BY  hospital_name ASC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            result = new LinkedHashMap<>();
            while (rs.next()) {
                result.put(rs.getString("doctor_id_for_staff"), String.format("%s (%s)", rs.getString("hospital_name"), rs.getString("doctor_id_for_staff")));
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
                getKeyByValue(hospitals, String.valueOf(hospitalComboBox.getSelectionModel().getSelectedItem()));

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            result = new TreeSet<>();
            while (rs.next()) {
                java.util.Date admissionDate = parseDate(rs.getString("admission_date"));
                if (admissionDate != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(admissionDate.getTime());
                    result.add(cal.get(Calendar.YEAR));
                } else {
                    System.err.println("impossible to parse: " + rs.getString("admission_date"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private java.util.Date parseDate(String d) {
        java.util.Date date = null;
        if (d != null) {
            for (String parse : timestampFormats) {
                SimpleDateFormat sdf = new SimpleDateFormat(parse);
                try {
                    date = sdf.parse(d);
                    System.out.println(parse);
                    break;
                } catch (ParseException ignore) {
                }
            }
        }
        return date;
    }

    private String getKeyByValue(Map<String, String> map, String valueToFind) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(valueToFind)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private Connection getConnection() throws SQLException {
        // jdbc:mysql://<host>[:<port>]/<database_name>
        return DriverManager.getConnection(
                String.format("jdbc:mysql://%s/%s", DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_URL), Constants.DB_NAME),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_USER),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_PASSWORD));
    }

}
