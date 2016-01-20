package util;

import java.util.LinkedHashMap;
import java.util.Map;

public class Constants {
    public static final String DB_URL = "db_url";
    public static final String DB_USER = "db_user";
    public static final String DB_PASSWORD = "db_password";
    public static final String DB_NAME = "novacar9_chikitsa";

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String PDF_RESOURCES = "pdf-resources";
    public static final String FILE_DOWNLOADING_DIR = PDF_RESOURCES + FILE_SEPARATOR + "load";

    public static final Map<Integer, String> MONTHS = new LinkedHashMap<Integer, String>() {
        {
            put(1, "January");
            put(2, "February");
            put(3, "March");
            put(4, "April");
            put(5, "May");
            put(6, "June");
            put(7, "July");
            put(8, "August");
            put(9, "September");
            put(10, "October");
            put(11, "November");
            put(12, "December");
        }
    };
}
