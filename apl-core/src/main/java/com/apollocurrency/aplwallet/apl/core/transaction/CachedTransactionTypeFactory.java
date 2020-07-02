/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.transaction.types.control.AccountControlTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.control.SetPhasingOnlyTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.PaymentTransactionType;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_ASSET_DELETE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DATA_TAGGED_DATA_EXTEND;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DEX_CLOSE_ORDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DEX_CONTRACT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DEX_ORDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DEX_ORDER_CANCEL;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DEX_TRANSFER_MONEY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_DELISTING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_DELIVERY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_LISTING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_PURCHASE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_DIGITAL_GOODS_REFUND;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ACCOUNT_INFO;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ALIAS_BUY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ALIAS_DELETE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ALIAS_SELL;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_POLL_CREATION;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_MESSAGING_VOTE_CASTING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_PAYMENT_PRIVATE_PAYMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_UPDATE_CRITICAL;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_UPDATE_IMPORTANT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_UPDATE_MINOR;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.SUBTYPE_UPDATE_V2;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_ACCOUNT_CONTROL;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_COLORED_COINS;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_DATA;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_DEX;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_DIGITAL_GOODS;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_MESSAGING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_MONETARY_SYSTEM;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_PAYMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_SHUFFLING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TYPE_UPDATE;

@Singleton
public class CachedTransactionTypeFactory {
    private Map<TypeSubtype, TransactionType> types = new HashMap<>();

    @Inject
    public CachedTransactionTypeFactory(Instance<TransactionType> typeInstances) {
        for (TransactionType typeInstance : typeInstances) {
            putIfNotPresent(typeInstance);
        }
    }

    private void putIfNotPresent(TransactionType type) {
        TypeSubtype typeSubtype = new TypeSubtype(type.getType(), type.getSubtype());
        if (types.containsKey(typeSubtype)) {
            throw new IllegalStateException("Duplicate instance for type: " + typeSubtype);
        }
        types.put(typeSubtype, type);
    }

    public CachedTransactionTypeFactory(Collection<TransactionType> transactionTypes) {
        for (TransactionType transactionType : transactionTypes) {
            putIfNotPresent(transactionType);
        }
    }

    @lombok.Data
    private static class TypeSubtype {
        private final int type;
        private final int subtype;
    }



    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return PaymentTransactionType.ORDINARY;
                    case SUBTYPE_PAYMENT_PRIVATE_PAYMENT:
                        return PaymentTransactionType.PRIVATE;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        throw new IllegalArgumentException("Hub Announcement no longer supported");
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    case SUBTYPE_MESSAGING_ALIAS_DELETE:
                        return Messaging.ALIAS_DELETE;
                    case SUBTYPE_MESSAGING_PHASING_VOTE_CASTING:
                        return Messaging.PHASING_VOTE_CASTING;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY:
                        return Messaging.ACCOUNT_PROPERTY;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE:
                        return Messaging.ACCOUNT_PROPERTY_DELETE;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoins.BID_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT:
                        return ColoredCoins.DIVIDEND_PAYMENT;
                    case SUBTYPE_COLORED_COINS_ASSET_DELETE:
                        return ColoredCoins.ASSET_DELETE;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return AccountControlTransactionType.EFFECTIVE_BALANCE_LEASING;
                    case SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY:
                        return SetPhasingOnlyTransactionType.SET_PHASING_ONLY;
                    default:
                        return null;
                }
            case TYPE_MONETARY_SYSTEM:
                return MonetarySystem.findTransactionType(subtype);
            case TYPE_DATA:
                switch (subtype) {
                    case SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                        return Data.TAGGED_DATA_UPLOAD;
                    case SUBTYPE_DATA_TAGGED_DATA_EXTEND:
                        return Data.TAGGED_DATA_EXTEND;
                    default:
                        return null;
                }
            case TYPE_SHUFFLING:
                return ShufflingTransaction.findTransactionType(subtype);
            case TYPE_UPDATE:
                switch (subtype) {
                    case SUBTYPE_UPDATE_CRITICAL:
                        return Update.CRITICAL;
                    case SUBTYPE_UPDATE_IMPORTANT:
                        return Update.IMPORTANT;
                    case SUBTYPE_UPDATE_MINOR:
                        return Update.MINOR;
                    case SUBTYPE_UPDATE_V2:
                        return Update.UPDATE_V2;
                    default:
                        return null;
                }
            case TYPE_DEX:
                switch (subtype) {
                    case SUBTYPE_DEX_ORDER:
                        return DEX.DEX_ORDER_TRANSACTION;
                    case SUBTYPE_DEX_ORDER_CANCEL:
                        return DEX.DEX_CANCEL_ORDER_TRANSACTION;
                    case SUBTYPE_DEX_CONTRACT:
                        return DEX.DEX_CONTRACT_TRANSACTION;
                    case SUBTYPE_DEX_TRANSFER_MONEY:
                        return DEX.DEX_TRANSFER_MONEY_TRANSACTION;
                    case SUBTYPE_DEX_CLOSE_ORDER:
                        return DEX.DEX_CLOSE_ORDER;
                    default:
                        return null;
                }

            default:
                return null;
        }
    }
}
