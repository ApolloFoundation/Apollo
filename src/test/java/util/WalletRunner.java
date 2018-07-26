package util;

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
import java.util.*;

import static com.apollocurrency.aplwallet.apl.TestData.ADMIN_PASS;
import static org.slf4j.LoggerFactory.getLogger;

public class WalletRunner {

    private static final String PROPERTIES_PATH = "conf/apl.properties";
    private static final String DEFAULT_PROPERTIES_PATH = "conf/apl-default.properties";
    private static final Logger LOG = getLogger(WalletRunner.class);
    private final List<String> urls;
    private Path savedStandartPropertiesPath;
    private Properties standartProperties;
    private Properties defaultProperties;
    private boolean isTestnet;
    private TestClassLoader loader;
    private Class mainClass;

    public WalletRunner(boolean isTestnet) {
        this.isTestnet = isTestnet;
        this.loader = new TestClassLoader();
        this.standartProperties = readProperties(PROPERTIES_PATH);
        this.defaultProperties = readProperties(DEFAULT_PROPERTIES_PATH);
        this.urls = Collections.unmodifiableList(loadUrls());
    }

    public WalletRunner() {
        this(true);
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
            urls.add("http://" + ip + ":" + port + "/apl");
        }
        return urls;
    }

    public void run() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), Paths.get("conf/").toAbsolutePath().toFile().toURL());
        System.setProperty("apl.runtime.mode", "desktop");

        //change config for test purposes
            Map<String, String> parameters = new HashMap<>();
        if (isTestnet) {
            parameters.put("apl.isTestnet", "true");
        } else {
            parameters.put("apl.isTestnet", "false");
        }
        parameters.put("apl.adminPassword", ADMIN_PASS);
        savedStandartPropertiesPath = changeProperties(parameters);
        try {
            //run application
            String[] args = {""};
            mainClass = loader.loadClass("com.apollocurrency.aplwallet.apl.Apl");
            Object[] param = {args};
            Method main = mainClass.getMethod("main", args.getClass());
            main.invoke(null, param);
        //restore config
        } finally {
            restoreProperties(savedStandartPropertiesPath);
        }
    }

    private void restoreProperties(Path propertiesFilePath) throws IOException {
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

    private Path changeProperties(Map<String, String> newProperties) throws IOException {
        Path tempPropertiesFile = Files.createTempFile("apl", "");
        Files.delete(tempPropertiesFile);
        Files.copy(Paths.get(PROPERTIES_PATH), tempPropertiesFile, StandardCopyOption.REPLACE_EXISTING);
        Properties aplProperties = readProperties(PROPERTIES_PATH);
        newProperties.forEach((name, value) -> {
            aplProperties.setProperty(name, value);
        });
        writeProperties(aplProperties, PROPERTIES_PATH);
        return tempPropertiesFile;
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

    public void disableReloading() {
        loader.setUseCache(true);
    }

    public void enableReloading() {
        loader.setUseCache(false);
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return loader.loadClass(name);
    }
}
