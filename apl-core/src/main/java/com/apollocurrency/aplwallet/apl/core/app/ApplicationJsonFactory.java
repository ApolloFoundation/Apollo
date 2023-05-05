package com.apollocurrency.aplwallet.apl.core.app;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a Jackson's json factory and expose only those methods that
 * the current application is in need of.
 * <p>
 * Formed as a separate bean so that to share factory instance in the application
 * and to mock in unit tests.
 *
 * @author silaev-firstbridge on 10/17/2019
 */
@Singleton
public class ApplicationJsonFactory {
    /**
     * A single globally shared factory instance as per JsonFactory's java doc.
     */
    private final JsonFactory jsonFactory;

    public ApplicationJsonFactory() {
        this.jsonFactory = new JsonFactory();
    }

    public JsonParser createParser(final InputStream is) throws IOException {
        return jsonFactory.createParser(is);
    }
}
