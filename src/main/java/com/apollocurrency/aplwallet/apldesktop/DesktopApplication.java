/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apldesktop;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.http.API;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.TrustAllSSLProvider;
import com.sun.javafx.scene.web.Debugger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apldesktop.DesktopApplication.MainApplication.showStage;
import static org.slf4j.LoggerFactory.getLogger;

public class DesktopApplication extends Application {
        private static final Logger LOG = getLogger(DesktopApplication.class);
        
    private static final MainApplication MAIN_APPLICATION = MainApplication.getInstance();
    private static final SplashScreen SPLASH_SCREEN = SplashScreen.getInstance();
    private static final DbRecoveringUI DB_RECOVERING_UI = DbRecoveringUI.getInstance();
    private static final boolean ENABLE_JAVASCRIPT_DEBUGGER = false;
    private static volatile boolean isLaunched;
    private static volatile boolean isSplashScreenLaunched = false;
    private static volatile Stage mainStage;
    private static volatile Stage screenStage;

    public static void refreshMainApplication() {
        MainApplication.refresh();
    }

    private static String getUrl() {
        String url = API.getWelcomePageUri().toString();
        if (url.startsWith("https")) {
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        String defaultAccount = Apl.getStringProperty("apl.defaultDesktopAccount");
        if (defaultAccount != null && !defaultAccount.equals("")) {
            url += "?account=" + defaultAccount;
        }
        return url;
    }

    public static void shutdownSplashScreen() {
        SPLASH_SCREEN.shutdown();
        isSplashScreenLaunched = false;
    }

    public static void updateSplashScreenStatus(String newStatus) {
         SPLASH_SCREEN.setLastStatus(newStatus);
    }

    //rewrite (start on existing stage)
    public static void launch() {
        if (!isLaunched) {
            isLaunched = true;
            Application.launch(DesktopApplication.class);
            return;
        }
        if (mainStage != null) {
            Platform.runLater(() -> showStage(false));
        }
    }

    public static void recoverDbUI() {
        DB_RECOVERING_UI.tryToRecoverDB();
    }

    public static void startDesktopApplication() {
        if (isSplashScreenLaunched) {
            shutdownSplashScreen();
        }
        Platform.runLater(MAIN_APPLICATION::startDesktopApplication);
    }

    //start javaFx splash screen
    public static void showSplashScreen() {
        if (!isSplashScreenLaunched) {
            LOG.info("Starting splash screen");
            SPLASH_SCREEN.startAndShow();
            isSplashScreenLaunched = true;
        } else {
            LOG.info("Splash screen has already started");
        }
    }

    @SuppressWarnings("unused")
    public static void shutdown() {
        System.out.println("shutting down JavaFX platform");
        Platform.runLater(()-> {
            if (screenStage.isShowing()) {
                screenStage.close();
            }
            if (mainStage.isShowing()) {
                mainStage.close();
            }
        });
        Platform.exit();
        if (ENABLE_JAVASCRIPT_DEBUGGER) {
            try {
                Class<?> aClass = Class.forName("com.mohamnag.fxwebview_debugger.DevToolsDebuggerServer");
                aClass.getMethod("stopDebugServer").invoke(null);
            }
            catch (Exception e) {
                LOG.info("Error shutting down webview debugger", e);
            }
        }
        System.out.println("JavaFX platform shutdown complete");
    }

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        screenStage = new Stage();
        showSplashScreen();
    }

    public static class SplashScreen {
        private static SplashScreen instance = new SplashScreen();
        private AtomicBoolean shutdown = new AtomicBoolean(false);
        private volatile String lastStatus;

        private SplashScreen() {}

        public static SplashScreen getInstance() {
            return instance;
        }

        public String getLastStatus() {
            return lastStatus;
        }

        public void setLastStatus(String lastStatus) {
            this.lastStatus = lastStatus;
        }

        public void startAndShow() {
            screenStage.setTitle("Apollo wallet");
            AnchorPane pane = new AnchorPane();
            Scene scene = new Scene(pane);
            URL cssUrl = getClass().getClassLoader().getResource("css/splash-screen.css");
            pane.getStylesheets().addAll(cssUrl.toExternalForm());
            pane.setId("main-pane");
            ProgressIndicator indicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
            indicator.setId("progress-indicator");
            AnchorPane.setTopAnchor(indicator, 130.0);
            AnchorPane.setLeftAnchor(indicator, 175.0);
            pane.getChildren().add(indicator);
            Text statusText = new Text();
            statusText.setId("status-text");
            statusText.setText("Apollo wallet is loading. Please, wait");
            AnchorPane.setTopAnchor(statusText, 228.0);
            AnchorPane.setLeftAnchor(statusText, 60.0);
            pane.getChildren().add(statusText);
            screenStage.setScene(scene);
            screenStage.setHeight(250);
            screenStage.setWidth(400);
            screenStage.initStyle(StageStyle.UNDECORATED);
            screenStage.show();
            Runnable statusUpdater = () -> {
                while (!shutdown.get()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                        Platform.runLater(() -> {
                            String lastStatus = getLastStatus();
                            if (lastStatus != null && !statusText.getText().equalsIgnoreCase(lastStatus)) {
                                statusText.setText(lastStatus);
                            }
                        });
                    }
                    catch (InterruptedException e) {
                        LOG.info("GUI thread was interrupted", e);
                    }
                }
                LOG.info("Shutdown splash screen");
                Platform.runLater(() -> screenStage.hide());
                shutdown.set(false);
            };
            Thread updateSplashScreenThread = new Thread(statusUpdater, "Update splash screen status thread");
            updateSplashScreenThread.setDaemon(true);
            updateSplashScreenThread.start();
        }

        public void shutdown() {
            shutdown.set(true);
        }
    }

    public static class MainApplication {
        private static final Set DOWNLOAD_REQUEST_TYPES = new HashSet<>(Arrays.asList("downloadTaggedData", "downloadPrunableMessage"));
        private static volatile WebEngine webEngine;
        private static MainApplication instance = new MainApplication();
        private JSObject nrs;
        private volatile long updateTime;
        private volatile List<Transaction> unconfirmedTransactionUpdates = new ArrayList<>();
        private JavaScriptBridge javaScriptBridge;

        private MainApplication() {}

        public static void refresh() {
            Platform.runLater(() -> showStage(true));
        }

        static void showStage(boolean isRefresh) {
            if (isRefresh) {
                webEngine.load(getUrl());
            }
            if (!mainStage.isShowing()) {
                mainStage.show();
            } else if (mainStage.isIconified()) {
                mainStage.setIconified(false);
            } else {
                mainStage.toFront();
            }
        }

        public static MainApplication getInstance() {
            return instance;
        }

        public void startDesktopApplication() {
            mainStage = new Stage();
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            WebView browser = new WebView();
            browser.setOnContextMenuRequested(new WalletContextMenu());
            WebView invisible = new WebView();

            int height = (int) Math.min(primaryScreenBounds.getMaxY() - 100, 1000);
            int width = (int) Math.min(primaryScreenBounds.getMaxX() - 100, 1618);
            browser.setMinHeight(height);
            browser.setMinWidth(width);
            webEngine = browser.getEngine();
            webEngine.setUserDataDirectory(Apl.getConfDir());

            Worker<Void> loadWorker = webEngine.getLoadWorker();
            loadWorker.stateProperty().addListener(
                    (ov, oldState, newState) -> {
                        LOG.debug("loadWorker old state " + oldState + " new state " + newState);
                        if (newState != Worker.State.SUCCEEDED) {
                            LOG.debug("loadWorker state change ignored");
                            return;
                        }
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        javaScriptBridge = new JavaScriptBridge(this); // Must be a member variable to prevent gc
                        window.setMember("java", javaScriptBridge);
                        Locale locale = Locale.getDefault();
                        String language = locale.getLanguage().toLowerCase() + "-" + locale.getCountry().toUpperCase();
                        window.setMember("javaFxLanguage", language);
                        webEngine.executeScript("console.log = function(msg) { java.log(msg); };");
                        mainStage.setTitle(Constants.PROJECT_NAME + " Desktop - " + webEngine.getLocation());
                        nrs = (JSObject) webEngine.executeScript("NRS");
                        updateClientState("Desktop Wallet started");
                        BlockchainProcessor blockchainProcessor = Apl.getBlockchainProcessor();
                        blockchainProcessor.addListener((block) ->
                                updateClientState(BlockchainProcessor.Event.BLOCK_PUSHED, block), BlockchainProcessor.Event.BLOCK_PUSHED);
                        blockchainProcessor.addListener((block) ->
                                updateClientState(BlockchainProcessor.Event.AFTER_BLOCK_APPLY, block), BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
                        Apl.getTransactionProcessor().addListener(transaction ->
                                updateClientState(TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS, transaction), TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

                        if (ENABLE_JAVASCRIPT_DEBUGGER) {
                            try {
                                // Add the javafx_webview_debugger lib to the classpath
                                // For more details, check https://github.com/mohamnag/javafx_webview_debugger
                                Class<?> aClass = Class.forName("com.mohamnag.fxwebview_debugger.DevToolsDebuggerServer");
                                @SuppressWarnings("deprecation") Debugger debugger = webEngine.impl_getDebugger();
                                Method startDebugServer = aClass.getMethod("startDebugServer", Debugger.class, int.class);
                                startDebugServer.invoke(null, debugger, 51742);
                            }
                            catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                LOG.info("Cannot start JavaFx debugger", e);
                            }
                        }
                    });

            // Invoked by the webEngine popup handler
            // The invisible webView does not show the link, instead it opens a browser window
            invisible.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> popupHandlerURLChange(newValue));

            // Invoked when changing the document.location property, when issuing a download request
            webEngine.locationProperty().addListener((observable, oldValue, newValue) -> webViewURLChange(newValue));

            // Invoked when clicking a link to external site like Help or API console
            webEngine.setCreatePopupHandler(
                    config -> {
                        LOG.info("popup request from webEngine");
                        return invisible.getEngine();
                    });

            webEngine.load(getUrl());

            Scene scene = new Scene(browser);
            String address = API.getServerRootUri().toString();
            mainStage.getIcons().add(new Image(address + "/img/apl-icon-32x32.png"));
            mainStage.initStyle(StageStyle.DECORATED);
            mainStage.setScene(scene);
            mainStage.sizeToScene();
            mainStage.show();
            Platform.setImplicitExit(false); // So that we can reopen the application in case the user closed it
        }

        private void updateClientState(BlockchainProcessor.Event blockEvent, Block block) {
            BlockchainProcessor blockchainProcessor = Apl.getBlockchainProcessor();
            if (blockEvent == BlockchainProcessor.Event.BLOCK_PUSHED && blockchainProcessor.isDownloading()) {
                if (!(block.getHeight() % 100 == 0)) {
                    return;
                }
            }
            if (blockEvent == BlockchainProcessor.Event.AFTER_BLOCK_APPLY) {
                if (blockchainProcessor.isScanning()) {
                    if (!(block.getHeight() % 100 == 0)) {
                        return;
                    }
                } else {
                    return;
                }
            }
            String msg = blockEvent.toString() + " id " + block.getStringId() + " height " + block.getHeight();
            updateClientState(msg);
        }

        private void updateClientState(TransactionProcessor.Event transactionEvent, List<? extends Transaction> transactions) {
            if (transactions.size() == 0) {
                return;
            }
            unconfirmedTransactionUpdates.addAll(transactions);
            if (System.currentTimeMillis() - updateTime > 3000L) {
                String msg = transactionEvent.toString() + " ids " + unconfirmedTransactionUpdates.stream().map(Transaction::getStringId).collect(Collectors.joining(","));
                updateTime = System.currentTimeMillis();
                unconfirmedTransactionUpdates = new ArrayList<>();
                updateClientState(msg);
            }
        }

        private void updateClientState(String msg) {
            Platform.runLater(() -> webEngine.executeScript("NRS.getState(null, '" + msg + "')"));
        }

        @SuppressWarnings("WeakerAccess")
        public void popupHandlerURLChange(String newValue) {
            LOG.info("popup request for " + newValue);
            Platform.runLater(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(newValue));
                }
                catch (Exception e) {
                    LOG.info("Cannot open " + newValue + " error " + e.getMessage());
                }
            });
        }

        private void webViewURLChange(String newValue) {
            LOG.info("webview address changed to " + newValue);
            URL url;
            try {
                url = new URL(newValue);
            }
            catch (MalformedURLException e) {
                LOG.info("Malformed URL " + newValue, e);
                return;
            }
            String query = url.getQuery();
            if (query == null) {
                return;
            }
            String[] paramPairs = query.split("&");
            Map<String, String> params = new HashMap<>();
            for (String paramPair : paramPairs) {
                String[] keyValuePair = paramPair.split("=");
                if (keyValuePair.length == 2) {
                    params.put(keyValuePair[0], keyValuePair[1]);
                }
            }
            String requestType = params.get("requestType");
            if (DOWNLOAD_REQUEST_TYPES.contains(requestType)) {
                download(requestType, params);
            } else {
                LOG.info(String.format("requestType %s is not a download request", requestType));
            }
        }

        private void download(String requestType, Map<String, String> params) {
            long transactionId = Convert.parseUnsignedLong(params.get("transaction"));
            TaggedData taggedData = TaggedData.getData(transactionId);
            boolean retrieve = "true".equals(params.get("retrieve"));
            if (requestType.equals("downloadTaggedData")) {
                if (taggedData == null && retrieve) {
                    try {
                        if (Apl.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                            growl("Pruned transaction data not currently available from any peer");
                            return;
                        }
                    }
                    catch (IllegalArgumentException e) {
                        growl("Pruned transaction data cannot be restored using desktop wallet without full blockchain. Use Web Wallet instead");
                        return;
                    }
                    taggedData = TaggedData.getData(transactionId);
                }
                if (taggedData == null) {
                    growl("Tagged data not found");
                    return;
                }
                byte[] data = taggedData.getData();
                String filename = taggedData.getFilename();
                if (filename == null || filename.trim().isEmpty()) {
                    filename = taggedData.getName().trim();
                }
                downloadFile(data, filename);
            } else if (requestType.equals("downloadPrunableMessage")) {
                PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
                if (prunableMessage == null && retrieve) {
                    try {
                        if (Apl.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                            growl("Pruned message not currently available from any peer");
                            return;
                        }
                    }
                    catch (IllegalArgumentException e) {
                        growl("Pruned message cannot be restored using desktop wallet without full blockchain. Use Web Wallet instead");
                        return;
                    }
                    prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
                }
                String secretPhrase = params.get("secretPhrase");
                byte[] sharedKey = Convert.parseHexString(params.get("sharedKey"));
                if (sharedKey == null) {
                    sharedKey = Convert.EMPTY_BYTE;
                }
                if (sharedKey.length != 0 && secretPhrase != null) {
                    growl("Do not specify both secret phrase and shared key");
                    return;
                }
                byte[] data = null;
                if (prunableMessage != null) {
                    try {
                        if (secretPhrase != null) {
                            data = prunableMessage.decrypt(secretPhrase);
                        } else if (sharedKey.length > 0) {
                            data = prunableMessage.decrypt(sharedKey);
                        } else {
                            data = prunableMessage.getMessage();
                        }
                    }
                    catch (RuntimeException e) {
                        LOG.debug("Decryption of message to recipient failed: " + e.toString());
                        growl("Wrong secretPhrase or sharedKey");
                        return;
                    }
                }
                if (data == null) {
                    data = Convert.EMPTY_BYTE;
                }
                downloadFile(data, "" + transactionId);
            }
        }

        private void downloadFile(byte[] data, String filename) {
            Path folderPath = Paths.get(System.getProperty("user.home"), "downloads");
            Path path = Paths.get(folderPath.toString(), filename);
            LOG.info("Downloading data to " + path.toAbsolutePath());
            try {
                OutputStream outputStream = Files.newOutputStream(path);
                outputStream.write(data);
                outputStream.close();
                growl(String.format("File %s saved to folder %s", filename, folderPath));
            }
            catch (IOException e) {
                growl("Download failed " + e.getMessage(), e);
            }
        }

        public void stop() {
            System.out.println("DesktopApplication stopped"); // Should never happen
        }

        private void growl(String msg) {
            growl(msg, null);
        }

        private void growl(String msg, Exception e) {
            if (e == null) {
                LOG.info(msg);
            } else {
                LOG.info(msg, e);
            }
            nrs.call("growl", msg);
        }


    }

    private static class DbRecoveringUI {
        private static DbRecoveringUI instance = new DbRecoveringUI();

        private DbRecoveringUI() {}

        public static DbRecoveringUI getInstance() {
            return instance;
        }

        public void tryToRecoverDB() {
            new JFXPanel(); // prepare JavaFX toolkit and environment
            Platform.runLater(() -> {
                Alert alert = prepareAlert(Alert.AlertType.ERROR, "Db initialization failed", "Db was crashed! Do you want to recover db?", 180, new ButtonType("Yes", ButtonBar.ButtonData.YES), new ButtonType("No", ButtonBar.ButtonData.NO));
                Optional<ButtonType> selectedButtonType = alert.showAndWait();
                if (selectedButtonType.isPresent()) {
                    if (selectedButtonType.get().getButtonData() == ButtonBar.ButtonData.YES) {
                        try {
                            LOG.info("Trying to reindex db...");
                            //re-index db and return alert
                            Optional<ButtonType> clickedButtonType = reindexDbUI().showAndWait();
                            if (clickedButtonType.isPresent()) {
                                ButtonType clickedButton = clickedButtonType.get();
                                if (clickedButton.getText().equalsIgnoreCase("Remove db")) {
                                    //delete db and show alert
                                    deleteDbAndHandleException().show();
                                }
                            }
                        }
                        catch (SQLException sqlEx) {
                            LOG.error("Cannot reindex database!", sqlEx);
                            //delete db and show alert
                            deleteDbAndHandleException().show();
                        }
                    }
                }
                System.exit(0);
            });
        }

        private Alert reindexDbUI() throws SQLException {
            FullTextTrigger.reindex(Db.db.getConnection());
            return prepareAlert(Alert.AlertType.INFORMATION, "DB was re-indexed", "Db was re-indexed successfully! Please restart the wallet. Note: If wallet still failed after successful re-indexing, click on \"Remove db\" button", 180, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE), new ButtonType("Remove db", ButtonBar.ButtonData.APPLY));
        }


        private Alert prepareAlert(Alert.AlertType alertType, String title, String contentText, int height, ButtonType... buttons) {
            Alert alert = new Alert(alertType, contentText);
            alert.getDialogPane().setMinHeight(height); // resizing
            alert.setTitle(title);
            if (buttons != null && buttons.length != 0) {
                alert.getButtonTypes().clear();
                for (ButtonType button : buttons) {
                    alert.getButtonTypes().add(button);
                }
            }
            return alert;
        }

        private Alert deleteDbAndHandleException() {

            Alert alert;
            try {
                Db.tryToDeleteDb();
                alert = prepareAlert(Alert.AlertType.INFORMATION, "Success", "DB was removed successfully! Please, restart the wallet.", 180);
            }
            catch (IOException e) {
                LOG.error("Unable to delete db!", e);
                alert = prepareAlert(Alert.AlertType.ERROR, "Db was not recovered", "Cannot recover db. Try to manually delete db folder.", 180);
            }
            return alert;
        }
    }
}
