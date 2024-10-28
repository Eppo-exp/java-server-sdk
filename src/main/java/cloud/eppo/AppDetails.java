package cloud.eppo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppDetails {
  private static final Logger log = LoggerFactory.getLogger(AppDetails.class);
  static AppDetails instance;
  private final String version;
  private final String name;

  static AppDetails getInstance() {
    if (instance == null) {
      instance = new AppDetails();
    }
    return instance;
  }

  AppDetails() {
    Properties prop = new Properties();
    try {
      prop = readPropertiesFile("app.properties");
    } catch (Exception ex) {
      log.warn("Unable to read properties file", ex);
    }
    this.version = prop.getProperty("app.version", "3.0.0");
    this.name = prop.getProperty("app.name", "java-server-sdk");
  }

  String getVersion() {
    return this.version;
  }

  String getName() {
    return this.name;
  }

  static Properties readPropertiesFile(String fileName) throws IOException {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    Properties props = new Properties();
    InputStream resourceStream = loader.getResourceAsStream(fileName);
    props.load(resourceStream);
    return props;
  }
}
