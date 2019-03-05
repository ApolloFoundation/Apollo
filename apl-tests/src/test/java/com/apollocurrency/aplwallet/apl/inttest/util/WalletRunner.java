/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.inttest.util;

import static com.apollocurrency.aplwallet.apl.inttest.core.TestConstants.ADMIN_PASS;
import static org.slf4j.LoggerFactory.getLogger;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.slf4j.Logger;

public class WalletRunner {

    private static final String PROPERTIES_FILENAME = "apl-blockchain.properties";
    private static final String DEFAULT_PROPERTIES_FILENAME = "apl-default.properties";
    private static final String DEFAULT_PROPERTIES_DIR = "conf";
    private static final Logger LOG = getLogger(WalletRunner.class);

    private final String workingDirectory;
    private final String propertiesDir;

    private final List<String> urls;
    private Properties standartProperties;
    private Properties defaultProperties;
    private boolean isTestnet;
    private TestClassLoader loader;
    private Class mainClass;
    private String propertiesPath;
    private String defaultPropertiesPath;

    public WalletRunner(boolean isTestnet, String workingDirectory, String propertiesDir) {
        this.isTestnet = isTestnet;
        this.loader = new TestClassLoader();
        this.workingDirectory = Convert.nullToEmpty(workingDirectory);
        this.propertiesDir = Convert.nullToEmpty(propertiesDir);

        this.propertiesPath = Paths.get(this.workingDirectory, this.propertiesDir, PROPERTIES_FILENAME).toString();

        this.standartProperties = readProperties(propertiesPath);
        this.defaultPropertiesPath = Paths.get(this.workingDirectory, this.propertiesDir, DEFAULT_PROPERTIES_FILENAME).toString();
        this.defaultProperties =
                readProperties(defaultPropertiesPath);
        this.urls = Collections.unmodifiableList(loadUrls());
    }

    public WalletRunner(boolean isTestnet) {
        this(isTestnet, Convert.nullToEmpty(System.getProperty("walletWorkingDir")), DEFAULT_PROPERTIES_DIR);
    }
    public WalletRunner() {
        this(true, Convert.nullToEmpty(System.getProperty("walletWorkingDir")), DEFAULT_PROPERTIES_DIR);
    }

    public WalletRunner(boolean isTestnet, String workingDirectory) {
        this(isTestnet, workingDirectory, DEFAULT_PROPERTIES_DIR);
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
                throw new RuntimeException("Cannot load peers ips from " + propertiesPath + " and from " + defaultPropertiesPath);
        }
        List<String> urls = new ArrayList<>();
        for (String ip : ips) {
            urls.add("http://" + ip + ":" + port + "/apl");
        }
        return urls;
    }

    public void run() throws IOException {

        try {
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
            parameters.put("apl.allowUpdates", String.valueOf(false));
            Path savedStandartPropertiesPath = changeProperties(parameters);
            try {
                //run application
                String[] args = {""};
                mainClass = loader.loadClass("com.apollocurrency.aplwallet.apl.Apl");
                Object[] param = {args};
                Method main = mainClass.getMethod("main", args.getClass());
                main.invoke(null, param);
                //restore config
            }

            finally {
                restoreProperties(savedStandartPropertiesPath);
            }
        }
        catch (NoSuchMethodException | InvocationTargetException | ClassNotFoundException | IllegalAccessException e) {
                LOG.error("Cannot start wallet", e);
            }
    }

    private void restoreProperties(Path propertiesFilePath) throws IOException {
        Path changedPropertiesPath = Paths.get(propertiesPath).toAbsolutePath();
        try (
        InputStream inputStream = Files.newInputStream(propertiesFilePath);
        OutputStream outputStream = Files.newOutputStream(changedPropertiesPath)) {

            byte[] buff = new byte[1024];
            int count;
            while ((count = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, count);
            }
        }
        Files.deleteIfExists(propertiesFilePath);
    }

    private Path changeProperties(Map<String, String> newProperties) throws IOException {
        Path tempPropertiesFile = Files.createTempFile("apl", "");
        Files.copy(Paths.get(propertiesPath), tempPropertiesFile, StandardCopyOption.REPLACE_EXISTING);
        Properties aplProperties = readProperties(propertiesPath);
        newProperties.forEach((name, value) -> {
            aplProperties.setProperty(name, value);
        });
        writeProperties(aplProperties, propertiesPath);
        return tempPropertiesFile;
    }

    public void shutdown() {
        try {
            Method removeShutdownHook = mainClass.getDeclaredMethod("removeShutdownHook");
            removeShutdownHook.setAccessible(true);
            removeShutdownHook.invoke(null);

            mainClass.getDeclaredMethod("shutdown").invoke(null);
            // make possible to run again
            this.loader = new TestClassLoader();
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
