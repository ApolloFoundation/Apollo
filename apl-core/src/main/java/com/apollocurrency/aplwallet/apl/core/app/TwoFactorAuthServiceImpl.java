/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Random;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepository;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;

@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {
    private static final Logger LOG = getLogger(TwoFactorAuthServiceImpl.class);
    private static final Base32 BASE_32 = new Base32();
    private static final String ISSUER_URL_TEMPLATE = "&issuer=Apollo-%s-%d";
    private static final int SECRET_LENGTH = 32;
    private static final int UPPER_BOUND_OF_RANDOM_SUFFIX_NUMBER = 1_000_000;
    private static final String DEFAULT_CHARSET = "UTF-8";

    private TwoFactorAuthRepository repository;
    private TwoFactorAuthRepository targetFileRepository;
    private final Random random;
    private final String issuerSuffix;

    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository, String issuerSuffix,
                                    TwoFactorAuthRepository targetFileRepository) {
        this(repository, issuerSuffix, new Random(), targetFileRepository);
    }

    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository, String issuerSuffix, Random random,
                                    TwoFactorAuthRepository targetFileRepository) {
        if (issuerSuffix == null || issuerSuffix.trim().isEmpty()) {
            throw new IllegalArgumentException("issuerSuffix cannot be null or empty");
        }
        this.repository = repository; // database repo
        this.random = random;
        this.issuerSuffix = issuerSuffix.trim();
        this.targetFileRepository = targetFileRepository; // file repo
    }

    @Override
//    transaction management required
    public TwoFactorAuthDetails enable(long accountId) {
//        check existing and not confirmed 2fa
//        TwoFactorAuthEntity entity = repository.get(accountId);
        TwoFactorAuthEntity entity = targetFileRepository.get(accountId); // switched to File storage
        if (entity != null) {
            if (!entity.isConfirmed()) {

            String existingBase32Secret = BASE_32.encodeToString(entity.getSecret());
            return new TwoFactorAuthDetails(getQrCodeUrl(Convert2.defaultRsAccount(entity.getAccount()), existingBase32Secret),
                    existingBase32Secret, Status2FA.OK);
            }
            return new TwoFactorAuthDetails(null, null, Status2FA.ALREADY_ENABLED);
        }
        //length of Base32Secret should be multiple 8 (length % 8 == 0); e.g. 8, 16, 24, 32, etc.

        String base32Secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(SECRET_LENGTH);
        byte[] base32Bytes = BASE_32.decode(base32Secret);
//        boolean saved = repository.add(new TwoFactorAuthEntity(accountId, base32Bytes, false));
        boolean saved = targetFileRepository.add(new TwoFactorAuthEntity(accountId, base32Bytes, false)); // switched to File storage
        if (!saved) {
            return new TwoFactorAuthDetails(null, null, Status2FA.INTERNAL_ERROR);
        }

        String qrCodeUrl = getQrCodeUrl(Convert2.defaultRsAccount(accountId), base32Secret);
        return new TwoFactorAuthDetails(qrCodeUrl, base32Secret, Status2FA.OK);
    }

    private String getQrCodeUrl(String rsAccount, String base32Secret) {
        try {
            String baseUrl = TimeBasedOneTimePasswordUtil.qrImageUrl(URLEncoder.encode(rsAccount, DEFAULT_CHARSET), base32Secret);
            String issuerUrlPart = String.format(ISSUER_URL_TEMPLATE, issuerSuffix, random.nextInt(UPPER_BOUND_OF_RANDOM_SUFFIX_NUMBER));
            return baseUrl + URLEncoder.encode(issuerUrlPart, DEFAULT_CHARSET);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Status2FA disable(long accountId, int authCode) {
//        TwoFactorAuthEntity entity = repository.get(accountId);
        TwoFactorAuthEntity entity = targetFileRepository.get(accountId); // switched to File storage
        Status2FA status2Fa = process2FAEntity(entity, true, authCode);
        if (status2Fa != Status2FA.OK) {
            return status2Fa;
        }
//        return repository.delete(accountId) ? Status2FA.OK : Status2FA.INTERNAL_ERROR;
        return targetFileRepository.delete(accountId) ? Status2FA.OK : Status2FA.INTERNAL_ERROR; // switched to File storage
    }

    @Override
    public boolean isEnabled(long accountId) {
//        TwoFactorAuthEntity entity = repository.get(accountId);
        TwoFactorAuthEntity entity = targetFileRepository.get(accountId); // switched to File storage
        return entity != null && entity.isConfirmed() && entity.getSecret() != null;
    }

    @Override
    public Status2FA tryAuth(long accountId, int authCode) {
//        TwoFactorAuthEntity entity = repository.get(accountId);
        TwoFactorAuthEntity entity = targetFileRepository.get(accountId); // switched to File storage
        return process2FAEntity(entity, true, authCode);
    }

    private boolean authEntity(TwoFactorAuthEntity entity, int authCode) {

        boolean success = false;
        try {
            if (entity != null) {
                String base32Secret = BASE_32.encodeToString(entity.getSecret());
                //window millis should be 0, other parameters will not work properly
                success = TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                        base32Secret, authCode, 0);
            }
        }
        catch (GeneralSecurityException e) {
            LOG.error("Unable to create temporal code", e);
        }
        return success;
    }

    @Override
    public Status2FA confirm(long accountId, int authCode) {
//        TwoFactorAuthEntity entity = repository.get(accountId);
        TwoFactorAuthEntity entity = targetFileRepository.get(accountId); // switched to File storage
        Status2FA analyzedStatus = process2FAEntity(entity, false, authCode);
        if (analyzedStatus != Status2FA.OK) {
            return analyzedStatus;
        }
        entity.setConfirmed(true);
//        boolean updated = repository.update(entity);
        boolean updated = targetFileRepository.update(entity); // switched to File storage
        if (!updated) {
            return Status2FA.INTERNAL_ERROR;
        }
        return Status2FA.OK;
    }

    private Status2FA process2FAEntity(TwoFactorAuthEntity entity, boolean shouldBeConfirmed, int authCode) {
        if (entity == null) {
            return Status2FA.NOT_ENABLED;
        }
        if (!shouldBeConfirmed && entity.isConfirmed()) {
            return Status2FA.ALREADY_CONFIRMED;
        }
        if (shouldBeConfirmed && !entity.isConfirmed()) {
            return Status2FA.NOT_CONFIRMED;
        }
        if (!authEntity(entity, authCode)) {
            return Status2FA.INCORRECT_CODE;
        }
        return Status2FA.OK;
    }

    @Override
    public int attemptMoveDataFromDatabase() {
        log.debug("make attempt to move data from db into file...");
        // select all possible records from db
        List<TwoFactorAuthEntity> result = repository.selectAll();
        log.debug("Db repo found 2fa records = [{}]", result.size());
        boolean isAllImportedIntoFile = true; // store and check if any error has happened on any record
        int countProcesses = 0; // store really processed records
        // loop over all db records if any
        for (TwoFactorAuthEntity itemRecord: result ) {
            log.debug("Start conversion 2fa record = {}...", itemRecord);
            if (targetFileRepository.add(itemRecord)) { // if 2fa file added
                // added new file, record stored as 2fa file
                log.debug("File Stored for 2fa record = {}...", itemRecord);
            } else {
                // 2fa file is not added by some reason
                if (targetFileRepository.get(itemRecord.getAccount()) != null) {
                    // a previous file found for 2fa record, remove db recors
                    log.debug("SKIPPING, 2fa File already stored for record = {}", itemRecord);
                } else {
                    // a previous file is NOT found for 2fa record AND it is NOT added
                    isAllImportedIntoFile = false;
                    continue; // skip for next time, do not remove db record
                }
            }
            try {
                repository.delete(itemRecord.getAccount()); // remove record from db
                countProcesses++; // increase counter when really processed
                log.debug("DONE conversion 2fa record = {}...", itemRecord);
            } catch (Exception e) {
                isAllImportedIntoFile = false;
                log.warn("Error removing 2fa record from db = {}", itemRecord, e);
            }
        }
        if (!isAllImportedIntoFile) {
            log.error("Something went wrong on export/import all 2fa records !!!");
        }
        log.info("Moved 2fa records = [{}], 2fa db records found = [{}]", countProcesses, result.size());
        return countProcesses;
    }
}
