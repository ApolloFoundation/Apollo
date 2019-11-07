package com.apollocurrency.aplwallet.apl.core.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Filters a carriage return character ('\r') from an underlying input stream.
 *
 * @author silaev-firstbridge
 */
public class FilterCarriageReturnCharacterInputStream extends FilterInputStream {

    public FilterCarriageReturnCharacterInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException ee) {
        }
        return i;
    }


    @Override
    public int read() throws IOException {
        int c;
        do {
            c = super.read();
        } while (c == '\r');
        return c;
    }
}