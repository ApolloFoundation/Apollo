/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
abstract class AbstractSigData {
    public static final byte[] ZERO4BYTES = {0, 0, 0, 0};
    public static final String ZERO4BYTES_STRING = Convert.toHexString(ZERO4BYTES);

    public static final byte[] PUBLIC_KEY1 = Convert.parseHexString("39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152");
    public static final byte[] SIGNATURE1 = Convert.parseHexString("7ecae5825a24dedc42dd11e2239ced7ad797c6d6c9aedc3d3275204630b7e20832f9543d1063787ea1f32ab0993ea733aa46a52664755d9e54f211cdc3c5c5fd");

    public static final byte[] PUBLIC_KEY2 = Convert.parseHexString("7f7aee80a5f9b4460945ef564099c6774fd92f031e4773d9da467924d274004c");
    public static final byte[] SIGNATURE2 = Convert.parseHexString("afb31f67c1101c2eae312da60b7087b122cada3c929ac032e3b3a6079f61a905bb6787128a2c37bd76bdc8640d82762589fbe31cc145f11ba0359bd55800501d");

    public static final byte[] PUBLIC_KEY3 = Convert.parseHexString("129749b75f13a861a24619f5994dc24ed63df7bd49ce6331f68517a8c611ef1d");

}