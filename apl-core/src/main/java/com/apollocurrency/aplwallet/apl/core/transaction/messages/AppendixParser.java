/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;


import org.json.simple.JSONObject;

public interface AppendixParser<T extends Appendix> {
    /**
     * Try to parse appendix of {@link T} type if any from attachment json data
     * @param jsonData attachment json data, which contains different appendices and transaction type-specific attachment
     * @return {@link T} typed appendix when it was found and correctly parsed, otherwise return null
     */
    T parse(JSONObject jsonData);

    /**
     * @return class instance, which represents {@link T} Appendix class which has to be parsed
     */
    Class<T> forClass();
}
