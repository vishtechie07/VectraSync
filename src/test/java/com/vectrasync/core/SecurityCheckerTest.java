package com.vectrasync.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCheckerTest {

    private final SecurityChecker checker = new SecurityChecker();

    @Test
    void masksEmailKeepingDomain() {
        assertThat(checker.maskPii("reach ada.lovelace@example.com today"))
                .contains("a***@example.com")
                .doesNotContain("ada.lovelace@example.com");
    }

    @Test
    void masksPhoneNumbers() {
        assertThat(checker.maskPii("call +1 (415) 555-1234 now"))
                .contains("***-PHONE-***")
                .doesNotContain("555-1234");
    }

    @Test
    void masksSsn() {
        assertThat(checker.maskPii("SSN 123-45-6789"))
                .contains("***-SSN-***")
                .doesNotContain("123-45-6789");
    }

    @Test
    void masksBearerToken() {
        assertThat(checker.maskPii("Authorization: Bearer abc.def.ghi"))
                .contains("Bearer ***REDACTED***")
                .doesNotContain("abc.def.ghi");
    }

    @Test
    void masksOpenAiAndGeminiKeys() {
        assertThat(checker.maskPii("key=sk-proj-abcdef1234567890"))
                .contains("***API_KEY***")
                .doesNotContain("sk-proj-abcdef");
        assertThat(checker.maskPii("AIzaSyD_abcdefghijk0123456"))
                .contains("***API_KEY***");
    }

    @Test
    void identifiesSensitiveHeaders() {
        assertThat(checker.isSensitiveHeader("Authorization")).isTrue();
        assertThat(checker.isSensitiveHeader("X-Api-Key")).isTrue();
        assertThat(checker.isSensitiveHeader("Content-Type")).isFalse();
        assertThat(checker.maskHeaderValue("Authorization", "Bearer xyz"))
                .isEqualTo("***REDACTED***");
    }
}
