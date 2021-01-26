package com.apollocurrency.aplwallet.vault.rest.converter;

import com.apollocurrency.aplwallet.api.dto.vault.UserKeyStoreDTO;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.vault.model.UserKeyStore;

public class UserKeyStoreConverter implements Converter<UserKeyStore, UserKeyStoreDTO> {

    @Override
    public UserKeyStoreDTO apply(UserKeyStore userKeyStore) {
        return new UserKeyStoreDTO(userKeyStore.getFile(), userKeyStore.getFileName());
    }
}
