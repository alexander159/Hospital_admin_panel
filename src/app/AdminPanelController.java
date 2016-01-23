package app;

import aws.SimpleAmazonS3Service;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.HospitalShortInfo;
import model.PharmacyStoreOrder;
import org.apache.commons.io.FileDeleteStrategy;
import util.*;

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
    @FXML
    public CheckBox saveAsPdfCheckBox;
    @FXML
    public CheckBox saveAsImageCheckBox;
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label downloadedImagesLabel;
    @FXML
    public Label createdPdfsLabel;

    private LinkedList<HospitalShortInfo> hospitals;
    private LinkedList<PharmacyStoreOrder> pharmacyStoreOrders;
    private String selectedHospitalFolderName;
    private int downloadedImagesCount;
    private int createdPDFsCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            hospitals = loadHospitals();
            hospitals.forEach(hospitalShortInfo -> hospitalComboBox.getItems().add(hospitalShortInfo.getId(), hospitalShortInfo.getHospitalName()));

            progressBar.setMaxWidth(Double.MAX_VALUE);  //set button width equals window width
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
                recordsListView.getItems().add(i, Constants.Values.MONTHS.get(i + 1) + " " + recordsCountPerMonth[i] + " records");
            }
        });

        saveAsImageCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            saveAsPdfCheckBox.setSelected(false);
        });

        saveAsPdfCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            saveAsImageCheckBox.setSelected(false);
        });

        downloadButton.setOnAction(event -> {
            if (hospitalComboBox.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Hospital is not selected!\n");
            } else if (yearComboBox.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Year is not selected!\n");
            } else if (recordsListView.getSelectionModel().getSelectedItem() == null) {
                logTextArea.appendText("Month is not selected!\n");
            } else if (!saveAsImageCheckBox.isSelected() && !saveAsPdfCheckBox.isSelected()) {
                logTextArea.appendText("'Save as image' or 'Save as PDF' ignored!\n");
            } else {
                final int[] month = {0};
                Constants.Values.MONTHS.forEach((monthNumber, monthValue) -> {
                    if (String.valueOf(recordsListView.getSelectionModel().getSelectedItem()).contains(monthValue)) {
                        month[0] = monthNumber;
                    }
                });
                pharmacyStoreOrders = loadPharmacyStoreOrders(month[0]);

                initSelectedHospitalDownloadingFolder();    //create name variable for selected current hospital
                progressBar.setProgress(0d);
                logTextArea.clear();
                downloadedImagesCount = 0;
                createdPDFsCount = 0;
                downloadedImagesLabel.setText("Downloaded images: 0/" + getCountOfImagesToDownload(pharmacyStoreOrders));
                createdPdfsLabel.setText("Created PDFs: 0/" + pharmacyStoreOrders.size());

                if (pharmacyStoreOrders.size() != 0) {
                    downloadAllData();
                }
            }
        });
    }

    private void setDownloadedImagesCount(int count) {
        Platform.runLater(() -> downloadedImagesLabel.setText("Downloaded images: " + count + downloadedImagesLabel.getText().substring(downloadedImagesLabel.getText().indexOf("/"))));
    }

    private void setCreatedPDFsCount(int count) {
        Platform.runLater(() -> createdPdfsLabel.setText("Created PDFs: " + count + createdPdfsLabel.getText().substring(createdPdfsLabel.getText().indexOf("/"))));
    }

    private void updateProgressBar(double value) {
        Platform.runLater(() -> progressBar.setProgress(value)); //from 0 to 1
    }

    private int getCountOfImagesToDownload(LinkedList<PharmacyStoreOrder> ordersList) {
        final int[] count = {0};
        ordersList.forEach(order -> order.getOrderImages().forEach(img -> count[0]++));

        return count[0];
    }

    private void downloadAllData() {
        Task loadImagesTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (FileManager.isDirectoryExist(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf")) {
                    if (!FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf")) {
                        logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf" + "!\n");
                        throw new Exception("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf");
                    }
                }

                if (FileManager.isDirectoryExist(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
                    if (FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
                        downloadImages();
                    } else {
                        logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
                        throw new Exception("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName);
                    }
                } else {
                    downloadImages();
                }
                return null;
            }
        };

        loadImagesTask.setOnFailed(e -> {
            lockGuiElements(false);
            logTextArea.appendText("=======Downloading failed=======\n");
            System.err.println("Downloading failed");

            //delete downloading directory with all files inside
            if (FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
                logTextArea.appendText("Directory deleted:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
            } else {
                logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
            }
        });

        loadImagesTask.setOnSucceeded(e -> {
            logTextArea.appendText("=======Downloading finished=======\n");

            if (saveAsPdfCheckBox.isSelected()) {
                progressBar.setProgress(0d);
                Task createPdf = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        if (FileManager.createDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf")) {
                            logTextArea.appendText("Directory created: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf\n");
                            createPdfFiles();
                        } else {
                            logTextArea.appendText("Directory failed to create: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf!\n");
                            throw new Exception("Directory failed to create: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf");
                        }
                        return null;
                    }
                };

                createPdf.setOnFailed(e2 -> {
                    lockGuiElements(false);
                    logTextArea.appendText("=======PDF creating failed=======\n");
                    System.err.println("Pdf creating failed");

                    //delete images directory with all files inside
                    if (FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
                        logTextArea.appendText("Directory deleted:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
                    } else {
                        logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
                    }

                    //delete pdf directory with all files inside
                    if (FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf")) {
                        logTextArea.appendText("Directory deleted:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf!\n");
                    } else {
                        logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf!\n");
                    }
                });

                createPdf.setOnSucceeded(e1 -> {
                    logTextArea.appendText("=======PDF creating finished=======\n");
                    lockGuiElements(false);

                    //delete images directory with all files inside
                    if (FileManager.removeDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
                        logTextArea.appendText("Directory deleted:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
                    } else {
                        logTextArea.appendText("Removing directory failed:" + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
                    }

                    lockGuiElements(false);
                });

                new Thread(createPdf).start();
            } else {
                lockGuiElements(false);
            }
        });

        new Thread(loadImagesTask).start();
    }

    private void createPdfFiles() {
        logTextArea.appendText("=======PDF creating started=======\n");
        ExecutorService es = Executors.newFixedThreadPool(10);

        pharmacyStoreOrders.forEach(order -> es.execute(() -> {
            try {
                PDF.create(order, Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName, Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf");
                Platform.runLater(() -> logTextArea.appendText("PDF created: " + String.format("patient%s_order%s.pdf", order.getPatientId(), order.getOrderId()) + "\n"));
                setCreatedPDFsCount(++createdPDFsCount);
                updateProgressBar(createdPDFsCount / Double.parseDouble((createdPdfsLabel.getText().substring(createdPdfsLabel.getText().indexOf("/") + 1))));
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> logTextArea.appendText("Failed to create PDF. OrderId = " + order.getOrderId() + "\n"));

                try {
                    FileDeleteStrategy.FORCE.delete(new File(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "-pdf"
                            + Constants.Directory.FILE_SEPARATOR + String.format("patient%s_order%s.pdf", order.getPatientId(), order.getOrderId())));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void downloadImages() throws Exception {
        if (FileManager.createDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName)) {
            logTextArea.appendText("Directory created: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "\n");
            logTextArea.appendText("=======Downloading started=======\n");

            lockGuiElements(true);
            ExecutorService es = Executors.newFixedThreadPool(10);
            if (saveAsPdfCheckBox.isSelected()) {
                pharmacyStoreOrders.forEach(order -> order.getOrderImages().forEach(imgFileName -> es.execute(() -> {
                            try {
                                Platform.runLater(() -> logTextArea.appendText("Downloading " + imgFileName + "...\n"));
                                downloadImgFromS3Server(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName, imgFileName);
                                //Platform.runLater(() -> logTextArea.appendText("Download finished " + imgFileName + "!\n"));
                                setDownloadedImagesCount(++downloadedImagesCount);
                                updateProgressBar(downloadedImagesCount / Double.parseDouble((downloadedImagesLabel.getText().substring(downloadedImagesLabel.getText().indexOf("/") + 1))));
                            } catch (IOException e) {
                                System.err.println("Download failed " + imgFileName + "\n");
                                Platform.runLater(() -> logTextArea.appendText("Download failed " + imgFileName + "\n"));
                                e.printStackTrace();
                            }
                        }
                )));
            } else {
                pharmacyStoreOrders.forEach(order -> {
                    if (FileManager.createDirectory(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + Constants.Directory.FILE_SEPARATOR +
                            String.format("patient%s_order%s", order.getPatientId(), order.getOrderId()))) {
                        Platform.runLater(() -> logTextArea.appendText("Directory created: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + Constants.Directory.FILE_SEPARATOR +
                                String.format("patient%s_order%s", order.getPatientId(), order.getOrderId()) + "\n"));
                        order.getOrderImages().forEach(imgFileName -> es.execute(() -> {
                                    try {
                                        Platform.runLater(() -> Platform.runLater(() -> logTextArea.appendText("Downloading " + imgFileName + "...\n")));
                                        downloadImgFromS3Server(Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName
                                                + Constants.Directory.FILE_SEPARATOR + String.format("patient%s_order%s", order.getPatientId(), order.getOrderId()), imgFileName);
                                        //todo bug (nullpointer exception if uncomment)
                                        //Platform.runLater(() -> logTextArea.appendText("Download finished " + imgFileName + "!\n"));
                                        setDownloadedImagesCount(++downloadedImagesCount);
                                        updateProgressBar(downloadedImagesCount / Double.parseDouble((downloadedImagesLabel.getText().substring(downloadedImagesLabel.getText().indexOf("/") + 1))));
                                    } catch (IOException e) {
                                        System.err.println("Download failed " + imgFileName + "\n");
                                        Platform.runLater(() -> logTextArea.appendText("Download failed " + imgFileName + "\n"));
                                        e.printStackTrace();
                                    }
                                }
                        ));
                    } else {
                        Platform.runLater(() -> logTextArea.appendText("Directory failed to create: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + Constants.Directory.FILE_SEPARATOR +
                                String.format("patient%s_order%s", order.getPatientId(), order.getOrderId())));
                    }
                });
            }

            es.shutdown();
            try {
                es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            logTextArea.appendText("Directory failed to create: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName + "!\n");
            throw new Exception("Directory failed to create: " + Constants.Directory.OUTPUT_DIR + Constants.Directory.FILE_SEPARATOR + selectedHospitalFolderName);
        }
    }

    private void downloadImgFromS3Server(String savePath, String fileName) throws IOException {
        SimpleAmazonS3Service amazonS3Service = new SimpleAmazonS3Service();
        amazonS3Service.downloadFromS3(fileName, new File(savePath + Constants.Directory.FILE_SEPARATOR + fileName));
    }

    private void lockGuiElements(boolean state) {
        hospitalComboBox.setDisable(state);
        yearComboBox.setDisable(state);
        recordsListView.setDisable(state);
        saveAsImageCheckBox.setDisable(state);
        saveAsPdfCheckBox.setDisable(state);
        downloadButton.setDisable(state);
    }

    private void initSelectedHospitalDownloadingFolder() {
        final int[] month = {0};
        Constants.Values.MONTHS.forEach((monthNumber, monthValue) -> {
            if (String.valueOf(recordsListView.getSelectionModel().getSelectedItem()).contains(monthValue)) {
                month[0] = monthNumber;
            }
        });

        selectedHospitalFolderName = String.format("%s_%s_%s",
                String.valueOf(Constants.Values.MONTHS.get(month[0])),
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
                String.format("jdbc:mysql://%s/%s", DatabaseCredentials.getInstance().getCredentials().get(Constants.Database.DB_URL), Constants.Database.DB_NAME),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.Database.DB_USER),
                DatabaseCredentials.getInstance().getCredentials().get(Constants.Database.DB_PASSWORD));
    }
}
