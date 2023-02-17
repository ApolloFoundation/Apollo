/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NameBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Secured2FA {
    /**
     * Vault Account Name
     *
     * @return the vault account name, default value is "account"
     */
    String value() default "account";

    /**
     * Array of the supported 'passphrase' parameter names. Required only for
     * backward compatibility with 'passPhrase' and other wrong parameter naming cases
     * @return array of the possible 'passphrase' parameter names, first are more significant
     */
    String[] passphraseParamNames() default {"passphrase"};
}
