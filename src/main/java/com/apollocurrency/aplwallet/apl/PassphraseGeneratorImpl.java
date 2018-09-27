/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PassphraseGeneratorImpl implements PassphraseGenerator {
    private static final String DEFAULT_DICTIONARY_PATH = "pass-words.txt";
    private int minNumberOfWords = 10;
    private int maxNumberOfWords = 15;
    private List<String> dictionary;
    private Path dictionaryPath;

    public PassphraseGeneratorImpl(int minNumberOfWords, int maxNumberOfWords, List<String> dictionary) {
        this(minNumberOfWords, maxNumberOfWords);
        this.dictionary = dictionary;
    }

    public PassphraseGeneratorImpl(int minNumberOfWords, int maxNumberOfWords, Path dictionaryPath) {
        this(minNumberOfWords, maxNumberOfWords);
        this.dictionaryPath = dictionaryPath;
    }

    public int getMinNumberOfWords() {
        return minNumberOfWords;
    }

    public void setMinNumberOfWords(int minNumberOfWords) {
        this.minNumberOfWords = minNumberOfWords;
    }

    public int getMaxNumberOfWords() {
        return maxNumberOfWords;
    }

    public void setMaxNumberOfWords(int maxNumberOfWords) {
        this.maxNumberOfWords = maxNumberOfWords;
    }

    public List<String> getDictionary() {
        return dictionary;
    }

    public PassphraseGeneratorImpl(int minNumberOfWords, int maxNumberOfWords) {
        if (minNumberOfWords <= 0 || maxNumberOfWords <= 0) {
            throw new IllegalArgumentException("'minNumberOfWords' and 'maxNumberOfWords' should be positive");
        }

        if (minNumberOfWords > maxNumberOfWords) {
            throw new IllegalArgumentException("'minNumberOfWords' should be less or equal to 'maxNumberOfWords'");
        }
        this.minNumberOfWords = minNumberOfWords;
        this.maxNumberOfWords = maxNumberOfWords;
    }

    public PassphraseGeneratorImpl() {
    }

    @Override
    public String generate() {
        try {
            dictionary = dictionary == null ? loadDictionary() : dictionary;
            if (dictionary.size() < maxNumberOfWords) {
                throw new RuntimeException("Lack of words in dictionary: required - " + maxNumberOfWords + " but present - " + dictionary.size());
            }
            SecureRandom random = Crypto.getSecureRandom();
            int numberOfWords = random.nextInt(maxNumberOfWords - minNumberOfWords) + minNumberOfWords;
            Set<String> passphraseWords = new LinkedHashSet<>();
            while (passphraseWords.size() != numberOfWords) {
                passphraseWords.add(dictionary.get(random.nextInt(dictionary.size())));
            }
            return passphraseWords.stream().collect(Collectors.joining(" "));
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    protected List<String> loadDictionary() throws IOException {
        if (dictionaryPath == null) {
            URL dictionaryURL = getClass().getClassLoader().getResource(DEFAULT_DICTIONARY_PATH);
            if (dictionaryURL == null) {
                throw new RuntimeException("Dictionary " + DEFAULT_DICTIONARY_PATH + " is not exist");
            }
            try {
                dictionaryPath = Paths.get(dictionaryURL.toURI());
            }
            catch (URISyntaxException e) {
                throw new RuntimeException("Invalid path to dictionary", e);
            }
        }
        return Files.readAllLines(dictionaryPath);
    }
}
