package com.bobbuy.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerIdentityResolverTest {

    private final CustomerIdentityResolver resolver = new CustomerIdentityResolver();

    @Test
    void pureNumericIdIsResolved() {
        assertThat(resolver.resolveCustomerId("1001")).isEqualTo(Optional.of(1001L));
        assertThat(resolver.resolveCustomerId("9999")).isEqualTo(Optional.of(9999L));
        assertThat(resolver.resolveCustomerId("0")).isEqualTo(Optional.of(0L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUST-1001", "BIZ-9002", "USER-42", "cust-1001"})
    void prefixedFormatIsResolved(String principal) {
        Optional<Long> result = resolver.resolveCustomerId(principal);
        assertThat(result).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"role-injected-customer", "role-injected-agent", "anonymous", "abc", "CUST-"})
    void unrecognizedFormatReturnsEmpty(String principal) {
        assertThat(resolver.resolveCustomerId(principal)).isEmpty();
    }

    @Test
    void nullReturnsEmpty() {
        assertThat(resolver.resolveCustomerId((String) null)).isEmpty();
        assertThat(resolver.resolveCustomerId((org.springframework.security.core.Authentication) null)).isEmpty();
    }

    @Test
    void blankReturnsEmpty() {
        assertThat(resolver.resolveCustomerId("")).isEmpty();
        assertThat(resolver.resolveCustomerId("   ")).isEmpty();
    }

    @Test
    void prefixedFormatExtractsCorrectId() {
        assertThat(resolver.resolveCustomerId("BIZ-1001")).isEqualTo(Optional.of(1001L));
        assertThat(resolver.resolveCustomerId("CUST-9002")).isEqualTo(Optional.of(9002L));
    }
}
