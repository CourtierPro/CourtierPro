package com.example.courtierprobackend.shared.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostalCodeUtilTest {
    @Test
    void normalize_WithInvalidFormat_ReturnsUppercaseTrimmed() {
        assertThat(PostalCodeUtil.normalize("foo123")).isEqualTo("FOO 123");
        assertThat(PostalCodeUtil.normalize("A1A1A1X")).isEqualTo("A1A1A1X");
        assertThat(PostalCodeUtil.normalize("A1A 1A1X")).isEqualTo("A1A 1A1X");
        assertThat(PostalCodeUtil.normalize("A1A-1A1X")).isEqualTo("A1A-1A1X");
    }

    @Test
    void normalize_WithExtraWhitespaceAndHyphen_Normalizes() {
        assertThat(PostalCodeUtil.normalize("  a1a-1a1  ")).isEqualTo("A1A 1A1");
        assertThat(PostalCodeUtil.normalize("A1A   1A1")).isEqualTo("A1A 1A1");
    }

    @Test
    void isValid_WithHyphenAndSpace_ReturnsTrue() {
        assertThat(PostalCodeUtil.isValid("A1A-1A1")).isTrue();
        assertThat(PostalCodeUtil.isValid("A1A 1A1")).isTrue();
    }

    @Test
    void isValid_WithMixedCaseAndWhitespace_ReturnsFalse() {
        assertThat(PostalCodeUtil.isValid(" a1a 1a1 ")).isFalse();
        assertThat(PostalCodeUtil.isValid("A1A-1A1 ")).isFalse();
    }

    @Test
    void isValid_WithLongerInvalidStrings_ReturnsFalse() {
        assertThat(PostalCodeUtil.isValid("A1A1A1X")).isFalse();
        assertThat(PostalCodeUtil.isValid("A1A 1A1X")).isFalse();
        assertThat(PostalCodeUtil.isValid("A1A-1A1X")).isFalse();
    }

    @Test
    void normalize_WithNull_ReturnsNull() {
        assertThat(PostalCodeUtil.normalize(null)).isNull();
    }

    @Test
    void normalize_WithEmptyString_ReturnsNull() {
        assertThat(PostalCodeUtil.normalize("")).isNull();
    }

    @Test
    void normalize_WithBlankString_ReturnsNull() {
        assertThat(PostalCodeUtil.normalize("   ")).isNull();
    }

    @Test
    void normalize_WithSixCharacters_AddsSpace() {
        assertThat(PostalCodeUtil.normalize("H1A1A1")).isEqualTo("H1A 1A1");
    }

    @Test
    void normalize_WithLowercase_ConvertsToUppercase() {
        assertThat(PostalCodeUtil.normalize("h1a1a1")).isEqualTo("H1A 1A1");
    }

    @Test
    void normalize_WithExistingSpace_PreservesFormat() {
        assertThat(PostalCodeUtil.normalize("H1A 1A1")).isEqualTo("H1A 1A1");
    }

    @Test
    void normalize_WithHyphen_ConvertsToSpace() {
        assertThat(PostalCodeUtil.normalize("H1A-1A1")).isEqualTo("H1A 1A1");
    }

    @Test
    void normalize_WithMixedCase_NormalizesCorrectly() {
        assertThat(PostalCodeUtil.normalize("h1A 1a1")).isEqualTo("H1A 1A1");
    }

    @Test
    void normalize_WithLeadingTrailingSpaces_TrimsAndNormalizes() {
        assertThat(PostalCodeUtil.normalize("  H1A1A1  ")).isEqualTo("H1A 1A1");
    }

    @Test
    void isValid_WithValidFormat_ReturnsTrue() {
        assertThat(PostalCodeUtil.isValid("H1A 1A1")).isTrue();
        assertThat(PostalCodeUtil.isValid("H1A1A1")).isTrue();
        assertThat(PostalCodeUtil.isValid("H1A-1A1")).isTrue();
        assertThat(PostalCodeUtil.isValid("h1a1a1")).isTrue();
    }

    @Test
    void isValid_WithNull_ReturnsFalse() {
        assertThat(PostalCodeUtil.isValid(null)).isFalse();
    }

    @Test
    void isValid_WithEmptyString_ReturnsFalse() {
        assertThat(PostalCodeUtil.isValid("")).isFalse();
    }

    @Test
    void isValid_WithInvalidFormat_ReturnsFalse() {
        assertThat(PostalCodeUtil.isValid("12345")).isFalse();
        assertThat(PostalCodeUtil.isValid("H1A")).isFalse();
        assertThat(PostalCodeUtil.isValid("ABCDEF")).isFalse();
    }
}
