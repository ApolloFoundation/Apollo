package com.apollocurrency.aplwallet.apl.core.transaction;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class TransactionTypes {
    public static final byte TYPE_PAYMENT = 0;
    public static final byte TYPE_MESSAGING = 1;
    public static final byte TYPE_COLORED_COINS = 2;
    public static final byte TYPE_DIGITAL_GOODS = 3;
    public static final byte TYPE_ACCOUNT_CONTROL = 4;
    public static final byte TYPE_MONETARY_SYSTEM = 5;
    public static final byte TYPE_DATA = 6;
    public static final byte TYPE_SHUFFLING = 7;
    public static final byte TYPE_UPDATE = 8;
    public static final byte TYPE_DEX = 9;
    public static final byte TYPE_CHILD_ACCOUNT = 10;
    public static final byte TYPE_SMC = 11;
    //TYPE=0
    public static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
    public static final byte SUBTYPE_PAYMENT_PRIVATE_PAYMENT = 1;
    //TYPE=1
    public static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    public static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    public static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    public static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    public static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    public static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    public static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    public static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    public static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY = 10;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE = 11;
    //TYPE=2
    public static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    public static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    public static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    public static final byte SUBTYPE_COLORED_COINS_ASSET_DELETE = 7;
    //TYPE=3
    public static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    public static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    public static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    public static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    public static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    public static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    public static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    public static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;
    //TYPE=4
    public static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    public static final byte SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY = 1;
    //TYPE=5
    public static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    public static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;
    //TYPE=6
    public static final byte SUBTYPE_UPDATE_CRITICAL = 0;
    public static final byte SUBTYPE_UPDATE_IMPORTANT = 1;
    public static final byte SUBTYPE_UPDATE_MINOR = 2;
    public static final byte SUBTYPE_UPDATE_V2 = 3;
    //TYPE=7
    public static final byte SUBTYPE_DEX_ORDER = 0;
    public static final byte SUBTYPE_DEX_ORDER_CANCEL = 1;
    public static final byte SUBTYPE_DEX_CONTRACT = 2;
    public static final byte SUBTYPE_DEX_TRANSFER_MONEY = 3;
    public static final byte SUBTYPE_DEX_CLOSE_ORDER = 4;
    //TYPE=8
    public static final byte SUBTYPE_SHUFFLING_CREATION = 0;
    public static final byte SUBTYPE_SHUFFLING_REGISTRATION = 1;
    public static final byte SUBTYPE_SHUFFLING_PROCESSING = 2;
    public static final byte SUBTYPE_SHUFFLING_RECIPIENTS = 3;
    public static final byte SUBTYPE_SHUFFLING_VERIFICATION = 4;
    public static final byte SUBTYPE_SHUFFLING_CANCELLATION = 5;
    //TYPE=9
    public static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE = 0;
    public static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE = 1;
    public static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM = 2;
    public static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER = 3;
    public static final byte SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER = 4;
    public static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY = 5;
    public static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL = 6;
    public static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING = 7;
    public static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION = 8;
    public static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_BURNING = 9;
    //TYPE=10
    public static final byte SUBTYPE_CHILD_CREATE = 0;
    public static final byte SUBTYPE_CHILD_CONVERT_TO = 1;
    //TYPE=11
    public static final byte SUBTYPE_SMC_PUBLISH = 0;
    public static final byte SUBTYPE_SMC_CALL_METHOD = 1;

    private static final Map<Integer, TransactionTypeSpec> ALL_TYPES = new HashMap<>();

    @Getter
    public enum TransactionTypeSpec {
        ORDINARY_PAYMENT(TYPE_PAYMENT, SUBTYPE_PAYMENT_ORDINARY_PAYMENT, "OrdinaryPayment"),
        PRIVATE_PAYMENT(TYPE_PAYMENT, SUBTYPE_PAYMENT_PRIVATE_PAYMENT, "PrivatePayment"),

        ARBITRARY_MESSAGE(TYPE_MESSAGING, SUBTYPE_MESSAGING_ARBITRARY_MESSAGE, "ArbitraryMessage"),
        ALIAS_ASSIGNMENT(TYPE_MESSAGING, SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT, "AliasAssignment"),
        POLL_CREATION(TYPE_MESSAGING, SUBTYPE_MESSAGING_POLL_CREATION, "PollCreation"),
        VOTE_CASTING(TYPE_MESSAGING, SUBTYPE_MESSAGING_VOTE_CASTING, "VoteCasting"),
        HUB_ANNOUNCEMENT(TYPE_MESSAGING, SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT, "HubAnnouncement"),
        ACCOUNT_INFO(TYPE_MESSAGING, SUBTYPE_MESSAGING_ACCOUNT_INFO, "AccountInfo"),
        ALIAS_SELL(TYPE_MESSAGING, SUBTYPE_MESSAGING_ALIAS_SELL, "AliasSell"),
        ALIAS_BUY(TYPE_MESSAGING, SUBTYPE_MESSAGING_ALIAS_BUY, "AliasBuy"),
        ALIAS_DELETE(TYPE_MESSAGING, SUBTYPE_MESSAGING_ALIAS_DELETE, "AliasDelete"),
        PHASING_VOTE_CASTING(TYPE_MESSAGING, SUBTYPE_MESSAGING_PHASING_VOTE_CASTING, "PhasingVoteCasting"),
        ACCOUNT_PROPERTY(TYPE_MESSAGING, SUBTYPE_MESSAGING_ACCOUNT_PROPERTY, "AccountProperty"),
        ACCOUNT_PROPERTY_DELETE(TYPE_MESSAGING, SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE, "AccountPropertyDelete"),

        EFFECTIVE_BALANCE_LEASING(TYPE_ACCOUNT_CONTROL, SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING, "EffectiveBalanceLeasing"),
        SET_PHASING_ONLY(TYPE_ACCOUNT_CONTROL, SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY, "SetPhasingOnly"),

        TAGGED_DATA_UPLOAD(TYPE_DATA, SUBTYPE_DATA_TAGGED_DATA_UPLOAD, "TaggedDataUpload"),
        TAGGED_DATA_EXTEND(TYPE_DATA, SUBTYPE_DATA_TAGGED_DATA_EXTEND, "TaggedDataExtend"),

        SHUFFLING_CREATION(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_CREATION, "ShufflingCreation"),
        SHUFFLING_REGISTRATION(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_REGISTRATION, "ShufflingRegistration"),
        SHUFFLING_PROCESSING(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_PROCESSING, "ShufflingProcessing"),
        SHUFFLING_RECIPIENTS(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_RECIPIENTS, "ShufflingRecipients"),
        SHUFFLING_VERIFICATION(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_VERIFICATION, "ShufflingVerification"),
        SHUFFLING_CANCELLATION(TYPE_SHUFFLING, SUBTYPE_SHUFFLING_CANCELLATION, "ShufflingCancellation"),

        MS_CURRENCY_ISSUANCE(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE, "CurrencyIssuance"),
        MS_RESERVE_INCREASE(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE, "ReserveIncrease"),
        MS_RESERVE_CLAIM(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM, "ReserveClaim"),
        MS_CURRENCY_TRANSFER(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER, "CurrencyTransfer"),
        MS_PUBLISH_EXCHANGE_OFFER(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER, "PublishExchangeOffer"),
        MS_EXCHANGE_BUY(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY, "ExchangeBuy"),
        MS_EXCHANGE_SELL(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL, "ExchangeSell"),
        MS_CURRENCY_MINTING(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING, "CurrencyMinting"),
        MS_CURRENCY_DELETION(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION, "CurrencyDeletion"),
        MS_CURRENCY_BURNING(TYPE_MONETARY_SYSTEM, SUBTYPE_MONETARY_SYSTEM_CURRENCY_BURNING, "CurrencyBurning"),

        CC_ASSET_ISSUANCE(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_ASSET_ISSUANCE, "AssetIssuance"),
        CC_ASSET_TRANSFER(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_ASSET_TRANSFER, "AssetTransfer"),
        CC_ASK_ORDER_PLACEMENT(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT, "AskOrderPlacement"),
        CC_BID_ORDER_PLACEMENT(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT, "BidOrderPlacement"),
        CC_ASK_ORDER_CANCELLATION(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION, "AskOrderCancellation"),
        CC_BID_ORDER_CANCELLATION(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION, "BidOrderCancellation"),
        CC_DIVIDEND_PAYMENT(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT, "DividendPayment"),
        CC_ASSET_DELETE(TYPE_COLORED_COINS, SUBTYPE_COLORED_COINS_ASSET_DELETE, "AssetDelete"),

        DGS_LISTING(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_LISTING, "DigitalGoodsListing"),
        DGS_DELISTING(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_DELISTING, "DigitalGoodsDelisting"),
        DGS_CHANGE_PRICE(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE, "DigitalGoodsPriceChange"),
        DGS_CHANGE_QUANTITY(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE, "DigitalGoodsQuantityChange"),
        DGS_PURCHASE(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_PURCHASE, "DigitalGoodsPurchase"),
        DGS_DELIVERY(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_DELIVERY, "DigitalGoodsDelivery"),
        DGS_FEEDBACK(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_FEEDBACK, "DigitalGoodsFeedback"),
        DGS_REFUND(TYPE_DIGITAL_GOODS, SUBTYPE_DIGITAL_GOODS_REFUND, "DigitalGoodsRefund"),

        CRITICAL_UPDATE(TYPE_UPDATE, SUBTYPE_UPDATE_CRITICAL, "CriticalUpdate"),
        IMPORTANT_UPDATE(TYPE_UPDATE, SUBTYPE_UPDATE_IMPORTANT, "ImportantUpdate"),
        MINOR_UPDATE(TYPE_UPDATE, SUBTYPE_UPDATE_MINOR, "MinorUpdate"),
        UPDATE_V2(TYPE_UPDATE, SUBTYPE_UPDATE_V2, "UpdateV2"),

        DEX_ORDER(TYPE_DEX, SUBTYPE_DEX_ORDER, "DexOrder"),
        DEX_CANCEL_ORDER(TYPE_DEX, SUBTYPE_DEX_ORDER_CANCEL, "CancelOrder"),
        DEX_CONTRACT(TYPE_DEX, SUBTYPE_DEX_CONTRACT, "DexContract"),
        DEX_TRANSFER_MONEY(TYPE_DEX, SUBTYPE_DEX_TRANSFER_MONEY, "DexTransferMoney"),
        DEX_CLOSE_ORDER(TYPE_DEX, SUBTYPE_DEX_CLOSE_ORDER, "DexCloseOrder"),

        CHILD_ACCOUNT_CREATE(TYPE_CHILD_ACCOUNT, SUBTYPE_CHILD_CREATE, "CreateChildAccount"),
        CHILD_ACCOUNT_CONVERT_TO(TYPE_CHILD_ACCOUNT, SUBTYPE_CHILD_CONVERT_TO, "ConvertToChildAccount"),

        SMC_PUBLISH(TYPE_SMC, SUBTYPE_SMC_PUBLISH, "SmcPublish"),
        SMC_CALL_METHOD(TYPE_SMC, SUBTYPE_SMC_CALL_METHOD, "SmcCallMethod")
        ;

        private final byte type;
        private final byte subtype;
        private final String compatibleName;

        TransactionTypeSpec(int type, int subtype, String compatibleName) {
            this.type = (byte) type;
            this.subtype = (byte) subtype;
            this.compatibleName = compatibleName;
            ALL_TYPES.put(subtype | type << 8, this);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", TransactionTypeSpec.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("subtype=" + subtype)
                .add("compatibleName='" + compatibleName + "'")
                .toString();
        }
    }

    public static TransactionTypeSpec find(int type, int subtype) {
        TransactionTypeSpec spec = ALL_TYPES.get(subtype | type << 8);
        if (spec == null) {
            throw new IllegalArgumentException("Unable to find spec for type '" + type + "' and subtype '" + subtype + "'");
        }
        return spec;
    }
}
