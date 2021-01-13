/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;


import org.json.simple.JSONObject;

public interface AppendixParser<T extends Appendix> {
    T parse(JSONObject jsonData);
}
