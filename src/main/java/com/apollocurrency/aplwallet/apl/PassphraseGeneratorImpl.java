/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PassphraseGeneratorImpl implements PassphraseGenerator {
    private static final String DEFAULT_DICTIONARY_PATH = "pass-words.txt";
    private int minNumberOfWords = 10;
    private int maxNumberOfWords = 15;
    private List<String> dictionary;
    private URL dictionaryURL;

    public PassphraseGeneratorImpl(int minNumberOfWords, int maxNumberOfWords, List<String> dictionary) {
        this(minNumberOfWords, maxNumberOfWords);
        this.dictionary = dictionary;
    }

    public PassphraseGeneratorImpl(int minNumberOfWords, int maxNumberOfWords, URL dictionaryURL) {
        this(minNumberOfWords, maxNumberOfWords);
        this.dictionaryURL = dictionaryURL;
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
        if (dictionaryURL == null) {
            dictionaryURL = getClass().getClassLoader().getResource(DEFAULT_DICTIONARY_PATH);
            if (dictionaryURL == null) {
                throw new RuntimeException("Dictionary " + DEFAULT_DICTIONARY_PATH + " is not exist");
            }
        }
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(dictionaryURL.openStream()))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                words.add(line);
            }
        }
        return words;
    }
}
