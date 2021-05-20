/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.MonetaryCurrencyMintingService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncrease;
import com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MonetarySystemExchangeTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_CURRENCY_BURNING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_CURRENCY_MINTING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_RESERVE_CLAIM;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_RESERVE_INCREASE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_CREATION;

/**
 * Define and validate currency capabilities
 */
@Slf4j
public enum CurrencyType implements CurrencyTypeValidatable {

    /**
     * Can be exchanged from/to APL<br>
     */
    EXCHANGEABLE(0x01) {
        @Override
        public void validateMissing(Currency currency, Transaction transaction,
                                    Set<CurrencyType> validators) throws AplException.NotValidException {
            log.trace("EXCHANGEABLE 2 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                if (!validators.contains(CLAIMABLE)) {
                    throw new AplException.NotValidException("Currency is not exchangeable and not claimable");
                }
            }
            if (transaction.getType() instanceof MonetarySystemExchangeTransactionType || transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER) {
                throw new AplException.NotValidException("Currency is not exchangeable");
            }
        }
    },
    /**
     * Transfers are only allowed from/to issuer account<br>
     * Only issuer account can publish exchange offer<br>
     */
    CONTROLLABLE(0x02) {
        @Override
        public void validate(Currency currency, Transaction transaction,
                             Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.NotValidException {
            log.trace("CONTROLLABLE 1 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.MS_CURRENCY_TRANSFER) {
                if (currency == null || (currency.getAccountId() != transaction.getSenderId() && currency.getAccountId() != transaction.getRecipientId())) {
                    throw new AplException.NotValidException("Controllable currency can only be transferred to/from issuer account");
                }
            }
            if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER) {
                if (currency == null || currency.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotValidException("Only currency issuer can publish an exchange offer for controllable currency");
                }
            }
        }
    },
    /**
     * Can be reserved before the currency is active, reserve is distributed to founders once the currency becomes active<br>
     */
    RESERVABLE(0x04) {
        @Override
        public void validate(Currency currency, Transaction transaction,
                             Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.ValidationException {
            log.trace("RESERVABLE 1 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
                int issuanceHeight = attachment.getIssuanceHeight();
                int finishHeight = finishValidationHeight;
                if (issuanceHeight <= finishHeight) {
                    throw new AplException.NotCurrentlyValidException(
                        String.format("Reservable currency activation height %d not higher than transaction apply height %d",
                            issuanceHeight, finishHeight));
                }
                if (attachment.getMinReservePerUnitATM() <= 0) {
                    throw new AplException.NotValidException("Minimum reserve per unit must be > 0");
                }

                if (Math.multiplyExact(attachment.getMinReservePerUnitATM(), attachment.getReserveSupply()) >
                    maxBalanceAtm) {
                    throw new AplException.NotValidException("Minimum reserve per unit is too large");
                }
                if (attachment.getReserveSupply() <= attachment.getInitialSupply()) {
                    throw new AplException.NotValidException("Reserve supply must exceed initial supply");
                }
                if (!validators.contains(MINTABLE) && attachment.getReserveSupply() < attachment.getMaxSupply()) {
                    throw new AplException.NotValidException("Max supply must not exceed reserve supply for reservable and non-mintable currency");
                }
            }
            if (transaction.getType().getSpec() == MS_RESERVE_INCREASE) {
                MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
                if (currency != null && currency.getIssuanceHeight() <= finishValidationHeight) {
                    throw new AplException.NotCurrentlyValidException("Cannot increase reserve for active currency");
                }
            }
        }

        @Override
        public void validateMissing(Currency currency, Transaction transaction,
                                    Set<CurrencyType> validators) throws AplException.NotValidException {
            log.trace("RESERVABLE 2 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_RESERVE_INCREASE) {
                throw new AplException.NotValidException("Cannot increase reserve since currency is not reservable");
            }
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (attachment.getIssuanceHeight() != 0) {
                    throw new AplException.NotValidException("Issuance height for non-reservable currency must be 0");
                }
                if (attachment.getMinReservePerUnitATM() > 0) {
                    throw new AplException.NotValidException("Minimum reserve per unit for non-reservable currency must be 0 ");
                }
                if (attachment.getReserveSupply() > 0) {
                    throw new AplException.NotValidException("Reserve supply for non-reservable currency must be 0");
                }
                if (!validators.contains(MINTABLE) && attachment.getInitialSupply() < attachment.getMaxSupply()) {
                    throw new AplException.NotValidException("Initial supply for non-reservable and non-mintable currency must be equal to max supply");
                }
            }
        }
    },
    /**
     * Is {@link #RESERVABLE} and can be claimed after currency is active<br>
     * Cannot be {@link #EXCHANGEABLE}
     */
    CLAIMABLE(0x08) {
        @Override
        public void validate(Currency currency, Transaction transaction,
                             Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.ValidationException {
            log.trace("CLAIMABLE 1 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (!validators.contains(RESERVABLE)) {
                    throw new AplException.NotValidException("Claimable currency must be reservable");
                }
                if (validators.contains(MINTABLE)) {
                    throw new AplException.NotValidException("Claimable currency cannot be mintable");
                }
                if (attachment.getInitialSupply() > 0) {
                    throw new AplException.NotValidException("Claimable currency must have initial supply 0");
                }
            }
            if (transaction.getType().getSpec() == MS_RESERVE_CLAIM) {
//                if (currency == null || !currency.isActive()) {
                if (currency == null ||  !isActiveCurrency) {
                    throw new AplException.NotCurrentlyValidException("Cannot claim reserve since currency is not yet active");
                }
            }
            if (transaction.getType().getSpec() == MS_CURRENCY_BURNING) {
                throw new AplException.NotCurrentlyValidException("Cannot burn Claimable currency, use claimReserve instead");
            }
        }

        @Override
        public void validateMissing(Currency currency, Transaction transaction,
                                    Set<CurrencyType> validators) throws AplException.NotValidException {
            log.trace("CLAIMABLE 2 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_RESERVE_CLAIM) {
                throw new AplException.NotValidException("Cannot claim reserve since currency is not claimable");
            }
        }
    },
    /**
     * Can be minted using proof of work algorithm<br>
     */
    MINTABLE(0x10) {
        @Override
        public void validate(Currency currency, Transaction transaction,
                             Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.NotValidException, AplException.NotCurrentlyValidException {
            log.trace("MINTABLE 1 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                MonetarySystemCurrencyIssuance issuanceAttachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
                try {
                    HashFunction hashFunction = HashFunction.getHashFunction(issuanceAttachment.getAlgorithm());
                    if (!MonetaryCurrencyMintingService.acceptedHashFunctions.contains(hashFunction)) {
                        throw new AplException.NotValidException("Invalid minting algorithm " + hashFunction);
                    }
                } catch (IllegalArgumentException e) {
                    throw new AplException.NotValidException("Illegal algorithm code specified", e);
                }
                if (issuanceAttachment.getMinDifficulty() < 1 || issuanceAttachment.getMaxDifficulty() > 255 ||
                    issuanceAttachment.getMaxDifficulty() < issuanceAttachment.getMinDifficulty()) {
                    throw new AplException.NotValidException(
                        String.format("Invalid minting difficulties min %d max %d, difficulty must be between 1 and 255, max larger than min",
                            issuanceAttachment.getMinDifficulty(), issuanceAttachment.getMaxDifficulty()));
                }
                if (issuanceAttachment.getMaxSupply() <= issuanceAttachment.getReserveSupply()) {
                    throw new AplException.NotValidException("Max supply for mintable currency must exceed reserve supply");
                }
            }
            if (transaction.getType().getSpec() == MS_CURRENCY_BURNING) {
                throw new AplException.NotCurrentlyValidException("Cannot burn Mintable currency");
            }
        }

        @Override
        public void validateMissing(Currency currency, Transaction transaction,
                                    Set<CurrencyType> validators) throws AplException.NotValidException {
            log.trace("MINTABLE 2 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == MS_CURRENCY_ISSUANCE) {
                MonetarySystemCurrencyIssuance issuanceAttachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (issuanceAttachment.getMinDifficulty() != 0 ||
                    issuanceAttachment.getMaxDifficulty() != 0 ||
                    issuanceAttachment.getAlgorithm() != 0) {
                    throw new AplException.NotValidException("Non mintable currency should not specify algorithm or difficulty");
                }
            }
            if (transaction.getType().getSpec() == MS_CURRENCY_MINTING) {
                throw new AplException.NotValidException("Currency is not mintable");
            }
        }

    },
    /**
     * Several accounts can shuffle their currency units and then distributed to recipients<br>
     */
    NON_SHUFFLEABLE(0x20) {
        @Override
        public void validate(Currency currency, Transaction transaction,
                             Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.ValidationException {
            log.trace("NON_SHUFFLEABLE 1 [{}]: \ncurrency={}, \n{}, \n{}", transaction.getECBlockHeight(), currency, transaction, validators);
            if (transaction.getType().getSpec() == SHUFFLING_CREATION) {
                throw new AplException.NotValidException("Shuffling is not allowed for this currency");
            }
        }
    };

    private final int code;

    CurrencyType(int code) {
        this.code = code;
    }

    public static CurrencyType get(int code) {
        for (CurrencyType currencyType : values()) {
            if (currencyType.getCode() == code) {
                return currencyType;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

}
