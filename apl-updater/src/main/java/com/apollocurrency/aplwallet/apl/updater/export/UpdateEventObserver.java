/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.export;

import static com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService.SECURE_STORE_KEY;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Base64;
import java.util.Objects;
import java.util.Properties;

import com.apollocurrency.aplwallet.apl.core.app.service.PropertyStorageService;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEvent;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEventData;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEventType;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * Class process sent event afer update is processed and not run script.
 * It stores some properties into config folder for later use.
 */
@Slf4j
@Singleton
public class UpdateEventObserver {

    private final PropertiesHolder propertiesHolder;
    private final OptionDAO optionDAO;
    private final PropertyStorageService propertyStorageService;

    @Inject
    public UpdateEventObserver(PropertiesHolder propertiesHolder, OptionDAO optionDAO, PropertyStorageService propertyStorageService) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is NULL");
        this.propertyStorageService = Objects.requireNonNull(propertyStorageService, "propertyStorageService is NULL");
    }

    public void onUpdateBefore(
        @Observes @UpdateEvent(UpdateEventType.BEFORE_SCRIPT) UpdateEventData updateEventData) {
        log.debug("onStartUpdateBefore...");
        // read some properties
        String nameKey = new String(Base64.getDecoder().decode(PropertyStorageService.AD_KEY_NAME));
        String propertyValue = propertiesHolder.getStringProperty(nameKey);
        log.debug("propertiesHolder value found? = '{}'", propertyValue != null && !propertyValue.isEmpty());

        // check if file has been written previously ans stores something valuable
        if (propertyStorageService.isExist()) {
            Properties properties = propertyStorageService.loadProperties();
            if(properties.containsKey(new String(Base64.getDecoder().decode("YXBsLmRwYXM=")))){
                // do not rewrite file, skip further processing
                log.warn("File is no empty, skip rewriting...");
                return;
            }
        }
        log.debug("Before saving data to file in config dir");

        Properties writeTempProps = new Properties();
        // set the properties value
        log.debug("START write props to file.");
        byte[] nameKeyHash;
        if (StringUtils.isNotBlank(propertyValue)) {
            nameKeyHash = Crypto.sha256().digest(propertyValue.getBytes());
            writeTempProps.setProperty(new String(Base64.getDecoder().decode(PropertyStorageService.PHASH_KEY_NAME)), Convert.toHexString(nameKeyHash) );
            writeTempProps.setProperty(new String(Base64.getDecoder().decode(PropertyStorageService.PVAL_KEY_NAME)), propertyValue);
        } else {
            writeTempProps.setProperty(new String(Base64.getDecoder().decode(PropertyStorageService.PVAL_KEY_NAME)),
                new String(Base64.getDecoder().decode("YWRtaW5QYXNz")) );
            nameKeyHash = Crypto.sha256().digest(Base64.getDecoder().decode("YWRtaW5QYXNz"));// default
            writeTempProps.setProperty(new String(Base64.getDecoder().decode(PropertyStorageService.PHASH_KEY_NAME)), Convert.toHexString(nameKeyHash));
        }
        String value = optionDAO.get(SECURE_STORE_KEY);
        if (StringUtils.isNotBlank(value)) {
            writeTempProps.setProperty(new String(Base64.getDecoder().decode("YXBsLmRwYXM=")), value);
        }
        // save properties to file
        boolean stored = propertyStorageService.storeProperties(writeTempProps);
        if(stored) {
            log.debug("SUCCESS props [{}] were written to file.", writeTempProps.size());
        } else {
            log.debug("FAILURED, props [{}] weren't written to file.", writeTempProps.size());
        }

        log.debug("onStartUpdateBefore DONE");
    }
}
