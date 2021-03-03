/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountPublicKeyServiceImpl implements AccountPublicKeyService {
    private final BlockChainInfoService blockChainInfoService;
    private final PublicKeyDao publicKeyDao;

    @Inject
    public AccountPublicKeyServiceImpl(BlockChainInfoService blockChainInfoService, PublicKeyDao publicKeyDao) {
        this.blockChainInfoService = blockChainInfoService;
        this.publicKeyDao = publicKeyDao;
    }

    @Override
    public int getCount() {
        return getPublicKeysCount() + getGenesisPublicKeysCount();
    }

    @Override
    public int getPublicKeysCount() {
        return publicKeyDao.count();
    }

    @Override
    public int getGenesisPublicKeysCount() {
        return publicKeyDao.genesisCount();
    }

    @Override
    public byte[] getPublicKeyByteArray(long id) {
        PublicKey publicKey = getPublicKey(id);
        if (publicKey == null || publicKey.getPublicKey() == null) {
            return null;
        }
        return publicKey.getPublicKey();
    }

    @Override
    public PublicKey getPublicKey(long accountId) {
        return publicKeyDao.searchAll(accountId);
    }


    @Override
    public PublicKey getByHeight(long id, int height) {
        return publicKeyDao.getByHeight(id, height);
    }

    @Override
    public List<PublicKey> loadPublicKeyList(int from, int to, boolean isGenesis) {
        if (isGenesis) {
            return publicKeyDao.getAllGenesis(from, to);
        } else {
            return publicKeyDao.getAll(from, to);
        }
    }

    @Override
    public EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress) {
        byte[] key = getPublicKeyByteArray(id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return EncryptedDataUtil.encryptTo(key, data, keySeed, compress);
    }

    @Override
    public byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] key = getPublicKeyByteArray(id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return EncryptedDataUtil.decryptFrom(key, encryptedData, recipientKeySeed, uncompress);
    }

    @Override
    public boolean setOrVerifyPublicKey(long accountId, byte[] key) {
        return setOrVerifyPublicKey(accountId, key, blockChainInfoService.getHeight());
    }

    //TODO setOrVerifyPublicKey ???  Split 2 on the methods
    @Override
    public boolean setOrVerifyPublicKey(long id, byte[] key, int height) {
        PublicKey publicKey = getPublicKey(id);
        if (publicKey == null) {
            publicKey = new PublicKey(id, null, height);
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            publicKey.setHeight(height);
            return true;
        }
        return Arrays.equals(publicKey.getPublicKey(), key);
    }

    @Override
    public boolean verifyPublicKey(byte[] key) {
        PublicKey publicKey = getPublicKey(AccountService.getId(key));
        if(publicKey == null || publicKey.getPublicKey() == null){
            return false;
        }
        return Arrays.equals(publicKey.getPublicKey(), key);
    }

    @Override
    public void apply(Account account, byte[] key) {
        apply(account, key, false);
    }

    @Override
    public void apply(Account account, byte[] key, boolean isGenesis) {
        PublicKey dbPublicKey = getPublicKey(account.getId());
        PublicKey publicKey = dbPublicKey;

        if (publicKey == null) {
            publicKey = new PublicKey(account.getId(), key, blockChainInfoService.getHeight());
        }
        //Such cases happens, because of air drop was on accounts id.
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            insertPublicKey(publicKey, isGenesis);
        } else if (!Arrays.equals(publicKey.getPublicKey(), key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockChainInfoService.getHeight() - 1) {
            if (dbPublicKey == null || dbPublicKey.getPublicKey() == null) {
                insertPublicKey(publicKey, isGenesis);
            }
        }
        account.setPublicKey(publicKey);
    }

    @Override
    public PublicKey insertNewPublicKey(long accountId) {
        PublicKey publicKey = new PublicKey(accountId, null, blockChainInfoService.getHeight());
        publicKeyDao.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertGenesisPublicKey(long accountId) {
        PublicKey publicKey = new PublicKey(accountId, null, blockChainInfoService.getHeight());
        publicKeyDao.insertGenesis(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis) {
        publicKey.setHeight(blockChainInfoService.getHeight());
        if (isGenesis) {
            publicKeyDao.insertGenesis(publicKey);
        } else {
            publicKeyDao.insert(publicKey);
        }
        return publicKey;
    }

    public void cleanUpPublicKeys() {
        publicKeyDao.truncate();
    }

}
