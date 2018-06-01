/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation B.V.,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.env;

import apl.Apl;
import apl.Db;
import apl.db.FullTextTrigger;
import apl.db.TransactionalDb;
import apl.util.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Optional;

public class DesktopMode implements RuntimeMode {

    private DesktopSystemTray desktopSystemTray;
    private Class desktopApplication;

    @Override
    public void init() {
        LookAndFeel.init();
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
    }

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    @Override
    public void launchDesktopApplication() {
        Logger.logInfoMessage("Launching desktop wallet");
        try {
            desktopApplication = Class.forName("apldesktop.DesktopApplication");
            desktopApplication.getMethod("launch").invoke(null);
        } catch (ReflectiveOperationException e) {
            Logger.logInfoMessage("apldesktop.DesktopApplication failed to launch", e);
        }
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
        if (desktopApplication == null) {
            return;
        }
        try {
            desktopApplication.getMethod("shutdown").invoke(null);
        } catch (ReflectiveOperationException e) {
            Logger.logInfoMessage("apldesktop.DesktopApplication failed to shutdown", e);
        }
    }

    @Override
    public void alert(String message) {
        desktopSystemTray.alert(message);
    }

    @Override
    public void recoverDb(Path dbPath, TransactionalDb db) {
        tryToRecoverDB(dbPath, db);
    }

    public static void tryToRecoverDB(Path dbPath, TransactionalDb db) {
        new JFXPanel(); // prepare JavaFX toolkit and environment
        Platform.runLater(() -> {
            Alert alert = prepareAlert(Alert.AlertType.ERROR, "Db initialization failed", "Db was crashed! Do you want to recover db?", 180, new ButtonType("Yes", ButtonBar.ButtonData.YES), new ButtonType("No", ButtonBar.ButtonData.NO));
            Optional<ButtonType> selectedButtonType = alert.showAndWait();
            if (selectedButtonType.isPresent()) {
                if (selectedButtonType.get().getButtonData() == ButtonBar.ButtonData.YES) {
                    try {
                        Logger.logInfoMessage("Trying to reindex db...");
                        //re-index fb and return alert
                        Optional<ButtonType> clickedButtonType = reindexDb().showAndWait();
                        if (clickedButtonType.isPresent()) {
                            ButtonType clickedButton = clickedButtonType.get();
                            if (clickedButton.getText().equalsIgnoreCase("Remove db")) {
                                //delete db and show alert
                                deleteDbAndHandleException(dbPath, db).show();
                            }
                        }
                    }
                    catch (SQLException sqlEx) {
                        Logger.logErrorMessage("Cannot reindex database!", sqlEx);
                        //delete db and show alert
                        deleteDbAndHandleException(dbPath, db).show();
                    }
                }
            }
            Apl.shutdown();
        });
    }

    private static Alert reindexDb() throws SQLException {
        FullTextTrigger.reindex(Db.db.getConnection());
        return prepareAlert(Alert.AlertType.INFORMATION, "DB was re-indexed", "Db was re-indexed successfully! Please restart the wallet. Note: If wallet still failed after successful re-indexing, click on \"Remove db\" button", 180, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE), new ButtonType("Remove db", ButtonBar.ButtonData.APPLY));
    }


    private static Alert prepareAlert(Alert.AlertType alertType, String title, String contentText, int height, ButtonType... buttons) {
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

    private static void deleteDb(Path dbPath) throws IOException {
        Files.walkFileTree(dbPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void tryToDeleteDb(Path dbPath, TransactionalDb db) throws IOException {
        db.shutdown();
        Logger.logInfoMessage("Removing db...");
        deleteDb(dbPath);
        Logger.logInfoMessage("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
    }
    private static Alert deleteDbAndHandleException(Path dbPath, TransactionalDb db) {

        Alert alert;
        try {
            tryToDeleteDb(dbPath, db);
            alert = prepareAlert(Alert.AlertType.INFORMATION, "Success", "DB was removed successfully! Please, restart the wallet.", 180);
        }
        catch (IOException e) {
            Logger.logErrorMessage("Unable to delete db from path:" + dbPath.toString(), e);
            alert = prepareAlert(Alert.AlertType.ERROR, "Db was not recovered", "Cannot recover db. Try to manually delete folder:" + dbPath.toAbsolutePath().toString() + "or start app with admin rights", 180);
        }
        return alert;
    }

}
