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

//import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;

import com.apollocurrency.aplwallet.apl.core.http.API;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The class itself and methods in this class are invoked from JavaScript therefore has to be public
 */
@SuppressWarnings("WeakerAccess")
public class JavaScriptBridge {
    private static final Logger LOG = getLogger(JavaScriptBridge.class);


    DesktopApplication.MainApplication application;
    private Clipboard clipboard;

    public JavaScriptBridge(DesktopApplication.MainApplication application) {
        this.application = application;
    }

    public void log(String message) {
        LOG.info(message);
    }

    @SuppressWarnings("unused")
    public void openBrowser(String account) {
        final String url = API.getWelcomePageUri().toString() + "?account=" + account;
        Platform.runLater(() -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                LOG.info("Cannot open " + API.getWelcomePageUri().toString() + " error " + e.getMessage());
            }
        });
    }

/*    @SuppressWarnings("unused")
    public String readContactsFile() {
        String fileName = "contacts.json";
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(AplCoreRuntime.getInstance().getUserHomeDir(), fileName));
        } catch (IOException e) {
            LOG.info("Cannot read file " + fileName + " error " + e.getMessage());
            JSONObject response = new JSONObject();
            response.put("error", "contacts_file_not_found");
            response.put("file", fileName);
            response.put("folder", AplCoreRuntime.getInstance().getUserHomeDir());
            response.put("type", "1");
            return JSON.toJSONString(response);
        }
        try {
            return new String(bytes, "utf8");
        } catch (UnsupportedEncodingException e) {
            LOG.info("Cannot parse file " + fileName + " content error " + e.getMessage());
            JSONObject response = new JSONObject();
            response.put("error", "unsupported_encoding");
            response.put("type", "2");
            return JSON.toJSONString(response);
        }
    }*/
////TODO: why?
//    public String getAdminPassword() {
//        return AdminPasswordVerifier.adminPassword;
//    }


    public void downloadFile(String content, String fileName) {
        Platform.runLater(() -> {
            String home = System.getProperty("user.home");
            Path downloadDir = Paths.get(home).resolve("Downloads");
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));

            fileChooser.setInitialDirectory(downloadDir.toFile());
            fileChooser.setInitialFileName(fileName);
            File chosenFile = fileChooser.showSaveDialog(DesktopApplication.mainStage);

            if (chosenFile != null) {
                LOG.info("Save file to {}", chosenFile);
                try {
                    Files.write(chosenFile.toPath(), Base64.getDecoder().decode(content));
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "File successfully downloaded to \'" + chosenFile + "\'", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                    Platform.runLater(alert::show);
                } catch (IOException e) {
                    LOG.error("Unable to write file to " + chosenFile, e);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "I/O error occurred during saving file: " + e.getMessage(), ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    Platform.runLater(alert::show);
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void popupHandlerURLChange(String newValue) {
        application.popupHandlerURLChange(newValue);
    }

    @SuppressWarnings("unused")
    public boolean copyText(String text) {
        if (clipboard == null) {
            clipboard = Clipboard.getSystemClipboard();
            if (clipboard == null) {
                return false;
            }
        }
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        return clipboard.setContent(content);
    }

}
