package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.cert.ApolloCertificate;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import io.firstbridge.cryptolib.FBCryptoAsym;
import io.firstbridge.cryptolib.FBCryptoFactory;
import io.firstbridge.cryptolib.FBCryptoParams;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Set;

@Singleton
@Slf4j
public class UpdateV2Transaction extends Update {
    private CertificateMemoryStore store;
    private FBCryptoAsym asymCrypto = FBCryptoFactory.create(FBCryptoParams.createDefault()).getAsymCrypto();//TODO build asym crypto on certificate specific params

    @Inject
    public UpdateV2Transaction(CertificateMemoryStore store) {
        this.store = store;
    }

    @Override
    public Level getLevel() {
        throw new UnsupportedOperationException("Level is not defined for UpdateV2 statically");
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
        UpdateV2Attachment attachment = (UpdateV2Attachment) transaction.getAttachment();
        Version version = attachment.getReleaseVersion();
        if (version.getMinorVersion() > Short.MAX_VALUE || version.getIntermediateVersion() > Short.MAX_VALUE || version.getMajorVersion() > Short.MAX_VALUE) {
            throw new AplException.NotValidException("Update version is too big! " + version);
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        super.applyAttachment(transaction, senderAccount, recipientAccount);
        UpdateV2Attachment attachment = (UpdateV2Attachment) transaction.getAttachment();
        if (attachment.getUpdateLevel() == Level.CRITICAL && isOurPlatformSupplied(attachment.getPlatforms())) {
            ApolloCertificate certificate = store.getBySn(attachment.getSerialNumber());
            if (certificate != null) {
                asymCrypto.setTheirPublicKey(certificate.getPublicKey());
                if (asymCrypto.verifySignature(attachment.dataBytes(), attachment.getSignature())) {
                    // call supervisor for update
                } else {
                    logUpdateTxError("Signature verification failed", transaction, attachment);
                }
            } else {
                logUpdateTxError("No certificate found", transaction, attachment);
            }
        } else {
            logUpdateTxError("Skip update tx, our platform is not supported or level is not critical", transaction, attachment);
        }
    }

    private void logUpdateTxError(String initialMessage, Transaction tx, UpdateV2Attachment attachment) {
        log.debug(initialMessage + " for update tx: {}, attachment {}", tx.getFullHashString(), attachment);
    }

    private boolean isOurPlatformSupplied(Set<PlatformSpec> platformSpecs) {
        PlatformSpec ourPlatform = PlatformSpec.current();
        for (PlatformSpec platform : platformSpecs) {
            if (ourPlatform.isAppropriate(platform)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_UPDATE_V2;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.UPDATE_V2;
    }

    @Override
    public String getName() {
        return "UpdateV2";
    }

    @Override
    public UpdateV2Attachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new UpdateV2Attachment(buffer);
    }

    @Override
    public UpdateV2Attachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new UpdateV2Attachment(attachmentData);
    }
};
