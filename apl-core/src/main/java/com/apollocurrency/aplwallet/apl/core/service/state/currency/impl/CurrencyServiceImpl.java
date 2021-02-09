/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyFounderService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.MonetaryCurrencyMintingService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TransactionValidationHelper;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType.CLAIMABLE;
import static com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType.MINTABLE;
import static com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType.RESERVABLE;
import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;

@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Slf4j
@Singleton
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencySupplyTable currencySupplyTable;
    private final CurrencyTable currencyTable;
    private final CurrencyMintTable currencyMintTable;
    private final MonetaryCurrencyMintingService monetaryCurrencyMintingService;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<Currency> iteratorToStreamConverter;
    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    private final CurrencyFounderService currencyFounderService;
    private final ExchangeService exchangeService;
    private final CurrencyTransferService currencyTransferService;
    private final ShufflingService shufflingService;
    private final BlockchainConfig blockchainConfig;
    private final TransactionValidationHelper validationHelper;
    private final FullTextSearchUpdater fullTextSearchUpdater;
    private final FullTextSearchService fullTextSearchService;

    @Inject
    public CurrencyServiceImpl(CurrencySupplyTable currencySupplyTable,
                               CurrencyTable currencyTable,
                               CurrencyMintTable currencyMintTable,
                               MonetaryCurrencyMintingService monetaryCurrencyMintingService,
                               BlockChainInfoService blockChainInfoService,
                               AccountService accountService,
                               AccountCurrencyService accountCurrencyService,
                               CurrencyExchangeOfferFacade currencyExchangeOfferFacade,
                               CurrencyFounderService currencyFounderService,
                               ExchangeService exchangeService,
                               CurrencyTransferService currencyTransferService,
                               ShufflingService shufflingService,
                               BlockchainConfig blockchainConfig,
                               TransactionValidationHelper transactionValidationHelper,
                               FullTextSearchUpdater fullTextSearchUpdater,
                               FullTextSearchService fullTextSearchService
    ) {
        this.currencySupplyTable = currencySupplyTable;
        this.currencyTable = currencyTable;
        this.currencyMintTable = currencyMintTable;
        this.monetaryCurrencyMintingService = monetaryCurrencyMintingService;
        this.blockChainInfoService = blockChainInfoService;
        this.validationHelper = transactionValidationHelper;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
        this.currencyExchangeOfferFacade = currencyExchangeOfferFacade;
        this.currencyFounderService = currencyFounderService;
        this.exchangeService = exchangeService;
        this.currencyTransferService = currencyTransferService;
        this.shufflingService = shufflingService;
        this.blockchainConfig = blockchainConfig;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Override
    public DbIterator<Currency> getAllCurrencies(int from, int to) {
        return currencyTable.getAll(from, to);
    }

    @Override
    public int getCount() {
        return currencyTable.getCount();
    }

    @Override
    public Currency getCurrency(long id) {
        return currencyTable.get(CurrencyTable.currencyDbKeyFactory.newKey(id));
    }

    @Override
    public Currency getCurrencyByName(String name) {
        return currencyTable.getBy(new DbClause.StringClause("name_lower", name.toLowerCase()));
    }

    @Override
    public Currency getCurrencyByCode(String code) {
        return currencyTable.getBy(new DbClause.StringClause("code", code.toUpperCase()));
    }

    @Override
    public DbIterator<Currency> getCurrencyIssuedBy(long accountId, int from, int to) {
        return currencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Stream<Currency> getCurrencyIssuedByAsStream(long accountId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to));
    }

    @Override
    public DbIterator<Currency> searchCurrencies(String query, int from, int to) {
//        return currencyTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, currency.creation_height DESC ");
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return DbIterator.EmptyDbIterator();
        }
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        String sort = " ORDER BY currency.creation_height DESC ";
        return fetchCurrencyByParams(from, to, inRangeClause, dbClause, sort);
    }

    @Override
    public Stream<Currency> searchCurrenciesStream(String query, int from, int to) {
        return iteratorToStreamConverter.apply(
            this.searchCurrencies(query, from, to)
        );
    }

    @Override
    public DbIterator<Currency> getIssuedCurrenciesByHeight(int height, int from, int to) {
        return currencyTable.getManyBy(new DbClause.IntClause("issuance_height", height), from, to);
    }

    @Override
    public Stream<Currency> getIssuedCurrenciesByHeightStream(int height, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyTable.getManyBy(new DbClause.IntClause("issuance_height", height), from, to)
        );
    }

    @Override
    public void addCurrency(LedgerEvent event, long eventId, Transaction transaction, Account senderAccount,
                            MonetarySystemCurrencyIssuance attachment) {
        Currency oldCurrency;
        if ((oldCurrency = this.getCurrencyByCode(attachment.getCode())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByCode(attachment.getName())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByName(attachment.getName())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByName(attachment.getCode())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        int height = blockChainInfoService.getHeight();
        Currency currency = new Currency(transaction, attachment, height);
        currencyTable.insert(currency);
        if (currency.is(MINTABLE) || currency.is(RESERVABLE)) {
            CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
            if (currencySupply != null) {
                currencySupply.setCurrentSupply( attachment.getInitialSupply() );
                currencySupply.setHeight(height);
                currencySupplyTable.insert(currencySupply);
            }
        }
        createAndFireFullTextSearchDataEvent(currency, FullTextOperationData.OperationType.INSERT_UPDATE);
    }

    @Override
    public void increaseReserve(LedgerEvent event, long eventId, Account account, long currencyId, long amountPerUnitATM) {
        Currency currency = this.getCurrency(currencyId);
        accountService.addToBalanceATM(account, event, eventId, -Math.multiplyExact(currency.getReserveSupply(), amountPerUnitATM));
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        if (currencySupply != null) {
            long tempAmountPerUnitATM = currencySupply.getCurrentReservePerUnitATM() + amountPerUnitATM;
            currencySupply.setCurrentReservePerUnitATM(tempAmountPerUnitATM);
            currencySupply.setHeight(blockChainInfoService.getHeight());
            currencySupplyTable.insert(currencySupply);
        }
        currencyFounderService.addOrUpdateFounder(currencyId, account.getId(), amountPerUnitATM);
    }

    @Override
    public void claimReserve(LedgerEvent event, long eventId, Account account, long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(account, event, eventId, currencyId, -units);
        Currency currency = this.getCurrency(currencyId);
        this.increaseSupply(currency, -units);
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId,
            Math.multiplyExact(units, this.getCurrentReservePerUnitATM(currency)));
    }

    @Override
    public void transferCurrency(LedgerEvent event, long eventId, Account senderAccount, Account recipientAccount,
                                 long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(senderAccount, event, eventId, currencyId, -units);
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(recipientAccount, event, eventId, currencyId, units);
    }

    public long getCurrentSupply(Currency currency) {
        if (!currency.is(RESERVABLE) && !currency.is(MINTABLE)) {
            return currency.getInitialSupply();
        }
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        if (currencySupply == null) {
            return 0;
        }
        return currencySupply.getCurrentSupply();
    }

    @Override
    public long getCurrentReservePerUnitATM(Currency currency) {
        if (!currency.is(RESERVABLE) || this.loadCurrencySupplyByCurrency(currency) == null) {
            return 0;
        }
        return currency.getCurrencySupply().getCurrentReservePerUnitATM();
    }

    @Override
    public boolean isActive(Currency currency) {
        return currency.getIssuanceHeight() <= blockChainInfoService.getHeight();
    }

    @Override
    public CurrencySupply loadCurrencySupplyByCurrency(Currency currency) {
        if (!currency.is(RESERVABLE) && !currency.is(MINTABLE)) {
            return null;
        }
        CurrencySupply currencySupply = currency.getCurrencySupply();
        if (currencySupply == null) {
            currencySupply = currencySupplyTable.get(currencyTable.getDbKeyFactory().newKey(currency));
            if (currencySupply == null) {
                currencySupply = new CurrencySupply(currency, blockChainInfoService.getHeight());
            }
            currency.setCurrencySupply(currencySupply);
        }
        return currencySupply;
    }

    @Override
    public void increaseSupply(Currency currency, long units) {
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        long tempCurrentSupply = currencySupply.getCurrentSupply() + units;
        currencySupply.setCurrentSupply(tempCurrentSupply);
        if (currencySupply.getCurrentSupply() > currency.getMaxSupply() || currencySupply.getCurrentSupply() < 0) {
            tempCurrentSupply = currencySupply.getCurrentSupply() - units;
            currencySupply.setCurrentSupply(tempCurrentSupply);
            throw new IllegalArgumentException("Cannot add " + units + " to current supply of " + currencySupply.getCurrentSupply());
        }
        currencySupply.setHeight(blockChainInfoService.getHeight());
        currencySupplyTable.insert(currencySupply);
    }

    @Override
    public DbIterator<Exchange> getExchanges(long currencyId, int from, int to) {
        return exchangeService.getCurrencyExchanges(currencyId, from, to);
    }

    @Override
    public DbIterator<CurrencyTransfer> getTransfers(long currencyId, int from, int to) {
        return currencyTransferService.getCurrencyTransfers(currencyId, from, to);
    }

    @Override
    public boolean canBeDeletedBy(Currency currency, long senderAccountId) {
        if (currency == null) return false; // prevent NPE

        if (!currency.is(CurrencyType.NON_SHUFFLEABLE)
            && shufflingService.getHoldingShufflingCount(currency.getId(), false) > 0) {
            return false;
        }
        if (!this.isActive(currency)) {
            return senderAccountId == currency.getAccountId();
        }
        if (currency.is(MINTABLE)
            && this.getCurrentSupply(currency) < currency.getMaxSupply()
            && senderAccountId != currency.getAccountId()) {
            return false;
        }

        List<AccountCurrency> accountCurrencies = accountCurrencyService
            .getCurrenciesByAccount(currency.getId(), 0, -1);
        return accountCurrencies.isEmpty() || accountCurrencies.size() == 1 && accountCurrencies.get(0).getAccountId() == senderAccountId;
    }

    @Override
    public void delete(Currency currency, LedgerEvent event, long eventId, Account senderAccount) {
        if (!canBeDeletedBy(currency, senderAccount.getId())) {
            // shouldn't happen as ownership has already been checked in validate, but as a safety check
            throw new IllegalStateException("Currency " + currency.getId() + " not entirely owned by "
                + senderAccount.getId());
        }
        if (currency.is(RESERVABLE)) {
            if (currency.is(CLAIMABLE) && this.isActive(currency)) {
                accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, event, eventId, currency.getId(),
                    -accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
                this.claimReserve(event, eventId, senderAccount, currency.getId(),
                    accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
            }
            if (!isActive(currency)) {
                Stream<CurrencyFounder> founders = currencyFounderService
                    .getCurrencyFoundersStream(currency.getId(), 0, Integer.MAX_VALUE);
                founders.forEach((founder) -> {
                    accountService.addToBalanceAndUnconfirmedBalanceATM(
                        accountService.getAccount(founder.getAccountId()),
                        event, eventId, Math.multiplyExact(currency.getReserveSupply(), founder.getAmountPerUnitATM()));
                });
            }
            currencyFounderService.remove(currency.getId());
        }
        if (currency.is(CurrencyType.EXCHANGEABLE)) {
            Stream<CurrencyBuyOffer> buyOffers =
                currencyExchangeOfferFacade.getCurrencyBuyOfferService().getOffersStream(currency, 0, -1);
            buyOffers.forEach((offer) -> {
                currencyExchangeOfferFacade.removeOffer(event, offer);
            });
        }
        if (currency.is(MINTABLE)) {
            // lazy init to break up circular dependency
            deleteMintingCurrency(currency);
        }
        accountCurrencyService.addToUnconfirmedCurrencyUnits(
            senderAccount, event, eventId, currency.getId(),
            -accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, currency.getId()));
        accountCurrencyService.addToCurrencyUnits(
            senderAccount, event, eventId, currency.getId(),
            -accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
        int height = blockChainInfoService.getHeight();
        currency.setHeight(height);
        currencyTable.deleteAtHeight(currency, height);
        createAndFireFullTextSearchDataEvent(currency, FullTextOperationData.OperationType.DELETE);
    }

    @Override
    public void validate(Currency currency, Transaction transaction) throws AplException.ValidationException {
        Objects.requireNonNull(transaction);
        if (currency == null) {
            log.trace("currency = {}, tr = {}, height = {}", currency, transaction, transaction.getHeight());
            log.trace("s-trace = {}", ThreadUtils.last5Stacktrace());
            throw new AplException.NotCurrentlyValidException("Unknown currency: " + transaction.getAttachment().getJSONObject());
        }
        this.validate(currency, currency.getType(), transaction);
    }

    public void validate(int type, Transaction transaction) throws AplException.ValidationException {
        Objects.requireNonNull(transaction);
        this.validate(null, type, transaction);
    }

    @Override
    public void validate(Currency currency, int type, Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException(String.format("Currency transaction %s amount must be 0", blockchainConfig.getCoinSymbol()));
        }
        if (type <= 0) {
            throw new AplException.NotValidException("Currency type not specified, because it's = " + type);
        }
        final EnumSet<CurrencyType> validators = EnumSet.noneOf(CurrencyType.class);
        for (CurrencyType currencyType : CurrencyType.values()) {
            if ((currencyType.getCode() & type) != 0) {
                validators.add(currencyType);
            }
        }
        long maxBalanceAtm = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        boolean isActiveCurrency = currency != null && this.isActive(currency);
        for (CurrencyType currencyType : CurrencyType.values()) {
            if ((currencyType.getCode() & type) != 0) {
                currencyType.validate(currency, transaction, validators, maxBalanceAtm, isActiveCurrency, validationHelper.getFinishValidationHeight(transaction, transaction.getAttachment()));
            } else {
                currencyType.validateMissing(currency, transaction, validators);
            }
        }
    }


    @Override
    public void validateCurrencyNamingStateIndependent(MonetarySystemCurrencyIssuance attachment) throws AplException.ValidationException {
        String name = attachment.getName();
        String code = attachment.getCode();
        String description = attachment.getDescription();
        if (name.length() < Constants.MIN_CURRENCY_NAME_LENGTH || name.length() > Constants.MAX_CURRENCY_NAME_LENGTH
            || name.length() < code.length()
            || code.length() < Constants.MIN_CURRENCY_CODE_LENGTH || code.length() > Constants.MAX_CURRENCY_CODE_LENGTH
            || description.length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            throw new AplException.NotValidException(String.format("Invalid currency name %s code %s or description %s", name, code, description));
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                throw new AplException.NotValidException("Invalid currency name: " + normalizedName);
            }
        }
        for (int i = 0; i < code.length(); i++) {
            if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(code.charAt(i)) < 0) {
                throw new AplException.NotValidException("Invalid currency code: " + code + " code must be all upper case");
            }
        }
        if (code.contains(blockchainConfig.getCoinSymbol()) || blockchainConfig.getCoinSymbol().toLowerCase().equals(normalizedName)) {
            throw new AplException.NotValidException("Currency name already used: " + code);
        }
    }
    @Override
    public void validateCurrencyNamingStateDependent(long issuerAccountId, MonetarySystemCurrencyIssuance attachment) throws AplException.ValidationException {
        String name = attachment.getName();
        String code = attachment.getCode();
        String normalizedName = name.toLowerCase();
        Currency currency;
        if ((currency = this.getCurrencyByName(normalizedName)) != null
            && !this.canBeDeletedBy(currency, issuerAccountId)) {
            throw new AplException.NotCurrentlyValidException("Currency name already used: " + normalizedName);
        }
        if ((currency = this.getCurrencyByCode(name)) != null
            && !this.canBeDeletedBy(currency, issuerAccountId)) {
            throw new AplException.NotCurrentlyValidException("Currency name already used as code: " + normalizedName);
        }
        if ((currency = this.getCurrencyByCode(code)) != null
            && !this.canBeDeletedBy(currency, issuerAccountId)) {
            throw new AplException.NotCurrentlyValidException("Currency code already used: " + code);
        }
        if ((currency = this.getCurrencyByName(code)) != null
            && !this.canBeDeletedBy(currency, issuerAccountId)) {
            throw new AplException.NotCurrentlyValidException("Currency code already used as name: " + code);
        }
    }


    @Override
    public void mintCurrency(LedgerEvent event, long eventId, final Account account,
                             final MonetarySystemCurrencyMinting attachment) {
        CurrencyMint currencyMint = currencyMintTable.get(
            CurrencyMintTable.currencyMintDbKeyFactory.newKey(attachment.getCurrencyId(), account.getId()));
        if (currencyMint != null && attachment.getCounter() <= currencyMint.getCounter()) {
            return;
        }
        Currency currency = getCurrency(attachment.getCurrencyId());
        CurrencySupply currencySupply = loadCurrencySupplyByCurrency(currency); // load dependency
        if (currencySupply != null) {
            currency.setCurrencySupply(currencySupply);
        }
        if (monetaryCurrencyMintingService.meetsTarget(account.getId(), currency, attachment)) {
            if (currencyMint == null) {
                currencyMint = new CurrencyMint(attachment.getCurrencyId(),
                    account.getId(), attachment.getCounter(), blockChainInfoService.getHeight());
            } else {
                currencyMint.setHeight(blockChainInfoService.getHeight());// important assign
                currencyMint.setCounter(attachment.getCounter());
            }
            currencyMintTable.insert(currencyMint);
            long units = Math.min(attachment.getUnits(),
                currency.getMaxSupply()
                    - (currency.getCurrencySupply() != null ? currency.getCurrencySupply().getCurrentSupply() : 0));
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                account, event, eventId, currency.getId(), units);
            increaseSupply(currency, units);
        } else {
            log.debug("Currency mint hash no longer meets target {}", attachment.getJSONObject().toJSONString());
        }
    }

    @Override
    public long getMintCounter(long currencyId, long accountId) {
        CurrencyMint currencyMint = currencyMintTable.get(CurrencyMintTable.currencyMintDbKeyFactory.newKey(currencyId, accountId));
        if (currencyMint != null) {
            return currencyMint.getCounter();
        } else {
            return 0;
        }
    }

    public void deleteMintingCurrency(Currency currency) {
        List<CurrencyMint> currencyMints = new ArrayList<>();
        try (DbIterator<CurrencyMint> mints = currencyMintTable.getManyBy(
            new DbClause.LongClause("currency_id", currency.getId()), 0, -1)) {
            while (mints.hasNext()) {
                currencyMints.add(mints.next());
            }
        }
        int currentHeight = blockChainInfoService.getHeight();
        currencyMints.forEach(c -> {
            c.setHeight(currentHeight); // important assign
            currencyMintTable.deleteAtHeight(c, currentHeight);
        });
    }

    /**
     * compose db_id list for in (id,..id) SQL luceneQuery
     *
     * @param luceneQuery lucene language luceneQuery pattern
     * @return composed sql luceneQuery part
     */
    private StringBuffer createDbIdInRangeFromLuceneData(String luceneQuery) {
        Objects.requireNonNull(luceneQuery, "luceneQuery is empty");
        StringBuffer inRange = new StringBuffer("(");
        int index = 0;
        try {
            ResultSet rs = fullTextSearchService.search("public", currencyTable.getTableName(), luceneQuery, Integer.MAX_VALUE, 0);
            while (rs.next()) {
                Long DB_ID = rs.getLong(5);
                if (index == 0) {
                    inRange.append(DB_ID);
                } else {
                    inRange.append(",").append(DB_ID);
                }
                index++;
            }
            inRange.append(")");
            log.debug("{}", inRange.toString());
        } catch (SQLException e) {
            log.error("FTS failed", e);
            throw new RuntimeException(e);
        }
        return inRange;
    }

    public DbIterator<Currency> fetchCurrencyByParams(int from, int to,
                                                      StringBuffer inRangeClause,
                                                      DbClause dbClause,
                                                      String sort) {
        Objects.requireNonNull(inRangeClause, "inRangeClause is NULL");
        Objects.requireNonNull(dbClause, "dbClause is NULL");
        Objects.requireNonNull(sort, "sort is NULL");

        Connection con = null;
        TransactionalDataSource dataSource = currencyTable.getDatabaseManager().getDataSource();
        final boolean doCache = dataSource.isInTransaction();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement(
                // select and load full entities from mariadb using prefetched DB_ID list from lucene
                "SELECT " + currencyTable.getTableName() + ".* FROM " + currencyTable.getTableName()
                    + " WHERE " + currencyTable.getTableName() + ".db_id in " + inRangeClause.toString()
                    + (currencyTable.isMultiversion() ? " AND " + currencyTable.getTableName() + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            DbUtils.setLimits(i, pstmt, from, to);
            return new DbIterator<>(con, pstmt, (connection, rs) -> {
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = currencyTable.getDbKeyFactory().newKey(rs);
                }
                return currencyTable.load(connection, rs, dbKey);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


    private void createAndFireFullTextSearchDataEvent(Currency currency, FullTextOperationData.OperationType operationType) {
        FullTextOperationData operationData = new FullTextOperationData(
            DEFAULT_SCHEMA, currencyTable.getTableName(), Thread.currentThread().getName());
        operationData.setOperationType(operationType);
        operationData.setDbIdValue(BigInteger.valueOf(currency.getDbId()));
        operationData.addColumnData(currency.getName()).addColumnData(currency.getDescription());
        // send data into Lucene index component
        log.trace("Put lucene index update data = {}", operationData);
        fullTextSearchUpdater.putFullTextOperationData(operationData);
    }

}
