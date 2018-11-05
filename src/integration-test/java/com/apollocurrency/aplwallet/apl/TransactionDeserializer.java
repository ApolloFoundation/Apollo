/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dto.JSONTransaction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;

public class TransactionDeserializer extends StdDeserializer<JSONTransaction> {
    private static final Logger LOG = getLogger(TransactionDeserializer.class);

        public TransactionDeserializer() {
            this(null);
        }

        public TransactionDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public JSONTransaction deserialize(JsonParser jp, DeserializationContext ctxt) {
            JSONObject json;
            try {
                JsonNode node = jp.getCodec().readTree(jp);
                JSONParser parser = new JSONParser();
                json = (JSONObject) parser.parse(node.toString());
                if (json == null || json.get("type") == null) {
                    return null;
                }
                TransactionImpl transaction = TransactionImpl.newTransactionBuilder(json).build();
                int numberOfConfirmations = -1;
                try {
                    numberOfConfirmations = Integer.parseInt(json.get("confirmations").toString());
                }
                catch (Exception e) {
                    LOG.debug("Got unconfirmed transaction");
                }
                int height = Integer.parseInt(json.get("height").toString());
                transaction.setHeight(height);
                JSONTransaction jsonTransaction = new JSONTransaction(transaction);
                jsonTransaction.setNumberOfConfirmations(numberOfConfirmations);
                return jsonTransaction;
            }
            catch (Exception e) {
                //ignore
                return null;
            }
        }
}
