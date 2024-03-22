package com.eppo.sdk.helpers;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class AppDetails {

    static AppDetails instance;
    private String version;
    private String name;

    public static AppDetails getInstance () {
        if (AppDetails.instance == null){
           AppDetails.instance = new AppDetails();
        }
        return AppDetails.instance;
    }

    public AppDetails() {
        Properties prop = new Properties();
        try {
            prop = readPropertiesFile("app.properties");
        } catch (Exception ex) {
            log.warn("Unable to read properties file", ex);
        }
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
        InputStream resourceStream = loader.getResourceAsStream(fileName);
        props.load(resourceStream);
        return props;
    }

}
