/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <andrew.zinchenko@gmail.com>
 */
public class AccountConverter implements Converter<Account, AccountDTO> {
    @Override
    public AccountDTO apply(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setAccount(Long.toUnsignedString(account.getId()));
        dto.setAccountRS(Convert2.rsAccount(account.getId()));
        dto.set2FA(Helper2FA.isEnabled2FA(account.getId()));
        byte[] publicKey = Account.getPublicKey(account.getId());
        if (publicKey != null) {
            dto.setPublicKey(Convert.toHexString(publicKey));
        }
        dto.setBalanceATM(account.getBalanceATM());
        dto.setUnconfirmedBalanceATM(account.getUnconfirmedBalanceATM());
        dto.setForgedBalanceATM(account.getForgedBalanceATM());

        if (!account.getControls().isEmpty()) {
            dto.setAccountControls(account.getControls().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet()));
        }

        AccountInfo accountInfo = account.getAccountInfo();
        if ( accountInfo != null){
            addAccountInfo(dto, accountInfo);
        }

        AccountLease accountLease = account.getAccountLease();
        if (accountLease != null){
            addAccountLease(dto, accountLease);
        }

        return dto;
    }

    public void addEffectiveBalances(AccountDTO o, Account model){
        if (o != null && model != null) {
            o.setEffectiveBalanceAPL(model.getEffectiveBalanceAPL());
            o.setGuaranteedBalanceATM(model.getGuaranteedBalanceATM());
        }
    }

    public void addAccountLessors(AccountDTO o, List<Account> model, boolean includeEffectiveBalance){
        if (o != null && model != null) {
            List<AccountLeaseDTO> lessors = model.stream()
                    .map(lessor -> {
                        AccountLeaseDTO dto = new AccountLeaseDTO();
                        dto.setAccount(Long.toUnsignedString(lessor.getId()));
                        dto.setAccountRS(Convert2.rsAccount(lessor.getId()));
                        AccountLease accountLease = lessor.getAccountLease();
                        if (accountLease.getCurrentLesseeId() != 0) {
                            dto.setCurrentLessee(Long.toUnsignedString(accountLease.getCurrentLesseeId()));
                            dto.setCurrentLesseeRS(Convert2.rsAccount(accountLease.getCurrentLesseeId()));
                            dto.setCurrentHeightFrom(accountLease.getCurrentLeasingHeightFrom());
                            dto.setCurrentHeightTo(accountLease.getCurrentLeasingHeightTo());
                            if (includeEffectiveBalance) {
                                dto.setEffectiveBalanceAPL(lessor.getGuaranteedBalanceATM() / Constants.ONE_APL);
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

    public void addAccountAssets(AccountDTO o, List<AccountAsset> model){
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

    public void addAccountCurrencies(AccountDTO o, List<AccountCurrency> model){
        if (o != null && model != null) {
            List<AccountCurrencyDTO> currencies = model.stream()
                    .map(accountCurrency -> {
                        AccountCurrencyDTO dto = new AccountCurrencyDTO();
                        dto.setAccount(Long.toUnsignedString(accountCurrency.getAccountId()));
                        dto.setAccountRS(Convert2.rsAccount(accountCurrency.getAccountId()));
                        dto.setCurrency(Long.toUnsignedString(accountCurrency.getCurrencyId()));
                        dto.setUnits(accountCurrency.getUnits());
                        dto.setUnconfirmedUnits(accountCurrency.getUnconfirmedUnits());

                        Currency currency = Currency.getCurrency(accountCurrency.getCurrencyId());
                        if (currency != null) {
                            dto.setName(currency.getName());
                            dto.setCode(currency.getCode());
                            dto.setType(currency.getType());
                            dto.setDecimals(currency.getDecimals());
                            dto.setIssuanceHeight(currency.getIssuanceHeight());
                            dto.setIssuerAccount(Long.toUnsignedString(currency.getAccountId()));
                            dto.setIssuerAccountRS(Convert2.rsAccount(currency.getAccountId()));
                        }
                        return dto;
                    }).collect(Collectors.toList());

            if (!currencies.isEmpty()) {
                o.setAccountCurrencies(currencies);
            }
        }
    }

    private static void addAccountInfo(AccountDTO o, AccountInfo model){
        if (o != null && model != null) {
            o.setName(model.getName());
            o.setDescription(model.getDescription());
        }
    }

    private static void addAccountLease(AccountDTO o, AccountLease model){
        if (o != null && model != null) {
            o.setCurrentLessee(Long.toUnsignedString(model.getCurrentLesseeId()));
            o.setCurrentLeasingHeightFrom(model.getCurrentLeasingHeightFrom());
            o.setCurrentLeasingHeightTo(model.getCurrentLeasingHeightTo());
            if ( model.getNextLesseeId() != 0 ){
                o.setNextLessee(Long.toUnsignedString(model.getNextLesseeId()));
                o.setNextLeasingHeightFrom(model.getNextLeasingHeightFrom());
                o.setNextLeasingHeightTo(model.getNextLeasingHeightTo());
            }
        }
    }

}
