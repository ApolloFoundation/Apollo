/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.utils;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Locale;

public class Messages {

    private static final Locale LOCALE = Locale.ROOT;

    private static Locale getLocale() {
        return LOCALE;
    }

    public static String format(final String format, final Object... args) {
        if (args == null || args.length == 0) {
            return format(format);
        }
        if (!format.contains("%")) {
            final MessageFormat formatter = new MessageFormat(format, getLocale());
            return formatter.format(args, new StringBuffer(), new FieldPosition(0)).toString();
        } else {
            return format(format, args[0]);
        }
    }

    public static String format(final String format, final Object arg) {
        return String.format(getLocale(), format, arg);
    }

    public static String format(final String format) {
        return format;
    }

}
