/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PassphraseGeneratorTest {
    private static final List<String> dictionary = new ArrayList<>();

    static {
        dictionary.add("one");
        dictionary.add("two");
        dictionary.add("three");
        dictionary.add("four");
        dictionary.add("five");
        dictionary.add("six");
        dictionary.add("seven");
        dictionary.add("eight");
        dictionary.add("nine");
        dictionary.add("ten");
        dictionary.add("eleven");
        dictionary.add("twelve");
    }

    @Test
    public void testGeneratePassphraseLoadDictionaryFromPath() throws IOException {
        Path dictionaryPath = Paths.get("tempDictionary");
        try {
            Files.write(dictionaryPath, dictionary);
            PassphraseGeneratorImpl passphraseGenerator = new PassphraseGeneratorImpl(5, 9, dictionaryPath.toUri().toURL());
            String passphrase = passphraseGenerator.generate();
            Assert.assertNotNull(passphrase);
            String[] words = passphrase.split(" ");
            Assert.assertTrue(words.length >= 5);
            Assert.assertTrue(words.length <= 9);
            dictionary.containsAll(Arrays.asList(words));
        }
        finally {
            Files.deleteIfExists(dictionaryPath);
        }
    }

    @Test
    public void testGeneratePassphrase() {
        PassphraseGeneratorImpl passphraseGenerator = new PassphraseGeneratorImpl(3, 8, dictionary);
        String passphrase = passphraseGenerator.generate();
        Assert.assertNotNull(passphrase);
        String[] words = passphrase.split(" ");
        Assert.assertTrue(words.length >= 3);
        Assert.assertTrue(words.length <= 8);
        dictionary.containsAll(Arrays.asList(words));
    }
}
