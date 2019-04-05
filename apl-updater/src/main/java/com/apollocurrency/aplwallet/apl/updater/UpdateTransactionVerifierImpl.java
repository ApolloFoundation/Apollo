/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.slf4j.Logger;

import java.util.regex.Pattern;
import javax.inject.Inject;


public class UpdateTransactionVerifierImpl implements UpdateTransactionVerifier {
        private static final Logger LOG = getLogger(UpdateTransactionVerifierImpl.class);

    public static final String VERSION_PLACEHOLDER = "$version$";
    public static final String PLATFORM_PLACEHOLDER = "$platform$";
    private static final String DEFAULT_URL_TEMPLATE = "((http)|(https))://.+/Apollo.*-" + VERSION_PLACEHOLDER + "-" + PLATFORM_PLACEHOLDER + ".jar";
    private UpdaterMediator updaterMediator;
    private UpdaterService updaterService;
    private String urlTemplate;

    public UpdateTransactionVerifierImpl(String urlTemplate, UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
        this.urlTemplate = urlTemplate;
    }

    @Inject
    public UpdateTransactionVerifierImpl(UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this(DEFAULT_URL_TEMPLATE, updaterMediator, updaterService);
    }

    @Override
    public UpdateData process(Transaction transaction) {
        if (updaterMediator.isUpdateTransaction(transaction)) {
            LOG.debug("Processing update transaction " + transaction.getId());
            return process(transaction.getAttachment(), transaction.getId());
        }
        return null;
    }

    @Override
    public UpdateData process(Attachment attachment, long transactionId) {
        if (attachment instanceof UpdateAttachment) {

            UpdateAttachment updateAttachment = (UpdateAttachment) attachment;
            if (updateAttachment.getAppVersion().greaterThan(updaterMediator.getWalletVersion())) {
                Platform currentPlatform = Platform.current();
                Architecture currentArchitecture = Architecture.current();
                if (currentPlatform != null && currentPlatform.isAppropriate(updateAttachment.getPlatform()) && updateAttachment.getArchitecture() == currentArchitecture) {
                    Pattern urlPattern = getUrlPattern(updateAttachment.getAppVersion(), updateAttachment.getPlatform());
                    DoubleByteArrayTuple encryptedUrl = updateAttachment.getUrl();
                    byte[] urlEncryptedBytes = UpdaterUtil.concatArrays(encryptedUrl.getFirst(), encryptedUrl.getSecond());
                    String url = updaterService.extractUrl(urlEncryptedBytes, urlPattern);
                    if (url != null && !url.isEmpty()) {
                        if (updaterService.verifyCertificates(UpdaterConstants.CERTIFICATE_DIRECTORY)) {
                            return new UpdateData(updateAttachment, transactionId, url);
                        } else {
                            LOG.error("Cannot verify certificates!");
                            updaterService.sendSecurityAlert("Certificate verification error" + attachment.getJSONObject().toJSONString());
                        }
                    } else {
                        LOG.error("Cannot decrypt url for update transaction {}", attachment.getJSONObject());
                        updaterService.sendSecurityAlert("Cannot decrypt url for update transaction:" + transactionId);
                    }
                }
            }
        } else {
            LOG.error("Attachment {} is not belong to update", attachment.getJSONObject().toJSONString());
        }
        return null;
    }

    private Pattern getUrlPattern(Version version, Platform platform) {
        String resultUrl = urlTemplate;
        if (resultUrl.contains(PLATFORM_PLACEHOLDER)) {
            resultUrl = resultUrl.replace(PLATFORM_PLACEHOLDER, String.valueOf(platform));
        }
        if (resultUrl.contains(VERSION_PLACEHOLDER)) {
            resultUrl = resultUrl.replace(VERSION_PLACEHOLDER, version.toString());
        }
        return Pattern.compile(resultUrl);
    }
    }
