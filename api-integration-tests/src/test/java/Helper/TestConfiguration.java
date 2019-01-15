package Helper;

import java.util.Properties;

public class TestConfiguration {
    private static TestConfiguration testConfiguration;

    private Properties properties;
    private final String propertyFilePath= "TestConfiguration.properties";
    private final String propertyTestEnvironmentKey = "TestEnvironment";
    private final String propertyPublicKey = "PublicKey";
    private final String propertySecretPhraseKey = "SecretPhrase";
    private final String propertyTestUser = "TestUser";
    private final String propertyPort= "Port";

    private TestConfiguration(){
        ConfigFileReader();
    }

    public static TestConfiguration getTestConfiguration() {
        if (testConfiguration == null){
            testConfiguration = new TestConfiguration();
        }
        return testConfiguration;
    }

    public void ConfigFileReader(){
	//Get file from resources folder
	ClassLoader classLoader = getClass().getClassLoader();
        try {
            properties = new Properties();
            properties.load(classLoader.getResourceAsStream(propertyFilePath));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Configuration.properties not found at " + propertyFilePath);
        }
    }

    public String getBaseURL() {
        return properties.getProperty(propertyTestEnvironmentKey);
    }
    public String getPublicKey() {
        return properties.getProperty(propertyPublicKey);
    }
    public String getSecretPhrase() {
        return properties.getProperty(propertySecretPhraseKey);
    }
    public String getTestUser() {
        return properties.getProperty(propertyTestUser);
    }
    public String getPort() {
        return properties.getProperty(propertyPort);
    }


}
