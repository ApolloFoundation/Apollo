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
import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;

import java.awt.*;
import java.net.URI;

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
//TODO: why?
    public String getAdminPassword() {
        return AdminPasswordVerifier.adminPassword;
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
