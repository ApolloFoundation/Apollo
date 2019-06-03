/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import javax.sql.DataSource;

public class ShardAddConstraintsSchemaVersion extends ShardInitTableSchemaVersion {
    private static final Logger log = getLogger(ShardAddConstraintsSchemaVersion.class);
    private static final int startNumber = 66;

    protected int update(int nextUpdate) {
        nextUpdate = super.update(nextUpdate);
        switch (nextUpdate) {
/* BLOCK -------------------    */
            case startNumber:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS PK_BLOCK_ID primary key (ID)"); // PK + unique index
            case startNumber + 1:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE BLOCK ADD CONSTRAINT IF NOT EXISTS chk_timeout CHECK (timeout >= 0)");
            case startNumber + 2:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS BLOCK_TIMESTAMP_IDX unique (TIMESTAMP)");
            case startNumber + 3:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS BLOCK_HEIGHT_IDX unique (HEIGHT)");
            case startNumber + 4:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("create unique index IF NOT EXISTS PRIMARY_KEY_BLOCK_DB_ID_INDEX on BLOCK (DB_ID)");
            case startNumber + 5:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("create index IF NOT EXISTS BLOCK_GENERATOR_ID_IDX on BLOCK (GENERATOR_ID)");
            case startNumber + 6:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON BLOCK (id)");
            case startNumber + 7:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON BLOCK (height)");
            case startNumber + 8:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx ON BLOCK (generator_id)");
            case startNumber + 9:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON BLOCK (timestamp DESC)");
/* TRANSACTION -------------------    */
            case startNumber + 10:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("alter table TRANSACTION add constraint IF NOT EXISTS PK_TRANSACTION_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 11:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("ALTER TABLE TRANSACTION ADD CONSTRAINT IF NOT EXISTS TRANSACTION_TO_BLOCK_FK FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE");
            case startNumber + 12:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("alter table TRANSACTION add constraint IF NOT EXISTS TRANSACTION_ID_IDX unique (ID)");
            case startNumber + 13:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON TRANSACTION (id)");
            case startNumber + 14:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON TRANSACTION (sender_id)");
            case startNumber + 15:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON TRANSACTION (recipient_id)");
            case startNumber + 16:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON TRANSACTION (block_timestamp DESC)");
            case startNumber + 17:
/* ALIAS -------------------    */
                apply("CREATE INDEX IF NOT EXISTS transaction_block_id_idx ON TRANSACTION (block_id)");
            case startNumber + 18:
                apply("alter table ALIAS add constraint IF NOT EXISTS PK_ALIAS_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 19:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON ALIAS (id, height DESC)");
            case startNumber + 20:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON ALIAS (account_id, height DESC)");
            case startNumber + 21:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON ALIAS (alias_name_lower)");
/* ALIAS_OFFER -------------------    */
            case startNumber + 22:
                apply("CREATE INDEX IF NOT EXISTS alias_height_id_idx ON ALIAS (height, id)");
            case startNumber + 23:
                apply("alter table ALIAS_OFFER add constraint IF NOT EXISTS PK_ALIAS_OFFER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 24:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON ALIAS_OFFER (id, height DESC)");
            case startNumber + 25:
                apply("CREATE INDEX IF NOT EXISTS alias_offer_height_id_idx ON ALIAS_OFFER (height, id)");
/* ASSET -------------------    */
            case startNumber + 26:
                apply("alter table ASSET add constraint IF NOT EXISTS PK_ASSET_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 27:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON ASSET (account_id)");
            case startNumber + 28:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_height_idx ON ASSET (id, height DESC)");
            case startNumber + 29:
                apply("CREATE INDEX IF NOT EXISTS asset_height_id_idx ON ASSET (height, id)");
/* TRADE -------------------    */
            case startNumber + 30:
                apply("alter table TRADE add constraint IF NOT EXISTS PK_TRADE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 31:
                apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC)");
            case startNumber + 32:
                apply("CREATE INDEX IF NOT EXISTS trade_seller_id_idx ON trade (seller_id, height DESC)");
            case startNumber + 33:
                apply("CREATE INDEX IF NOT EXISTS trade_buyer_id_idx ON trade (buyer_id, height DESC)");
            case startNumber + 34:
                apply("CREATE INDEX IF NOT EXISTS trade_height_idx ON trade(height)");
            case startNumber + 35:
                apply("CREATE INDEX IF NOT EXISTS trade_ask_idx ON trade (ask_order_id, height DESC)");
            case startNumber + 36:
                apply("CREATE INDEX IF NOT EXISTS trade_bid_idx ON trade (bid_order_id, height DESC)");
            case startNumber + 37:
                apply("CREATE INDEX IF NOT EXISTS trade_height_db_id_idx ON trade (height DESC, db_id DESC)");
/* ASK_ORDER -------------------    */
            case startNumber + 38:
                apply("alter table ASK_ORDER add constraint IF NOT EXISTS PK_ASK_ORDER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 39:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ASK_ORDER (id, height DESC)");
            case startNumber + 40:
                apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ASK_ORDER (account_id, height DESC)");
            case startNumber + 41:
                apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ASK_ORDER (asset_id, price)");
            case startNumber + 42:
                apply("CREATE INDEX IF NOT EXISTS ask_order_creation_idx ON ASK_ORDER (creation_height DESC)");
            case startNumber + 43:
                apply("CREATE INDEX IF NOT EXISTS ask_order_height_id_idx ON ASK_ORDER (height, id)");
/* BID_ORDER -------------------    */
            case startNumber + 44:
                apply("alter table BID_ORDER add constraint IF NOT EXISTS PK_BID_ORDER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 45:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON BID_ORDER (id, height DESC)");
            case startNumber + 46:
                apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON BID_ORDER (account_id, height DESC)");
            case startNumber + 47:
                apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON BID_ORDER (asset_id, price DESC)");
            case startNumber + 48:
                apply("CREATE INDEX IF NOT EXISTS bid_order_creation_idx ON BID_ORDER (creation_height DESC)");
            case startNumber + 49:
                apply("CREATE INDEX IF NOT EXISTS bid_order_height_id_idx ON BID_ORDER (height, id)");
/* GOODS -------------------    */
            case startNumber + 50:
                apply("alter table GOODS add constraint IF NOT EXISTS PK_GOODS_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 51:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON GOODS (id, height DESC)");
            case startNumber + 52:
                apply("CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON GOODS (seller_id, name)");
            case startNumber + 53:
                apply("CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON GOODS (timestamp DESC, height DESC)");
            case startNumber + 54:
                apply("CREATE INDEX IF NOT EXISTS goods_height_id_idx ON goods (height, id)");
/* PURCHASE -------------------    */
            case startNumber + 55:
                apply("alter table PURCHASE add constraint IF NOT EXISTS PK_PURCHASE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 56:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON PURCHASE (id, height DESC)");
            case startNumber + 57:
                apply("CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON PURCHASE (buyer_id, height DESC)");
            case startNumber + 58:
                apply("CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON PURCHASE (seller_id, height DESC)");
            case startNumber + 59:
                apply("CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON PURCHASE (deadline DESC, height DESC)");
            case startNumber + 60:
                apply("CREATE INDEX IF NOT EXISTS purchase_timestamp_idx ON PURCHASE (timestamp DESC, id)");
            case startNumber + 61:
                apply("CREATE INDEX IF NOT EXISTS purchase_height_id_idx ON PURCHASE (height, id)");
/* ACCOUNT -------------------    */
            case startNumber + 62:
                apply("alter table ACCOUNT add constraint IF NOT EXISTS PK_ACCOUNT_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 63:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON ACCOUNT (id, height DESC)");
            case startNumber + 64:
                apply("CREATE INDEX IF NOT EXISTS account_active_lessee_id_idx ON ACCOUNT (active_lessee_id)");
            case startNumber + 65:
                apply("CREATE INDEX IF NOT EXISTS account_height_id_idx ON ACCOUNT (height, id)");
/* ACCOUNT_ASSET -------------------    */
            case startNumber + 66:
                apply("alter table ACCOUNT_ASSET add constraint IF NOT EXISTS PK_ACCOUNT_ASSET_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 67:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON ACCOUNT_ASSET (account_id, asset_id, height DESC)");
            case startNumber + 68:
                apply("CREATE INDEX IF NOT EXISTS account_asset_quantity_idx ON ACCOUNT_ASSET (quantity DESC)");
            case startNumber + 69:
                apply("CREATE INDEX IF NOT EXISTS account_asset_asset_id_idx ON ACCOUNT_ASSET (asset_id)");
            case startNumber + 70:
                apply("CREATE INDEX IF NOT EXISTS account_asset_height_id_idx ON ACCOUNT_ASSET (height, account_id, asset_id)");
/* ACCOUNT_GUARANTEED_BALANCE -------------------    */
            case startNumber + 71:
                apply("alter table ACCOUNT_GUARANTEED_BALANCE add constraint IF NOT EXISTS PK_ACCOUNT_GUARANTEED_BALANCE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 72:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON ACCOUNT_GUARANTEED_BALANCE "
                        + "(account_id, height DESC)");
            case startNumber + 73:
                apply("CREATE INDEX IF NOT EXISTS account_guaranteed_balance_height_idx ON ACCOUNT_GUARANTEED_BALANCE (height)");
/* PURCHASE_FEEDBACK -------------------    */
            case startNumber + 74:
                apply("alter table PURCHASE_FEEDBACK add constraint IF NOT EXISTS PK_PURCHASE_FEEDBACK_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 75:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON PURCHASE_FEEDBACK (id, height DESC)");
            case startNumber + 76:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_height_id_idx ON PURCHASE_FEEDBACK (height, id)");
/* PURCHASE_PUBLIC_FEEDBACK -------------------    */
            case startNumber + 77:
                apply("alter table PURCHASE_PUBLIC_FEEDBACK add constraint IF NOT EXISTS PK_PURCHASE_PUBLIC_FEEDBACK_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 78:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON PURCHASE_PUBLIC_FEEDBACK (id, height DESC)");
            case startNumber + 79:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_height_id_idx ON PURCHASE_PUBLIC_FEEDBACK (height, id)");
/* UNCONFIRMED_TRANSACTION -------------------    */
            case startNumber + 80:
                apply("alter table UNCONFIRMED_TRANSACTION add constraint IF NOT EXISTS PK_UNCONFIRMED_TRANSACTION_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 81:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON UNCONFIRMED_TRANSACTION (id)");
            case startNumber + 82:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON UNCONFIRMED_TRANSACTION "
                        + "(transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC)");
            case startNumber + 83:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_expiration_idx ON UNCONFIRMED_TRANSACTION (expiration DESC)");
/* ASSET_TRANSFER -------------------    */
            case startNumber + 84:
                apply("alter table ASSET_TRANSFER add constraint IF NOT EXISTS PK_ASSET_TRANSFER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 85:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_transfer_id_idx ON ASSET_TRANSFER (id)");
            case startNumber + 86:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_asset_id_idx ON ASSET_TRANSFER (asset_id, height DESC)");
            case startNumber + 87:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_sender_id_idx ON ASSET_TRANSFER (sender_id, height DESC)");
            case startNumber + 88:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_recipient_id_idx ON ASSET_TRANSFER (recipient_id, height DESC)");
            case startNumber + 89:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_height_idx ON ASSET_TRANSFER (height)");
/* TAG -------------------    */
            case startNumber + 90:
                apply("alter table TAG add constraint IF NOT EXISTS PK_TAG_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 91:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tag_tag_idx ON TAG (tag, height DESC)");
            case startNumber + 92:
                apply("CREATE INDEX IF NOT EXISTS tag_in_stock_count_idx ON TAG (in_stock_count DESC, height DESC)");
            case startNumber + 93:
                apply("CREATE INDEX IF NOT EXISTS tag_height_tag_idx ON TAG (height, tag)");
/* CURRENCY -------------------    */
            case startNumber + 94:
                apply("alter table CURRENCY add constraint IF NOT EXISTS PK_CURRENCY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 95:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_id_height_idx ON CURRENCY (id, height DESC)");
            case startNumber + 96:
                apply("CREATE INDEX IF NOT EXISTS currency_account_id_idx ON CURRENCY (account_id)");
            case startNumber + 97:
                apply("CREATE INDEX IF NOT EXISTS currency_name_idx ON CURRENCY (name_lower, height DESC)");
            case startNumber + 98:
                apply("CREATE INDEX IF NOT EXISTS currency_code_idx ON CURRENCY (code, height DESC)");
            case startNumber + 99:
                apply("CREATE INDEX IF NOT EXISTS currency_creation_height_idx ON CURRENCY (creation_height DESC)");
            case startNumber + 100:
                apply("CREATE INDEX IF NOT EXISTS currency_issuance_height_idx ON CURRENCY (issuance_height)");
            case startNumber + 101:
                apply("CREATE INDEX IF NOT EXISTS currency_height_id_idx ON CURRENCY (height, id)");
//                apply("ALTER TABLE IF EXISTS CURRENCY ALTER COLUMN IF EXISTS min_reserve_per_unit_nqt RENAME TO min_reserve_per_unit_atm");
            case startNumber + 102:
                apply(null); // apply / commit
/* ACCOUNT_CURRENCY -------------------    */
            case startNumber + 103:
                apply("alter table ACCOUNT_CURRENCY add constraint IF NOT EXISTS PK_ACCOUNT_CURRENCY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 104:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_currency_id_height_idx ON ACCOUNT_CURRENCY (account_id, currency_id, height DESC)");
            case startNumber + 105:
                apply("CREATE INDEX IF NOT EXISTS account_currency_units_idx ON account_currency (units DESC)");
            case startNumber + 106:
                apply("CREATE INDEX IF NOT EXISTS account_currency_currency_id_idx ON ACCOUNT_CURRENCY (currency_id)");
            case startNumber + 107:
                apply("CREATE INDEX IF NOT EXISTS account_currency_height_id_idx ON ACCOUNT_CURRENCY (height, account_id, currency_id)");
/* CURRENCY_FOUNDER -------------------    */
            case startNumber + 108:
                apply("alter table CURRENCY_FOUNDER add constraint IF NOT EXISTS PK_CURRENCY_FOUNDER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 109:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_founder_currency_id_idx ON CURRENCY_FOUNDER (currency_id, account_id, height DESC)");
            case startNumber + 110:
                apply("CREATE INDEX IF NOT EXISTS currency_founder_account_id_idx ON CURRENCY_FOUNDER (account_id, height DESC)");
            case startNumber + 111:
                apply("CREATE INDEX IF NOT EXISTS currency_founder_height_id_idx ON CURRENCY_FOUNDER (height, currency_id, account_id)");
/* CURRENCY_MINT -------------------    */
            case startNumber + 112:
                apply("alter table CURRENCY_MINT add constraint IF NOT EXISTS PK_CURRENCY_MINT_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 113:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_mint_currency_id_account_id_idx ON CURRENCY_MINT (currency_id, account_id, height DESC)");
            case startNumber + 114:
                apply("CREATE INDEX IF NOT EXISTS currency_mint_height_id_idx ON CURRENCY_MINT (height, currency_id, account_id)");
/* BUY_OFFER -------------------    */
            case startNumber + 115:
                apply("alter table BUY_OFFER add constraint IF NOT EXISTS PK_BUY_OFFER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 116:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_id_idx ON BUY_OFFER (id, height DESC)");
            case startNumber + 117:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_currency_id_account_id_idx ON BUY_OFFER (currency_id, account_id, height DESC)");
            case startNumber + 118:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_rate_height_idx ON BUY_OFFER (rate DESC, creation_height ASC)");
            case startNumber + 121:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_height_id_idx ON BUY_OFFER (height, id)");
/* SELL_OFFER -------------------    */
            case startNumber + 122:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_id_idx ON SELL_OFFER (id, height DESC)");
            case startNumber + 119:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_currency_id_account_id_idx ON SELL_OFFER (currency_id, account_id, height DESC)");
            case startNumber + 124:
                apply("alter table SELL_OFFER add constraint IF NOT EXISTS PK_SELL_OFFER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 120:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_height_id_idx ON SELL_OFFER (height, id)");
            case startNumber + 123:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_rate_height_idx ON SELL_OFFER (rate ASC, creation_height ASC)");
/* EXCHANGE -------------------    */
            case startNumber + 125:
                apply("alter table EXCHANGE add constraint IF NOT EXISTS PK_EXCHANGE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 126:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS exchange_offer_idx ON EXCHANGE (transaction_id, offer_id)");
            case startNumber + 127:
                apply("CREATE INDEX IF NOT EXISTS exchange_currency_id_idx ON EXCHANGE (currency_id, height DESC)");
            case startNumber + 128:
                apply("CREATE INDEX IF NOT EXISTS exchange_seller_id_idx ON EXCHANGE (seller_id, height DESC)");
            case startNumber + 129:
                apply("CREATE INDEX IF NOT EXISTS exchange_buyer_id_idx ON EXCHANGE (buyer_id, height DESC)");
            case startNumber + 130:
                apply("CREATE INDEX IF NOT EXISTS exchange_height_idx ON EXCHANGE (height)");
            case startNumber + 131:
                apply("CREATE INDEX IF NOT EXISTS exchange_height_db_id_idx ON EXCHANGE (height DESC, db_id DESC)");
/* CURRENCY_TRANSFER -------------------    */
            case startNumber + 132:
                apply("alter table CURRENCY_TRANSFER add constraint IF NOT EXISTS PK_CURRENCY_TRANSFER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 133:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_transfer_id_idx ON CURRENCY_TRANSFER (id)");
            case startNumber + 134:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_sender_id_idx ON CURRENCY_TRANSFER (sender_id, height DESC)");
            case startNumber + 135:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_recipient_id_idx ON CURRENCY_TRANSFER (recipient_id, height DESC)");
            case startNumber + 136:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_height_idx ON CURRENCY_TRANSFER (height)");
            case startNumber + 137:
/* SCAN -------------------    */
                apply("INSERT INTO scan (rescan, height, validate) VALUES (false, 0, false)");
/* CURRENCY_SUPPLY -------------------    */
            case startNumber + 138:
                apply("alter table CURRENCY_SUPPLY add constraint IF NOT EXISTS PK_CURRENCY_SUPPLY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 139:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_supply_id_height_idx ON CURRENCY_SUPPLY (id, height DESC)");
            case startNumber + 140:
                apply("CREATE INDEX IF NOT EXISTS currency_supply_height_id_idx ON CURRENCY_SUPPLY (height, id)");
//                apply("ALTER TABLE currency_supply ALTER COLUMN IF EXISTS current_reserve_per_unit_nqt RENAME TO current_reserve_per_unit_atm");
            case startNumber + 141:
                apply(null); // apply / commit
/* PUBLIC_KEY -------------------    */
            case startNumber + 142:
                apply("alter table PUBLIC_KEY add constraint IF NOT EXISTS PK_PUBLIC_KEY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 143:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS public_key_account_id_height_idx ON PUBLIC_KEY (account_id, height DESC)");
            case startNumber + 144:
                apply("CREATE INDEX IF NOT EXISTS public_key_height_idx on PUBLIC_KEY (height)");
/* VOTE -------------------    */
            case startNumber + 145:
                apply("alter table VOTE add constraint IF NOT EXISTS PK_VOTE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 146:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS vote_id_idx ON VOTE (id)");
            case startNumber + 147:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS vote_poll_id_idx ON VOTE (poll_id, voter_id)");
            case startNumber + 148:
                apply("CREATE INDEX IF NOT EXISTS vote_height_idx ON VOTE (height)");
/* POLL -------------------    */
            case startNumber + 149:
                apply("alter table POLL add constraint IF NOT EXISTS PK_POLL_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 150:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS poll_id_idx ON POLL (id)");
            case startNumber + 151:
                apply("CREATE INDEX IF NOT EXISTS poll_height_idx ON POLL (height)");
            case startNumber + 152:
                apply("CREATE INDEX IF NOT EXISTS poll_account_idx ON POLL (account_id)");
            case startNumber + 153:
                apply("CREATE INDEX IF NOT EXISTS poll_finish_height_idx ON POLL (finish_height DESC)");
/* POLL_RESULT -------------------    */
            case startNumber + 154:
                apply("alter table POLL_RESULT add constraint IF NOT EXISTS PK_POLL_RESULT_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 155:
                apply("CREATE INDEX IF NOT EXISTS poll_result_poll_id_idx ON POLL_RESULT (poll_id)");
            case startNumber + 156:
                apply("CREATE INDEX IF NOT EXISTS poll_result_height_idx ON POLL_RESULT (height)");
/* PHASING_POLL -------------------    */
            case startNumber + 157:
                apply("alter table PHASING_POLL add constraint IF NOT EXISTS PK_PHASING_POLL_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 158:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_id_idx ON PHASING_POLL (id)");
            case startNumber + 159:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_height_idx ON PHASING_POLL (height)");
            case startNumber + 160:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_account_id_idx ON PHASING_POLL (account_id, height DESC)");
            case startNumber + 161:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_holding_id_idx ON PHASING_POLL (holding_id, height DESC)");
/* PHASING_VOTE -------------------    */
            case startNumber + 162:
                apply("alter table PHASING_VOTE add constraint IF NOT EXISTS PK_PHASING_VOTE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 163:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_vote_transaction_voter_idx ON PHASING_VOTE (transaction_id, voter_id)");
            case startNumber + 164:
                apply("CREATE INDEX IF NOT EXISTS phasing_vote_height_idx ON PHASING_VOTE (height)");
/* PHASING_POLL_VOTER -------------------    */
            case startNumber + 165:
                apply("alter table PHASING_POLL_VOTER add constraint IF NOT EXISTS PK_PHASING_POLL_VOTER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 166:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_voter_transaction_voter_idx ON PHASING_POLL_VOTER (transaction_id, voter_id)");
            case startNumber + 167:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_voter_height_idx ON PHASING_POLL_VOTER (height)");
/* PHASING_POLL_RESULT -------------------    */
            case startNumber + 168:
                apply("alter table PHASING_POLL_RESULT add constraint IF NOT EXISTS PK_PHASING_POLL_RESULT_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 169:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_result_id_idx ON PHASING_POLL_RESULT (id)");
            case startNumber + 170:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_result_height_idx ON PHASING_POLL_RESULT (height)");
/* ACCOUNT_INFO -------------------    */
            case startNumber + 171:
                apply("alter table ACCOUNT_INFO add constraint IF NOT EXISTS PK_ACCOUNT_INFO_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 172:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_info_id_height_idx ON ACCOUNT_INFO (account_id, height DESC)");
            case startNumber + 173:
                apply("CREATE INDEX IF NOT EXISTS account_info_height_id_idx ON ACCOUNT_INFO (height, account_id)");
/* PRUNABLE_MESSAGE -------------------    */
            case startNumber + 174:
                apply("alter table PRUNABLE_MESSAGE add constraint IF NOT EXISTS PK_PRUNABLE_MESSAGE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 175:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS prunable_message_id_idx ON PRUNABLE_MESSAGE (id)");
            case startNumber + 176:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_transaction_timestamp_idx ON PRUNABLE_MESSAGE (transaction_timestamp DESC)");
            case startNumber + 177:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_sender_idx ON PRUNABLE_MESSAGE (sender_id)");
            case startNumber + 178:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_recipient_idx ON PRUNABLE_MESSAGE (recipient_id)");
            case startNumber + 179:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_block_timestamp_dbid_idx ON PRUNABLE_MESSAGE (block_timestamp DESC, db_id DESC)");
/* TAGGED_DATA -------------------    */
            case startNumber + 180:
                apply("alter table TAGGED_DATA add constraint IF NOT EXISTS PK_TAGGED_DATA_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 181:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_id_height_idx ON TAGGED_DATA (id, height DESC)");
            case startNumber + 182:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_expiration_idx ON TAGGED_DATA (transaction_timestamp DESC)");
            case startNumber + 183:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_account_id_height_idx ON TAGGED_DATA (account_id, height DESC)");
            case startNumber + 184:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_block_timestamp_height_db_id_idx ON TAGGED_DATA (block_timestamp DESC, height DESC, db_id DESC)");
            case startNumber + 185:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_channel_idx ON TAGGED_DATA (channel, height DESC)");
/* DATA_TAG -------------------    */
            case startNumber + 186:
                apply("alter table DATA_TAG add constraint IF NOT EXISTS PK_DATA_TAG_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 187:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS data_tag_tag_height_idx ON DATA_TAG (tag, height DESC)");
            case startNumber + 188:
                apply("CREATE INDEX IF NOT EXISTS data_tag_count_height_idx ON DATA_TAG (tag_count DESC, height DESC)");
/* TAGGED_DATA_TIMESTAMP -------------------    */
            case startNumber + 189:
                apply("alter table TAGGED_DATA_TIMESTAMP add constraint IF NOT EXISTS PK_TAGGED_DATA_TIMESTAMP_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 190:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_timestamp_id_height_idx ON TAGGED_DATA_TIMESTAMP (id, height DESC)");
            case startNumber + 191:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_timestamp_height_id_idx ON TAGGED_DATA_TIMESTAMP (height, id)");
/* ACCOUNT_LEASE -------------------    */
            case startNumber + 192:
                apply("alter table ACCOUNT_LEASE add constraint IF NOT EXISTS PK_ACCOUNT_LEASE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 193:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_lease_lessor_id_height_idx ON ACCOUNT_LEASE (lessor_id, height DESC)");
            case startNumber + 194:
                apply("CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_from_idx ON ACCOUNT_LEASE (current_leasing_height_from)");
            case startNumber + 195:
                apply("CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_to_idx ON ACCOUNT_LEASE (current_leasing_height_to)");
            case startNumber + 196:
                apply("CREATE INDEX IF NOT EXISTS account_lease_height_id_idx ON ACCOUNT_LEASE (height, lessor_id)");
/* EXCHANGE_REQUEST -------------------    */
            case startNumber + 197:
                apply("alter table EXCHANGE_REQUEST add constraint IF NOT EXISTS PK_EXCHANGE_REQUEST_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 198:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS exchange_request_id_idx ON EXCHANGE_REQUEST (id)");
            case startNumber + 199:
                apply("CREATE INDEX IF NOT EXISTS exchange_request_account_currency_idx ON EXCHANGE_REQUEST (account_id, currency_id, height DESC)");
            case startNumber + 200:
                apply("CREATE INDEX IF NOT EXISTS exchange_request_currency_idx ON EXCHANGE_REQUEST (currency_id, height DESC)");
            case startNumber + 201:
                apply("CREATE INDEX IF NOT EXISTS exchange_request_height_db_id_idx ON EXCHANGE_REQUEST (height DESC, db_id DESC)");
            case startNumber + 202:
                apply("CREATE INDEX IF NOT EXISTS exchange_request_height_idx ON EXCHANGE_REQUEST (height)");
/* ACCOUNT_LEDGER -------------------    */
            case startNumber + 203:
                apply("alter table ACCOUNT_LEDGER add constraint IF NOT EXISTS PK_ACCOUNT_LEDGER_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 204:
                apply("CREATE INDEX IF NOT EXISTS account_ledger_id_idx ON ACCOUNT_LEDGER (account_id, db_id)");
            case startNumber + 205:
                apply("CREATE INDEX IF NOT EXISTS account_ledger_height_idx ON ACCOUNT_LEDGER (height)");
/* TAGGED_DATA_EXTEND -------------------    */
            case startNumber + 206:
                apply("alter table TAGGED_DATA_EXTEND add constraint IF NOT EXISTS PK_TAGGED_DATA_EXTEND_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 207:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_extend_id_height_idx ON TAGGED_DATA_EXTEND (id, height DESC)");
            case startNumber + 208:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_extend_height_id_idx ON TAGGED_DATA_EXTEND (height, id)");
            case startNumber + 209:
                apply(null);
/* SHUFFLING -------------------    */
            case startNumber + 210:
                apply("alter table SHUFFLING add constraint IF NOT EXISTS PK_SHUFFLING_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 211:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS shuffling_id_height_idx ON SHUFFLING (id, height DESC)");
            case startNumber + 212:
                apply("CREATE INDEX IF NOT EXISTS shuffling_holding_id_height_idx ON SHUFFLING (holding_id, height DESC)");
            case startNumber + 213:
                apply("CREATE INDEX IF NOT EXISTS shuffling_assignee_account_id_height_idx ON SHUFFLING (assignee_account_id, height DESC)");
            case startNumber + 214:
                apply("CREATE INDEX IF NOT EXISTS shuffling_height_id_idx ON SHUFFLING (height, id)");
            case startNumber + 215:
                apply("CREATE INDEX IF NOT EXISTS shuffling_blocks_remaining_height_idx ON SHUFFLING (blocks_remaining, height DESC)");
/* SHUFFLING_PARTICIPANT -------------------    */
            case startNumber + 216:
                apply("alter table SHUFFLING_PARTICIPANT add constraint IF NOT EXISTS PK_SHUFFLING_PARTICIPANT_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 217:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS shuffling_participant_shuffling_id_account_id_idx ON SHUFFLING_PARTICIPANT "
                        + "(shuffling_id, account_id, height DESC)");
            case startNumber + 218:
                apply("CREATE INDEX IF NOT EXISTS shuffling_participant_height_idx ON SHUFFLING_PARTICIPANT (height, shuffling_id, account_id)");
/* SHUFFLING_DATA -------------------    */
            case startNumber + 219:
                apply("alter table SHUFFLING_DATA add constraint IF NOT EXISTS PK_SHUFFLING_DATA_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 220:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS shuffling_data_id_height_idx ON SHUFFLING_DATA (shuffling_id, height DESC)");
            case startNumber + 221:
/* PHASING_POLL_LINKED_TRANSACTION -------------------    */
                apply("CREATE INDEX shuffling_data_transaction_timestamp_idx ON SHUFFLING_DATA (transaction_timestamp DESC)");
            case startNumber + 222:
                apply("alter table PHASING_POLL_LINKED_TRANSACTION add constraint IF NOT EXISTS PK_PHASING_POLL_LINKED_TRANSACTION_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 223:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_linked_transaction_id_link_idx "
                        + "ON PHASING_POLL_LINKED_TRANSACTION (transaction_id, linked_transaction_id)");
            case startNumber + 224:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_linked_transaction_height_idx ON PHASING_POLL_LINKED_TRANSACTION (height)");
            case startNumber + 225:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_linked_transaction_link_id_idx "
                        + "ON PHASING_POLL_LINKED_TRANSACTION (linked_transaction_id, transaction_id)");
/* ACCOUNT_CONTROL_PHASING -------------------    */
            case startNumber + 226:
                apply("alter table ACCOUNT_CONTROL_PHASING add constraint IF NOT EXISTS PK_ACCOUNT_CONTROL_PHASING_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 227:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_control_phasing_id_height_idx ON ACCOUNT_CONTROL_PHASING (account_id, height DESC)");
            case startNumber + 228:
                apply("CREATE INDEX IF NOT EXISTS account_control_phasing_height_id_idx ON ACCOUNT_CONTROL_PHASING (height, account_id)");
/* ACCOUNT_PROPERTY -------------------    */
            case startNumber + 229:
                apply("alter table ACCOUNT_PROPERTY add constraint IF NOT EXISTS PK_ACCOUNT_PROPERTY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 230:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_property_id_height_idx ON ACCOUNT_PROPERTY (id, height DESC)");
            case startNumber + 240:
                apply("CREATE INDEX IF NOT EXISTS account_property_height_id_idx ON ACCOUNT_PROPERTY (height, id)");
            case startNumber + 241:
                apply("CREATE INDEX IF NOT EXISTS account_property_recipient_height_idx ON ACCOUNT_PROPERTY (recipient_id, height DESC)");
            case startNumber + 242:
                apply("CREATE INDEX IF NOT EXISTS account_property_setter_recipient_idx ON ACCOUNT_PROPERTY (setter_id, recipient_id)");
/* ASSET_DELETE -------------------    */
            case startNumber + 243:
                apply("alter table ASSET_DELETE add constraint IF NOT EXISTS PK_ASSET_DELETE_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 244:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_delete_id_idx ON ASSET_DELETE (id)");
            case startNumber + 245:
                apply("CREATE INDEX IF NOT EXISTS asset_delete_asset_id_idx ON ASSET_DELETE (asset_id, height DESC)");
            case startNumber + 246:
                apply("CREATE INDEX IF NOT EXISTS asset_delete_account_id_idx ON ASSET_DELETE (account_id, height DESC)");
            case startNumber + 247:
                apply("CREATE INDEX IF NOT EXISTS asset_delete_height_idx ON ASSET_DELETE (height)");
/* REFERENCED_TRANSACTION -------------------    */
            case startNumber + 248:
                apply("alter table REFERENCED_TRANSACTION add constraint IF NOT EXISTS PK_REFERENCED_TRANSACTION_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 249:
                apply("CREATE INDEX IF NOT EXISTS referenced_transaction_referenced_transaction_id_idx ON REFERENCED_TRANSACTION (referenced_transaction_id)");
/* ASSET_DIVIDEND -------------------    */
            case startNumber + 250:
                apply("alter table ASSET_DIVIDEND add constraint IF NOT EXISTS PK_ASSET_DIVIDEND_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 251:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_dividend_id_idx ON ASSET_DIVIDEND (id)");
            case startNumber + 252:
                apply("CREATE INDEX IF NOT EXISTS asset_dividend_asset_id_idx ON ASSET_DIVIDEND (asset_id, height DESC)");
            case startNumber + 253:
                apply("CREATE INDEX IF NOT EXISTS asset_dividend_height_idx ON ASSET_DIVIDEND (height)");
/* GENESIS_PUBLIC_KEY -------------------    */
            case startNumber + 254:
                apply("alter table GENESIS_PUBLIC_KEY add constraint IF NOT EXISTS PK_GENESIS_PUBLIC_KEY_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 255:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS genesis_public_key_account_id_height_idx on GENESIS_PUBLIC_KEY (account_id, height)");
            case startNumber + 256:
                apply("CREATE INDEX IF NOT EXISTS genesis_public_key_height_idx on GENESIS_PUBLIC_KEY (height)");
/* OPTION -------------------    */
            case startNumber + 257:
                apply("CREATE UNIQUE INDEX option_name_value_idx ON OPTION (name, value)");
/* SHARD -------------------    */
            case startNumber + 258:
                apply("alter table shard add constraint IF NOT EXISTS PK_SHARD_ID primary key (shard_id)"); // primary key + index
            case startNumber + 259:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS shard_height_index on SHARD (shard_height DESC, shard_id)");
/* BLOCK_INDEX -------------------    */
            case startNumber + 260:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_id_shard_id_idx ON BLOCK_INDEX (block_id, shard_id DESC)");
            case startNumber + 261:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_height_shard_id_idx ON BLOCK_INDEX (block_height, shard_id DESC)");
/* TRANSACTION_SHARD_INDEX -------------------    */
            case startNumber + 262:
                apply("ALTER TABLE TRANSACTION_SHARD_INDEX ADD CONSTRAINT IF NOT EXISTS fk_transaction_shard_index_block_id " +
                        "FOREIGN KEY (block_id) REFERENCES BLOCK_INDEX (block_id) ON DELETE CASCADE");
            case startNumber + 263:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_index_shard_1_idx ON TRANSACTION_SHARD_INDEX (transaction_id, block_id)");
/* SHARD_RECOVERY -------------------    */
            case startNumber + 264:
                apply("ALTER TABLE SHARD_RECOVERY ADD CONSTRAINT IF NOT EXISTS pk_shard_recovery_state PRIMARY KEY(shard_recovery_id)");  // primary key + index
            case startNumber + 265:
                apply("ALTER TABLE shard_recovery ADD CONSTRAINT IF NOT EXISTS shard_recovery_id_state_object_idx unique (shard_recovery_id, state)");
            case startNumber + 266:
                apply("ALTER TABLE shard_recovery ADD CONSTRAINT IF NOT EXISTS shard_recovery_id_state_object_idx unique (shard_recovery_id, state)");
            case startNumber + 267:
                return startNumber + 267;
            default:
                throw new RuntimeException("Shard ADD CONSTRAINTS/INDEXES database is inconsistent with code, at update " + nextUpdate
                        + ", probably trying to run older code on newer database");
        }
    }

    @Override
    void init(DataSource dataSource) {
        super.init(dataSource);
    }

    @Override
    protected void apply(String sql) {
        super.apply(sql);
    }

    @Override
    public String toString() {
        return "ShardAddConstraintsSchemaVersion";
    }
}
