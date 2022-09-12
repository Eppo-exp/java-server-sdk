package com.eppo.sdk.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppDetails {

    static AppDetails instance;
    private String version;
    private String name;

    public static AppDetails getInstance () {
        if (AppDetails.instance == null){
            try {
                AppDetails.instance = new AppDetails();
            }
            catch (Exception e) {
                throw new RuntimeException("Unable to read properties file!");
            }
        }
        return AppDetails.instance;
    }

    public AppDetails() throws IOException {
        Properties prop = readPropertiesFile("app.properties");
        this.version = prop.getProperty("app.version", "1.0.0");
        this.name = prop.getProperty("app.name", "java-server-sdk");
    }

    public String getVersion() {
        return this.version;
    }

    public String getName() {
        return this.name;
    }

    public static Properties readPropertiesFile(String fileName) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try(InputStream resourceStream = loader.getResourceAsStream(fileName)) {
            props.load(resourceStream);
        }
        return props;
    }

}
