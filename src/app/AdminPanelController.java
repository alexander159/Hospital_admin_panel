package app;

import aws.SimpleAmazonS3Service;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.HospitalShortInfo;
import model.PharmacyStoreOrder;
import org.apache.commons.io.FileUtils;
import util.Constants;
import util.DatabaseCredentials;
import util.SimpleDateParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    @FXML
    public TextArea logTextArea;

    private LinkedList<HospitalShortInfo> hospitals;
    private LinkedList<PharmacyStoreOrder> pharmacyStoreOrders;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            hospitals = loadHospitals();
            hospitals.forEach(hospitalShortInfo -> hospitalComboBox.getItems().add(hospitalShortInfo.getId(), hospitalShortInfo.getHospitalName()));

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
            recordsListView.getSelectionModel().clearSelection();
            recordsListView.getItems().clear();

            if (newValue == null)
                return; //smth was selected in the hospitalComboBox for the first time, reset yearComboBox

            Integer[] recordsCountPerMonth = loadRecordsCountPerMonth();

            for (int i = 0; i < recordsCountPerMonth.length; i++) {
                recordsListView.getItems().add(i, Constants.MONTHS.get(i + 1) + " " + recordsCountPerMonth[i] + " records");
            }
        });

        downloadButton.setOnAction(event -> {
            if (hospitalComboBox.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Hospital is not selected!\n");
            } else if (yearComboBox.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Year is not selected!\n");
            } else if (recordsListView.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Month is not selected!\n");
            } else {
                final int[] month = {0};
                Constants.MONTHS.forEach((monthNumber, monthValue) -> {
                    if (String.valueOf(recordsListView.getSelectionModel().getSelectedItem()).contains(monthValue)) {
                        month[0] = monthNumber;
                    }
                });
                pharmacyStoreOrders = loadPharmacyStoreOrders(month[0]);

                if (pharmacyStoreOrders.size() != 0) {
                    Task task = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            ExecutorService es = Executors.newFixedThreadPool(10);
                            if (createMonthDownloadingDir()) {
                                lockGuiElements(true);
                                pharmacyStoreOrders.forEach(order -> es.execute(() -> downloadImages(order)));

                                es.shutdown();
                                try {
                                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                                } catch (InterruptedException ignored) {
                                }
                            }
                            return null;
                        }
                    };

                    task.setOnSucceeded(e -> {
                        logTextArea.appendText("=======Downloading finished=======\n");

                        //create pdf
                        lockGuiElements(false);
                    });
                    task.setOnFailed(e -> {
                        lockGuiElements(false);
                        logTextArea.appendText("=======Downloading failed=======\n");
                        System.err.println("Downloading failed");

                        //delete all downloaded files
                        File monthDir = new File(getCurrentDownloadingDir());
                        if (!monthDir.exists()) {
                            try {
                                FileUtils.deleteDirectory(monthDir);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });

                    new Thread(task).start();
                }
            }
        });
    }

    private void lockGuiElements(boolean state) {
        hospitalComboBox.setDisable(state);
        yearComboBox.setDisable(state);
        recordsListView.setDisable(state);
        downloadButton.setDisable(state);
    }

    private boolean createMonthDownloadingDir() {
        File monthDir = new File(getCurrentDownloadingDir());

        if (!monthDir.exists()) {
            if (monthDir.mkdirs()) {
                logTextArea.appendText(monthDir.getPath() + " directory created\n");
                System.out.println(monthDir.getPath() + " directory created\n");
                return true;
            } else {
                logTextArea.appendText(monthDir.getPath() + " failed to create directory\n");
                System.out.println(monthDir.getPath() + " failed to create directory\n");
                return false;
            }
        } else {
            logTextArea.appendText(monthDir.getPath() + " directory is already exist\n");
            System.out.println(monthDir.getPath() + " directory is already exist\n");
            return false;
        }
    }

    private void downloadImages(PharmacyStoreOrder pharmacyStoreOrder) {
        SimpleAmazonS3Service amazonS3Service = new SimpleAmazonS3Service();
        String monthDir = getCurrentDownloadingDir();

        for (int i = 0; i < pharmacyStoreOrder.getOrderImage().size(); i++) {
            final int finalI = i;
            try {
                Platform.runLater(() -> logTextArea.appendText("Downloading " + pharmacyStoreOrder.getOrderImage().get(finalI) + "...\n"));
                amazonS3Service.downloadFromS3(pharmacyStoreOrder.getOrderImage().get(i), new File(monthDir + Constants.FILE_SEPARATOR + pharmacyStoreOrder.getOrderImage().get(i)));
                Platform.runLater(() -> logTextArea.appendText("Download finished " + pharmacyStoreOrder.getOrderImage().get(finalI) + "!\n"));
            } catch (IOException e) {
                System.err.println("Download failed " + pharmacyStoreOrder.getOrderImage().get(i) + "\n");
                Platform.runLater(() -> logTextArea.appendText("Download failed " + pharmacyStoreOrder.getOrderImage().get(finalI) + "\n"));
                e.printStackTrace();
            }
        }
    }

    private String getCurrentDownloadingDir() {
        final int[] month = {0};
        Constants.MONTHS.forEach((monthNumber, monthValue) -> {
            if (String.valueOf(recordsListView.getSelectionModel().getSelectedItem()).contains(monthValue)) {
                month[0] = monthNumber;
            }
        });

        return Constants.FILE_DOWNLOADING_DIR +
                Constants.FILE_SEPARATOR +
                String.format("%s_%s_%s",
                        String.valueOf(Constants.MONTHS.get(month[0])),
                        String.valueOf(yearComboBox.getSelectionModel().getSelectedItem()),
                        String.valueOf(hospitalComboBox.getSelectionModel().getSelectedItem()).replace(" ", ""));
    }

    private LinkedList<HospitalShortInfo> loadHospitals() {
        LinkedList<HospitalShortInfo> result = new LinkedList<>();

        String sql = "SELECT doctor_id_for_staff, hospital_name\n" +
                "FROM ck_clinic_staff\n" +
                "WHERE NOT ( hospital_name =  'N/A' )\n" +
                "GROUP BY doctor_id_for_staff\n" +
                "ORDER BY  hospital_name ASC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int id = 0;
            while (rs.next()) {
                result.add(new HospitalShortInfo(id++, rs.getString("doctor_id_for_staff"), rs.getString("hospital_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private TreeSet<Integer> loadYears() {
        TreeSet<Integer> result = new TreeSet<>();

        String sql = "SELECT admission_date\n" +
                "FROM ck_pharmacy_store_order\n" +
                "WHERE admission_date != 'N/A' AND admission_date != '' AND admission_date NOT LIKE '%.%' AND doctor_id = " +
                hospitals.get(hospitalComboBox.getSelectionModel().getSelectedIndex()).getDoctorIdFromStaff();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SimpleDateParser.SimpleDate simpleDate = SimpleDateParser.parse(rs.getString("admission_date"));
                if (simpleDate != null) {
                    result.add(simpleDate.getYear());
                } else {
                    System.err.println("invalid date: " + rs.getString("admission_date"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Integer[] loadRecordsCountPerMonth() {
        Integer[] countPerMonth = new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        final String request = "SELECT COUNT(*)\n" +
                "FROM ck_pharmacy_store_order\n" +
                "WHERE admission_date != 'N/A' AND admission_date != '' AND admission_date NOT LIKE '%.%' AND doctor_id = " + hospitals.get(hospitalComboBox.getSelectionModel().getSelectedIndex()).getDoctorIdFromStaff() + "\n" +
                "AND (admission_date LIKE '%/<fullmonthnum>/<year>' OR admission_date LIKE '%/<shortmonthnum>/<year>' OR admission_date LIKE '<year>-<fullmonthnum>%' OR admission_date LIKE '<year>-<shortmonthnum>%')\n" +
                "AND order_image != '' AND order_image != 'N/A'";

        try (Connection con = getConnection()) {
            for (int i = 1; i <= 12; i++) {
                String sql = request;
                if (i >= 10) {
                    sql = sql.replace("<year>", String.valueOf(yearComboBox.getSelectionModel().getSelectedItem()))
                            .replace("<fullmonthnum>", String.valueOf(i))
                            .replace("<shortmonthnum>", String.valueOf(i));
                } else {
                    sql = sql.replace("<year>", String.valueOf(yearComboBox.getSelectionModel().getSelectedItem()))
                            .replace("<fullmonthnum>", "0" + String.valueOf(i))
                            .replace("<shortmonthnum>", String.valueOf(i));
                }

                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        countPerMonth[i - 1] = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return countPerMonth;
    }

    private LinkedList<PharmacyStoreOrder> loadPharmacyStoreOrders(int month) {
        LinkedList<PharmacyStoreOrder> result = new LinkedList<>();

        String sql = "SELECT order_id, patient_id, order_image, admission_date\n" +
                "FROM ck_pharmacy_store_order\n" +
                "WHERE admission_date != 'N/A' AND admission_date != '' AND admission_date NOT LIKE '%.%' AND doctor_id = " + hospitals.get(hospitalComboBox.getSelectionModel().getSelectedIndex()).getDoctorIdFromStaff() + "\n" +
                "AND (admission_date LIKE '%/<fullmonthnum>/<year>' OR admission_date LIKE '%/<shortmonthnum>/<year>' OR admission_date LIKE '<year>-<fullmonthnum>%' OR admission_date LIKE '<year>-<shortmonthnum>%')\n" +
                "AND order_image != '' AND order_image != 'N/A'";

        if (month >= 10) {
            sql = sql.replace("<year>", String.valueOf(yearComboBox.getSelectionModel().getSelectedItem()))
                    .replace("<fullmonthnum>", String.valueOf(month))
                    .replace("<shortmonthnum>", String.valueOf(month));
        } else {
            sql = sql.replace("<year>", String.valueOf(yearComboBox.getSelectionModel().getSelectedItem()))
                    .replace("<fullmonthnum>", "0" + String.valueOf(month))
                    .replace("<shortmonthnum>", String.valueOf(month));
        }

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String urls = rs.getString("order_image");
                if (urls == null || urls.trim().isEmpty() || urls.trim().equals("N/A")) {
                    System.err.println("invalid url:" + urls);
                } else {
                    result.add(new PharmacyStoreOrder(rs.getLong("order_id"), rs.getLong("patient_id"), SimpleDateParser.parse(rs.getString("admission_date")), PharmacyStoreOrder.parseUrlsString(rs.getString("order_image"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Connection getConnection() throws SQLException {
        // jdbc:mysql://<host>[:<port>]/<database_name>
        return DriverManager.getConnection(
                String.format("jdbc:mysql://%s/%s", DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_URL), Constants.DB_NAME),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_USER),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.DB_PASSWORD));
    }
}
