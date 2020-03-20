/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.container.DataRecord;
import io.firstbridge.cryptolib.container.FbWallet;
import io.firstbridge.cryptolib.container.KeyRecord;
import io.firstbridge.cryptolib.container.KeyTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SecureStorage extends FbWallet {
    private static Logger LOG = LoggerFactory.getLogger(SecureStorage.class);

    public static final String DEX_PRIVATE_KEYS = "dex_keys";
    private static final ObjectMapper JSON_MAPPER = JSON.getMapper();

    public Map<Long, String> getDexKeys(){
        String json = this.getAllData().stream()
                .filter(dataRecord -> Objects.equals(dataRecord.alias, DEX_PRIVATE_KEYS))
                .map(dataRecord -> dataRecord.data)
                .findFirst().orElse(null);

        Map<Long, String> store = new HashMap<>();
        try {
            store.putAll(JSON_MAPPER.readValue(json, new TypeReference<Map<Long,String>>(){}));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return store;
    }

    public void addDexKeys(Map<Long, String> userKeys) throws AplException.ExecutiveProcessException {
        DataRecord dr = new DataRecord();
        dr.alias = DEX_PRIVATE_KEYS;
        try {
            dr.data = JSON_MAPPER.writeValueAsString(userKeys);
        } catch (JsonProcessingException e) {
            throw new AplException.ExecutiveProcessException("adding dex secret keys is failed, JsonProcessingException");
        }
        dr.encoding = "HEX";

        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.OTHER;
        kr.publicKey = null;
        this.addData(dr);
        this.addKey(kr);
    }

    public boolean store(String privateKey, String storagePath){
        byte[] salt = generateBytes(12);
        try {
            byte[] key = keyFromPassPhrase(privateKey, salt);
            saveFile(storagePath, key, salt);
        } catch (IOException | CryptoNotValidException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public static SecureStorage get(String privateKey, String storagePath){
        Objects.requireNonNull(privateKey);
        SecureStorage fbWallet = new SecureStorage();
        try {
            fbWallet.readOpenData(storagePath);
            byte[] salt = fbWallet.getContanerIV();
            byte[] key = fbWallet.keyFromPassPhrase(privateKey, salt);
            fbWallet.openFile(storagePath, key);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } catch (CryptoNotValidException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

        return fbWallet;
    }

    private byte[] generateBytes(int size) {
        byte[] nonce = new byte[size];
        Crypto.getSecureRandom().nextBytes(nonce);
        return nonce;
    }


}
