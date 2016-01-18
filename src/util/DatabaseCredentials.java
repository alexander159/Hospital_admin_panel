package util;

import java.util.HashMap;
import java.util.Map;

public class DatabaseCredentials {
    private static DatabaseCredentials instance;
    private Map<String, String> credentials = new HashMap<>();

    private DatabaseCredentials() {
    }

    public static DatabaseCredentials getInstance() {
        if (instance == null) {
            instance = new DatabaseCredentials();
        }
        return instance;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }
}

