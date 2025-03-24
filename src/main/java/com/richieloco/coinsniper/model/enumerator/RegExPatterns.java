package com.richieloco.coinsniper.model.enumerator;

import lombok.Getter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Getter
public enum RegExPatterns {
    // Initial new coin listing announcement
    NEW_COIN_LIST_PREFIX("Binance Will List"),
    // Usually follows on from "Binance Will List"...
    NEW_COIN_ADD_PREFIX("Binance Will Add"),
    // e.g. "FORM (FORM)"
    SINGLE_WORD_COIN("\\b[A-Z]+ \\([A-Z]+\\)\\b"),
    // e.g. "StraitsX USD (XUSD)"
    TWO_WORDS_COIN("\\b[A-Za-z]+ [A-Za-z]+ \\([A-Z]+\\)\\b"),
    // Suffix for "Binance Will Add"
    NEW_COIN_ADD_FOR_BUY_SUFFIX("Earn, Buy Crypto, Margin, Convert & Futures");

    // Getter for raw expression
    private final String expression;
    // Getter for compiled Pattern
    private final Pattern pattern;

    // Constructor
    RegExPatterns(String expression) {
        this.expression = expression;
        this.pattern = Pattern.compile(expression);
    }

    // Method to check if a string matches the regex
    public boolean matches(String input) {
        return pattern.matcher(input).find();
    }

    // Method to extract matches
    public String extractFirstMatch(String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group() : null;
    }
}

