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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public final class Convert {

    private static final char[] hexChars = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
    private static final long[] multipliers = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    public static final BigInteger two64 = new BigInteger("18446744073709551616");
    public static final long[] EMPTY_LONG = new long[0];
    public static final byte[] EMPTY_BYTE = new byte[0];
    public static final byte[][] EMPTY_BYTES = new byte[0][];
    public static final String[] EMPTY_STRING = new String[0];

    private Convert() {} //never

    public static byte[] parseHexString(String hex) {
        if (hex == null) {
            return null;
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int char1 = hex.charAt(i * 2);
            char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
            int char2 = hex.charAt(i * 2 + 1);
            char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
            if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15) {
                throw new NumberFormatException("Invalid hex number: " + hex);
            }
            bytes[i] = (byte)((char1 << 4) + char2);
        }
        return bytes;
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = hexChars[((bytes[i] >> 4) & 0xF)];
            chars[i * 2 + 1] = hexChars[(bytes[i] & 0xF)];
        }
        return String.valueOf(chars);
    }

    public static long parseUnsignedLong(String number) {
        if (number == null) {
            return 0;
        }
        return Long.parseUnsignedLong(number);
    }

    public static long parseLong(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof Long) {
            return ((Long)o);
        } else if (o instanceof String) {
            return Long.parseLong((String)o);
        } else {
            throw new IllegalArgumentException("Not a long: " + o);
        }
    }

    public static long parseAccountId(String account) {
        if (account == null || (account = account.trim()).isEmpty()) {
            return 0;
        }
        account = account.toUpperCase();
        int prefixEnd = account.indexOf('-');
        if (prefixEnd > 0) {
            return Crypto.rsDecode(account.substring(prefixEnd + 1));
        } else if (prefixEnd == 0) {
            return Long.valueOf(account);
        } else {
            return Long.parseUnsignedLong(account);
        }
    }

    public static byte[] longToBytes(long l) {
        int longSize = Long.BYTES;
        byte[] result = new byte[longSize];
        for (int i = longSize - 1; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= longSize;
        }
        return result;
    }

    public static byte[] concat(byte[]... arrs) {
        Objects.requireNonNull(arrs, "Arrs cannot be null");

        int length = 0;
        for (byte[] arr : arrs) {
            length += arr.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] arr : arrs) {
            System.arraycopy(arr, 0, result, offset, arr.length);
            offset += arr.length;
        }
        return result;
    }

    public static byte[] reverse(byte[] bytes) {
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - i - 1];
        }
        return reversed;
    }

    public static byte[] toFullHash(long id, byte[] partialHash) {
        Objects.requireNonNull(partialHash, "partialHash should not be null");
        byte[] bytes = Convert.longToBytes(id);
        byte[] firstPartOfHash = Convert.reverse(bytes); //reverse bytes according to order in Convert.fullHashToId
        return Convert.concat(firstPartOfHash, partialHash);
    }
    public static byte[] reverseSelf(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
        return bytes;
    }

    public static long bytesToLong(byte[] b) {
        int longSize = Long.BYTES;
        long result = 0;
        for (int i = 0; i < longSize; i++) {
            result <<= longSize;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    //avoid static initialization chain when call Constants.ACCOUNT_PREFIX in rsAccount method
    public static String defaultRsAccount(long accountId) {
        return  "APL-" + Crypto.rsEncode(accountId);
    }

    public static long fullHashToId(byte[] hash) {
        if (hash == null || hash.length < 8) {
            throw new IllegalArgumentException("Invalid hash: " + Arrays.toString(hash));
        }
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        return bigInteger.longValue();
    }

    public static byte[] toPartialHash(byte[] hash) {
        Objects.requireNonNull(hash, "Hash should not be null");
        if (hash.length != 32) {
            throw new IllegalArgumentException("Invalid hash length");
        }
        return Arrays.copyOfRange(hash, 8, hash.length);
    }



    public static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static byte[] emptyToNull(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        for (byte b : bytes) {
            if (b != 0) {
                return bytes;
            }
        }
        return null;
    }

    public static byte[][] nullToEmpty(byte[][] bytes) {
        return bytes == null ? EMPTY_BYTES : bytes;
    }

    public static long[] nullToEmpty(long[] array) {
        return array == null ? EMPTY_LONG : array;
    }

    public static long nullToZero(Long l) {
        return l == null ? 0 : l;
    }

    public static long[] toArray(List<Long> list) {
        return list.stream().mapToLong(x->x).toArray();
    }

    public static List<Long> toList(long[] array) {
        List<Long> result = new ArrayList<>(array.length);
        for (long elem : array) {
            result.add(elem);
        }
        return result;
    }

    public static Long[] toArray(long[] array) {
        Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static long[] toArray(Long[] array) {
        long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Set<Long> toSet(long[] array) {
        if (array == null || array.length ==0) {
            return Collections.emptySet();
        }
        Set<Long> set = new HashSet<>(array.length);
        for (long elem : array) {
            set.add(elem);
        }
        return set;
    }

    public static byte[] toBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static byte[] toBytes(String s, boolean isText) {
        return isText ? toBytes(s) : parseHexString(s);
    }

    public static String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static String toString(byte[] bytes, boolean isText) {
        return isText ? toString(bytes) : toHexString(bytes);
    }

    public static byte[] toBytes(long n) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte)(n >> (8 * i));
        }
        return bytes;
    }

    public static String readString(ByteBuffer buffer, int numBytes, int maxLength) throws NotValidException {
        if (numBytes > 3 * maxLength) {
            throw new NotValidException("Max parameter length exceeded");
        }
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return Convert.toString(bytes);
    }

    public static String truncate(String s, String replaceNull, int limit, boolean dots) {
        return s == null ? replaceNull : s.length() > limit ? (s.substring(0, dots ? limit - 3 : limit) + (dots ? "..." : "")) : s;
    }

    public static long parseStringFraction(String value, int decimals, long maxValue) {
        String[] s = value.trim().split("\\.");
        if (s.length == 0 || s.length > 2) {
            throw new NumberFormatException("Invalid number: " + value);
        }
        long wholePart = Long.parseLong(s[0]);
        if (wholePart > maxValue) {
            throw new IllegalArgumentException("Whole part of value exceeds maximum possible");
        }
        if (s.length == 1) {
            return wholePart * multipliers[decimals];
        }
        long fractionalPart = Long.parseLong(s[1]);
        if (fractionalPart >= multipliers[decimals] || s[1].length() > decimals) {
            throw new IllegalArgumentException("Fractional part exceeds maximum allowed divisibility");
        }
        for (int i = s[1].length(); i < decimals; i++) {
            fractionalPart *= 10;
        }
        return wholePart * multipliers[decimals] + fractionalPart;
    }

    public static byte[] compress(byte[] bytes) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(bytes);
            gzip.flush();
            gzip.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] uncompress(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = gzip.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, nRead);
            }
            bos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final Comparator<byte[]> byteArrayComparator = (o1, o2) -> {
        int minLength = Math.min(o1.length, o2.length);
        for (int i = 0; i < minLength; i++) {
            int result = Byte.compare(o1[i], o2[i]);
            if (result != 0) {
                return result;
            }
        }
        return o1.length - o2.length;
    };

}
