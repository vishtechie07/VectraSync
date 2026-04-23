package com.vectrasync.core;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SecurityChecker {

    private static final Pattern EMAIL = Pattern.compile(
            "([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)");
    private static final Pattern SSN = Pattern.compile(
            "(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)");
    private static final Pattern CREDIT_CARD = Pattern.compile(
            "(?<!\\d)(?:\\d[ -]?){13,16}(?!\\d)");
    private static final Pattern BEARER = Pattern.compile(
            "(?i)(bearer\\s+)[A-Za-z0-9._\\-]+");
    private static final Pattern API_KEY = Pattern.compile(
            "\\b(sk-[A-Za-z0-9_\\-]{10,}|AIza[A-Za-z0-9_\\-]{10,})\\b");

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "x-api-key", "cookie", "set-cookie", "proxy-authorization");

    public String maskPii(String input) {
        if (input == null || input.isEmpty()) return input;
        String out = input;
        out = maskEmails(out);
        out = SSN.matcher(out).replaceAll("***-SSN-***");
        out = PHONE.matcher(out).replaceAll("***-PHONE-***");
        out = CREDIT_CARD.matcher(out).replaceAll("***-CARD-***");
        out = BEARER.matcher(out).replaceAll("$1***REDACTED***");
        out = API_KEY.matcher(out).replaceAll("***API_KEY***");
        return out;
    }

    public boolean isSensitiveHeader(String headerName) {
        return headerName != null && SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }

    public String maskHeaderValue(String headerName, String value) {
        return isSensitiveHeader(headerName) ? "***REDACTED***" : value;
    }

    private static String maskEmails(String s) {
        Matcher m = EMAIL.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "***@" + m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
