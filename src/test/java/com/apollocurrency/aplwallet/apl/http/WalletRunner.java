package com.apollocurrency.aplwallet.apl.http;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

public class WalletRunner {

    private static final String PROPERTIES_PATH = "conf/apl.properties";
    private static final String DEFAULT_PROPERTIES_PATH = "conf/apl-default.properties";
    private Path propertiesFilePath;
    private static final Logger LOG = getLogger(WalletRunner.class);
    private Properties standartProperties;
    private Properties defaultProperties;
    private boolean isTestnet;
    private final List<String> urls;
    private TestClassLoader loader;
    private Class mainClass;

    public WalletRunner(boolean isTestnet) {
        this.isTestnet = isTestnet;
        this.loader = new TestClassLoader();
        this.standartProperties = readProperties(PROPERTIES_PATH);
        this.defaultProperties = readProperties(DEFAULT_PROPERTIES_PATH);
        this.urls = Collections.unmodifiableList(loadUrls());
    }

    private List<String> loadUrls() {
        String peerString = isTestnet ? "apl.defaultTestnetPeers" : "apl.defaultPeers";
        String port = isTestnet ? "6876" : standartProperties.getProperty("apl.apiServerPort");
        String peersValue = standartProperties.getProperty(peerString);
        String[] ips;
        if (peersValue != null) {
            ips = peersValue.split(";");
        } else {
            peersValue = defaultProperties.getProperty(peerString);
            if (peersValue != null) {
                ips = peersValue.split(";");
            } else
                throw new RuntimeException("Cannot load peers ips from " + PROPERTIES_PATH + " and from " + DEFAULT_PROPERTIES_PATH);
        }
        List<String> urls = new ArrayList<>();
        for (String ip : ips) {
            urls.add("http://" + ip + ":" + port +"/apl");
        }
        return urls;
    }

    public WalletRunner() {
        this(true);
    }

    public void run() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), Paths.get("conf/").toAbsolutePath().toFile().toURL());
        System.setProperty("apl.runtime.mode", "desktop");
        if (isTestnet) {
            propertiesFilePath = Files.createTempFile("apl","");
            Files.delete(propertiesFilePath);
            Files.copy(Paths.get(PROPERTIES_PATH), propertiesFilePath, StandardCopyOption.REPLACE_EXISTING);
            Properties aplProperties = readProperties(PROPERTIES_PATH);
            aplProperties.setProperty("apl.isTestnet", "true");
            writeProperties(aplProperties, PROPERTIES_PATH);
        }

        String[] parameters ={""};
        mainClass = loader.loadClass("com.apollocurrency.aplwallet.apl.Apl");
        Object[] param = {parameters};
        Method main = mainClass.getMethod("main", parameters.getClass());
        main.invoke(null, param);

        if (isTestnet) {
            Path changedPropertiesPath = Paths.get(PROPERTIES_PATH).toAbsolutePath();
            InputStream inputStream = Files.newInputStream(propertiesFilePath);
            OutputStream outputStream = Files.newOutputStream(changedPropertiesPath);
            byte[] buff = new byte[1024];
            int count;
            while ((count = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, count);
            }
            inputStream.close();
            outputStream.close();
            Files.deleteIfExists(propertiesFilePath);
        }
    }

    public void shutdown() {
        try {
            mainClass.getMethod("shutdown").invoke(null);
        }
        catch (Throwable e) {
            LOG.error("Shutdown error! " + e.getLocalizedMessage());
        }
    }

    private void writeProperties(Properties props, String path) {
        try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
            props.store(out, "");
        }
        catch (IOException e) {
            LOG.error("Cannot write properties", e);
        }
    }

    private Properties readProperties(String path) {
        try (
            InputStream inStream = Files.newInputStream(Paths.get(path))) {
            Properties properties = new Properties();
            properties.load(inStream);
            return properties;
        }
        catch (IOException e) {
            LOG.error("Cannot read properties", e);
        }
        return null;
    }

    public List<String> getUrls() {
        return urls;
    }
}
