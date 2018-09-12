/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.util.regex.Pattern;


public class UpdateTransactionVerifierImpl implements UpdateTransactionVerifier {
    private static final String VERSION_PLACEHOLDER = "$version$";
    private static final String DEFAULT_URL_TEMPLATE = "((http)|(https))://.+/Apollo.*-" + VERSION_PLACEHOLDER + ".jar";
    private UpdaterMediator updaterMediator;
    private UpdaterService updaterService;
    private String urlTemplate;

    public UpdateTransactionVerifierImpl(String urlTemplate, UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
        this.urlTemplate = urlTemplate;
    }

    public UpdateTransactionVerifierImpl(UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this(DEFAULT_URL_TEMPLATE, updaterMediator, updaterService);
    }

    @Override
    public UpdateData process(Transaction transaction) {
        if (updaterMediator.isUpdateTransaction(transaction)) {
            Logger.logDebugMessage("Processing update transaction " + transaction.getId());
            Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
            if (attachment.getAppVersion().greaterThan(updaterMediator.getWalletVersion())) {
                Platform currentPlatform = Platform.current();
                Architecture currentArchitecture = Architecture.current();
                if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                    Pattern urlPattern = urlTemplate.contains(VERSION_PLACEHOLDER) ?
                            Pattern.compile(urlTemplate.replace(VERSION_PLACEHOLDER,
                                    attachment.getAppVersion().toString())) :
                            Pattern.compile(urlTemplate);

                    DoubleByteArrayTuple encryptedUrl = attachment.getUrl();
                    byte[] urlEncryptedBytes = UpdaterUtil.concatArrays(encryptedUrl.getFirst(), encryptedUrl.getSecond());
                    String url = updaterService.extractUrl(urlEncryptedBytes, urlPattern);
                    if (url != null && !url.isEmpty()) {
                        if (updaterService.verifyCertificates(UpdaterConstants.CERTIFICATE_DIRECTORY)) {
                            return new UpdateData(transaction, url);
                        } else {
                            Logger.logErrorMessage("Cannot verify certificates!");
                            updaterService.sendSecurityAlert("Certificate verification error" + transaction.getJSONObject().toJSONString());
                        }
                    } else {
                        Logger.logErrorMessage("Cannot decrypt url for update transaction:" + transaction.getId());
                        updaterService.sendSecurityAlert("Cannot decrypt url for update transaction:" + transaction.getId());
                    }
                }
            }
        }
        return null;
    }
}
