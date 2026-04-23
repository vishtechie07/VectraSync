package com.vectrasync.agent.tools;

import com.vectrasync.core.SecurityChecker;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class SecurityTool {

    private final SecurityChecker checker;

    public SecurityTool(SecurityChecker checker) {
        this.checker = checker;
    }

    @Tool("Masks any PII (emails, phones, SSNs, credit cards, API keys) in a string before it is logged or displayed.")
    public String maskPii(@P("Text that may contain PII.") String text) {
        return checker.maskPii(text);
    }
}
