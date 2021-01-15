package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.vault.service.KMSv1;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.INCORRECT_VALUE;
import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.MISSING_PARAM_LIST;

@Singleton
public class AccountParametersParser {
    public static final String SENDER_PARAM_NAME = "sender";
    public static final String DEADLINE_PARAM_NAME = "deadline";
    public static final String AMOUNT_PARAM_NAME = "amountATM";
    public static final String FEE_PARAM_NAME = "feeATM";
    public static final String RECIPIENT_PARAM_NAME = "recipientId";
    public static final String PUBLIC_KEY_PARAM_NAME = "recipientId";

    private final ElGamalEncryptor elGamal;
    private final AccountService accountService;
    private final KMSv1 kmSv1;

    @Inject
    public AccountParametersParser(AccountService accountService, ElGamalEncryptor elGamalEncryptor, KMSv1 kmSv1) {
        this.elGamal = elGamalEncryptor;
        this.accountService = accountService;
        this.kmSv1 = kmSv1;
    }

    public static long getAccountId(HttpServletRequest req, boolean isMandatory) {
        return getAccountId(req, "account", isMandatory);
    }

    public static long getAccountId(HttpServletRequest req, String name, boolean isMandatory) {
        return getAccountId(Convert.emptyToNull(req.getParameter(name)), name, isMandatory);
    }

    public static long getAccountId(String paramValue, String name, boolean isMandatory) {
        if (paramValue == null) {
            if (isMandatory) {
                throw new RestParameterException(ApiErrors.MISSING_PARAM, name);
            }
            return 0;
        }
        return parseAccountId(paramValue, name);
    }

    private static long parseAccountId(String accountParam, String name) {
        try {
            long value = Convert.parseAccountId(accountParam);
            if (value == 0) {
                throw new RestParameterException(INCORRECT_VALUE, name, accountParam);
            }
            return value;
        } catch (RuntimeException e) {
            throw new RestParameterException(INCORRECT_VALUE, name, accountParam);
        }
    }

    public static String getStringParameter(HttpServletRequest req, String name, boolean isMandatory) {
        String parameter = Convert.emptyToNull(req.getParameter(name));
        if (parameter == null && isMandatory) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM, name);
        }
        return parameter;
    }

    public byte[] getKeySeed(HttpServletRequest req, long senderId, boolean isMandatory) {
        byte[] secretBytes = getSecretBytes(req, senderId, isMandatory);
        return secretBytes == null ? null : Crypto.getKeySeed(secretBytes);
    }


    public byte[] getSecretBytes(HttpServletRequest req, long senderId, boolean isMandatory) {
        String secretPhrase = getSecretPhrase(req, false);
        if (secretPhrase != null) {
            return Convert.toBytes(secretPhrase);
        }
        String passphrase = Convert.emptyToNull(getPassphrase(req, false));
        if (passphrase != null) {
            return getKeySeed(passphrase, senderId);
        }
        if (isMandatory) {
            throw new RestParameterException(MISSING_PARAM_LIST, "secretPhrase, passphrase + accountId");
        }
        return null;
    }


    private byte[] getKeySeed(String passphrase, long accountId) {
        String seed = kmSv1.getAplKeySeed(accountId, passphrase);

        if (seed == null) {
            throw new RestParameterException(ApiErrors.BAD_CREDENTIALS, " account id or passphrase are not valid");
        }

        return Convert.parseHexString(seed);
    }

    public Account getSenderAccount(HttpServletRequest req, String accountName) {
        String accountParam = accountName == null ? "sender" : accountName;
        long accountId = getAccountId(req, accountParam, false);
        byte[] publicKey = getPublicKey(req, accountId);
        if (publicKey == null) {
            throw new RestParameterException(ApiErrors.UNKNOWN_VALUE, "public key", "null");
        }
        Account account = accountService.getAccount(publicKey);
        if (account == null) {
            throw new RestParameterException(ApiErrors.UNKNOWN_VALUE, "account", "publicKey:" + Convert.toHexString(publicKey));
        }
        account.setPublicKey(new PublicKey(accountId, publicKey, account.getPublicKey().getHeight()));
        return account;
    }

    public String getSecretPhrase(HttpServletRequest req, boolean isMandatory) {
        return getSecretPhrase(req, null, isMandatory);
    }

    public String getSecretPhrase(HttpServletRequest req, String secretPhraseParamName, boolean isMandatory) {
        if (StringUtils.isBlank(secretPhraseParamName)) {
            secretPhraseParamName = "secretPhrase";
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter(secretPhraseParamName));
        if (secretPhrase == null && isMandatory) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM, secretPhraseParamName);
        }
        if (secretPhrase != null) {
            return elGamal.elGamalDecrypt(secretPhrase);
        }
        return null;

    }

    public byte[] getPublicKey(HttpServletRequest req) {
        return getPublicKey(req, null);
    }

    public byte[] getPublicKey(HttpServletRequest req, String prefix) {
        return getPublicKey(req, prefix, 0);
    }

    public byte[] getPublicKey(HttpServletRequest req, long accountId) {
        return getPublicKey(req, null, accountId);
    }

    public byte[] getPublicKey(HttpServletRequest request, String prefix, long accountId) {
        return getPublicKey(request, prefix, accountId, true);
    }

    public byte[] getPublicKey(HttpServletRequest req, String prefix, long accountId, boolean isMandatory) {
        String secretPhraseParam = prefix == null ? "secretPhrase" : (prefix + "SecretPhrase");
        String publicKeyParam = prefix == null ? "publicKey" : (prefix + "PublicKey");
        String passphraseParam = prefix == null ? "passphrase" : (prefix + "Passphrase");
        String secretPhrase = getSecretPhrase(req, secretPhraseParam, false);
        if (secretPhrase == null) {
            try {
                byte[] publicKey = Convert.parseHexString(Convert.emptyToNull(req.getParameter(publicKeyParam)));
                if (publicKey == null) {
                    String passphrase = Convert.emptyToNull(getPassphrase(req, passphraseParam, false));
                    if (accountId == 0 || passphrase == null) {
                        if (isMandatory) {
                            throw new RestParameterException(ApiErrors.MISSING_PARAM_LIST, secretPhraseParam + "," + publicKeyParam + "," + passphraseParam);
                        }
                    } else {

                        byte[] keySeed = getKeySeed(passphrase, accountId);
                        return Crypto.getPublicKey(keySeed);
                    }
                } else {

                    if (!Crypto.isCanonicalPublicKey(publicKey)) {
                        if (isMandatory) {
                            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, publicKeyParam);
                        }
                    } else {
                        return publicKey;
                    }
                }
            } catch (RestParameterException e) {
                throw e;
            } catch (RuntimeException e) {
                if (isMandatory) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, publicKeyParam);
                }
            }
        } else {
            return Crypto.getPublicKey(secretPhrase);
        }
        return null;
    }

    public String getPassphrase(HttpServletRequest req, boolean isMandatory) {
        String secretPhrase = getStringParameter(req, "passphrase", isMandatory);
        return elGamal.elGamalDecrypt(secretPhrase);
    }

    public String getPassphrase(String passphrase, boolean isMandatory) {
        if (StringUtils.isBlank(passphrase) && isMandatory) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM, "passphrase");
        }
        return elGamal.elGamalDecrypt(passphrase);
    }

    public String getPassphrase(HttpServletRequest req, String parameterName, boolean isMandatory) {
        String secretPhrase = getStringParameter(req, parameterName, isMandatory);
        return elGamal.elGamalDecrypt(secretPhrase);
    }


    public byte[] getPublicKey(HttpServletRequest request, boolean isMandatory) {
        return getPublicKey(request, null, 0, isMandatory);
    }

    public Account getSenderAccount(HttpServletRequest req) {
        return getSenderAccount(req, null);
    }
}
