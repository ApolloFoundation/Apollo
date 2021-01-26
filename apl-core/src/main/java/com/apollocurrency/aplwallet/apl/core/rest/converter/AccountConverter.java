/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.AccountAssetBalanceDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetUnconfirmedBalanceDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountLeaseDTO;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.vault.service.auth.TwoFactorAuthService;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author <andrew.zinchenko@gmail.com>
 */
public class AccountConverter implements Converter<Account, AccountDTO> {

    private final AccountService accountService;
    private final AccountInfoService accountInfoService;
    private final AccountLeaseService accountLeaseService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final Blockchain blockchain;
    private final AccountCurrencyConverter accountCurrencyConverter;
    private final CurrencyService currencyService;
    private final BlockchainConfig blockchainConfig;

    @Inject
    public AccountConverter(AccountService accountService,
                            AccountInfoService accountInfoService,
                            AccountLeaseService accountLeaseService,
                            TwoFactorAuthService twoFactorAuthService,
                            Blockchain blockchain,
                            AccountCurrencyConverter accountCurrencyConverter,
                            CurrencyService currencyService,
                            BlockchainConfig blockchainConfig) {
        this.accountService = accountService;
        this.accountInfoService = accountInfoService;
        this.accountLeaseService = accountLeaseService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.blockchain = blockchain;
        this.accountCurrencyConverter = accountCurrencyConverter;
        this.currencyService = currencyService;
        this.blockchainConfig = blockchainConfig;
    }

    private static void addAccountInfo(AccountDTO o, AccountInfo model) {
        if (o != null && model != null) {
            o.setName(model.getName());
            o.setDescription(model.getDescription());
        }
    }

    private static void addAccountLease(AccountDTO o, AccountLease model) {
        if (o != null && model != null) {
            o.setCurrentLessee(Long.toUnsignedString(model.getCurrentLesseeId()));
            o.setCurrentLeasingHeightFrom(model.getCurrentLeasingHeightFrom());
            o.setCurrentLeasingHeightTo(model.getCurrentLeasingHeightTo());
            if (model.getNextLesseeId() != 0) {
                o.setNextLessee(Long.toUnsignedString(model.getNextLesseeId()));
                o.setNextLeasingHeightFrom(model.getNextLeasingHeightFrom());
                o.setNextLeasingHeightTo(model.getNextLeasingHeightTo());
            }
        }
    }

    public static long anonymizeAccount() {
        Random random = new Random();
        return random.nextLong();
    }

    public static long anonymizeBalance() {
        Random random = new Random();
        return 100_000_000L * (random.nextInt(10_000_000) + 1);
    }

    public static String anonymizePublicKey(){
        Random random = new Random();
        byte[] b = new byte[32];
        random.nextBytes(b);
        return Convert.toHexString(b);
    }

    @Override
    public AccountDTO apply(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setAccount(Long.toUnsignedString(account.getId()));
        dto.setAccountRS(Convert2.rsAccount(account.getId()));
        if (account.getParentId() != 0) {
            dto.setParent(Convert2.rsAccount(account.getParentId()));
            dto.setAddressScope(account.getAddrScope().name());
        }
        dto.set2FA(twoFactorAuthService.isEnabled(account.getId()));
        PublicKey pk = account.getPublicKey();
        if (pk != null) {
            byte[] publicKey = pk.getPublicKey();
            if (publicKey != null) {
                dto.setPublicKey(Convert.toHexString(publicKey));
            }
        }
        dto.setBalanceATM(account.getBalanceATM());
        dto.setUnconfirmedBalanceATM(account.getUnconfirmedBalanceATM());
        dto.setForgedBalanceATM(account.getForgedBalanceATM());

        if (!account.getControls().isEmpty()) {
            dto.setAccountControls(account.getControls().stream()
                .map(Enum::name)
                .collect(Collectors.toSet()));
        }

        AccountInfo accountInfo = accountInfoService.getAccountInfo(account);
        if (accountInfo != null) {
            addAccountInfo(dto, accountInfo);
        }

        AccountLease accountLease = accountLeaseService.getAccountLease(account);
        if (accountLease != null) {
            addAccountLease(dto, accountLease);
        }

        return dto;
    }

    public void addEffectiveBalances(AccountDTO o, Account account) {
        if (o != null && account != null) {
            o.setEffectiveBalanceAPL(accountService.getEffectiveBalanceAPL(account, blockchain.getHeight(), true));
            o.setGuaranteedBalanceATM(accountService.getGuaranteedBalanceATM(account));
        }
    }

    public void addAccountLessors(AccountDTO o, List<Account> model, boolean includeEffectiveBalance) {
        if (o != null && model != null) {
            List<AccountLeaseDTO> lessors = model.stream()
                .map(lessor -> {
                    AccountLeaseDTO dto = new AccountLeaseDTO();
                    dto.setAccount(Long.toUnsignedString(lessor.getId()));
                    dto.setAccountRS(Convert2.rsAccount(lessor.getId()));
                    AccountLease accountLease = accountLeaseService.getAccountLease(lessor);
                    if (accountLease.getCurrentLesseeId() != 0) {
                        dto.setCurrentLessee(Long.toUnsignedString(accountLease.getCurrentLesseeId()));
                        dto.setCurrentLesseeRS(Convert2.rsAccount(accountLease.getCurrentLesseeId()));
                        dto.setCurrentHeightFrom(accountLease.getCurrentLeasingHeightFrom());
                        dto.setCurrentHeightTo(accountLease.getCurrentLeasingHeightTo());
                        if (includeEffectiveBalance) {
                            dto.setEffectiveBalanceAPL(accountService.getGuaranteedBalanceATM(lessor) / blockchainConfig.getOneAPL());
                        }
                    }
                    if (accountLease.getNextLesseeId() != 0) {
                        dto.setNextLessee(Long.toUnsignedString(accountLease.getNextLesseeId()));
                        dto.setNextLesseeRS(Convert2.rsAccount(accountLease.getNextLesseeId()));
                        dto.setNextHeightFrom(accountLease.getNextLeasingHeightFrom());
                        dto.setNextHeightTo(accountLease.getNextLeasingHeightTo());
                    }
                    return dto;
                }).collect(Collectors.toList());

            if (!lessors.isEmpty()) {
                List<String> lessorIds = new LinkedList<>();
                List<String> lessorIdsRS = new LinkedList<>();

                lessors.forEach(dto -> {
                    lessorIds.add(dto.getAccount());
                    lessorIdsRS.add(dto.getAccountRS());
                });

                o.setLessors(lessorIds);
                o.setLessorsRS(lessorIdsRS);
                o.setLessorsInfo(lessors);
            }
        }
    }

    public void addAccountAssets(AccountDTO o, List<AccountAsset> model) {
        if (o != null && model != null) {
            List<AccountAssetBalanceDTO> assetBalanceList = new LinkedList<>();
            List<AccountAssetUnconfirmedBalanceDTO> assetUnconfirmedBalanceList = new LinkedList<>();

            model.forEach(accountAsset -> {
                AccountAssetBalanceDTO balanceDTO = new AccountAssetBalanceDTO();
                balanceDTO.setAsset(Long.toUnsignedString(accountAsset.getAssetId()));
                balanceDTO.setBalanceATU(accountAsset.getQuantityATU());
                assetBalanceList.add(balanceDTO);

                AccountAssetUnconfirmedBalanceDTO unconfirmedBalanceDTO = new AccountAssetUnconfirmedBalanceDTO();
                unconfirmedBalanceDTO.setAsset(Long.toUnsignedString(accountAsset.getAssetId()));
                unconfirmedBalanceDTO.setUnconfirmedBalanceATU(accountAsset.getUnconfirmedQuantityATU());
                assetUnconfirmedBalanceList.add(unconfirmedBalanceDTO);

            });
            if (assetBalanceList.size() > 0) {
                o.setAssetBalances(assetBalanceList);
            }
            if (assetUnconfirmedBalanceList.size() > 0) {
                o.setUnconfirmedAssetBalances(assetUnconfirmedBalanceList);
            }

        }
    }

    public void addAccountCurrencies(AccountDTO o, List<AccountCurrency> model) {
        if (o != null && model != null) {
            List<AccountCurrencyDTO> currencies = model.stream()
                .map(accountCurrency -> {
                    AccountCurrencyDTO dto = accountCurrencyConverter.convert(accountCurrency);
                    accountCurrencyConverter.addCurrency(dto,  currencyService.getCurrency(accountCurrency.getCurrencyId()));
                    return dto;
                }).collect(Collectors.toList());

            if (!currencies.isEmpty()) {
                o.setAccountCurrencies(currencies);
            }
        }
    }

}
