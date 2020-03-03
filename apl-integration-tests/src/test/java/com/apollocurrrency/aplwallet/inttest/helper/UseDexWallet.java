package com.apollocurrrency.aplwallet.inttest.helper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ArgumentsSource(DexWalletProvider.class)
@ParameterizedTest
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDexWallet  {
}
