/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.FullTextSearchDataEvent;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountInfoServiceImpl implements AccountInfoService {

    private final Blockchain blockchain;
    private final AccountInfoTable accountInfoTable;
    private Event<FullTextOperationData> fullTextOperationDataEvent;

    @Inject
    public AccountInfoServiceImpl(Blockchain blockchain,
                                  AccountInfoTable accountInfoTable,
                                  Event<FullTextOperationData> fullTextOperationDataEvent) {
        this.blockchain = blockchain;
        this.accountInfoTable = accountInfoTable;
        this.fullTextOperationDataEvent = fullTextOperationDataEvent;
    }

    @Override
    public void update(AccountInfo accountInfo) {
        Objects.requireNonNull(accountInfo);
        // prepare Event instance with data
        FullTextOperationData operationData = new FullTextOperationData(
            accountInfoTable.getTableName() + ";DB_ID;" + accountInfo.getDbId(), accountInfoTable.getTableName());
        operationData.setThread(Thread.currentThread().getName());

        if (accountInfo.getName() != null || accountInfo.getDescription() != null) {
            accountInfoTable.insert(accountInfo);
            // put relevant data into Event instance
            operationData.setOperationType(FullTextOperationData.OperationType.INSERT_UPDATE);
            operationData.addColumnData(accountInfo.getName()).addColumnData(accountInfo.getDescription());
        } else {
            accountInfoTable.deleteAtHeight(accountInfo, blockchain.getHeight());
            operationData.setOperationType(FullTextOperationData.OperationType.DELETE);
        }
        // fire event to send data into Lucene index component
        log.debug("Fire lucene index update by data = {}", operationData);
        fullTextOperationDataEvent.select(new AnnotationLiteral<FullTextSearchDataEvent>() {}).fireAsync(operationData);
    }

    @Override
    public AccountInfo getAccountInfo(Account account) {
        return accountInfoTable.get(AccountTable.newKey(account));
    }

    @Override
    public void updateAccountInfo(Account account, String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo(account);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(account.getId(), name, description, blockchain.getHeight());
        } else {
            accountInfo.setName(name);
            accountInfo.setDescription(description);
            accountInfo.setHeight(blockchain.getHeight());
        }
        update(accountInfo);
    }

    @Override
    public List<AccountInfo> searchAccounts(String query, int from, int to) {
        return toList(accountInfoTable.searchAccounts(query, from, to));
    }

}
