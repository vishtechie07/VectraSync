package com.vectrasync.agent;

import com.vectrasync.csv.MappingSuggestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MappingParserTest {

    @Test
    void parsesCleanJsonArray() {
        String json = "[{\"csvField\":\"work_email\",\"crmField\":\"email_addresses\",\"confidence\":0.92,\"reason\":\"semantic\"}]";
        List<MappingSuggestion> out = MappingParser.parse(json);
        assertThat(out).singleElement().satisfies(m -> {
            assertThat(m.csvField()).isEqualTo("work_email");
            assertThat(m.crmField()).isEqualTo("email_addresses");
            assertThat(m.confidence()).isEqualTo(0.92);
        });
    }

    @Test
    void stripsMarkdownFences() {
        String raw = """
                ```json
                [{"csvField":"name","crmField":"name","confidence":1.0,"reason":"exact"}]
                ```
                """;
        assertThat(MappingParser.parse(raw)).hasSize(1);
    }

    @Test
    void extractsArrayWhenProseLeaks() {
        String raw = "Sure! Here is the mapping:\n"
                + "[{\"csvField\":\"phone\",\"crmField\":null,\"confidence\":0.0,\"reason\":\"no match\"}]\n"
                + "Let me know if you need anything else.";
        List<MappingSuggestion> out = MappingParser.parse(raw);
        assertThat(out).singleElement().satisfies(m -> {
            assertThat(m.isMapped()).isFalse();
            assertThat(m.reason()).isEqualTo("no match");
        });
    }

    @Test
    void emptyInputFails() {
        assertThatThrownBy(() -> MappingParser.parse("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noArrayFails() {
        assertThatThrownBy(() -> MappingParser.parse("I can't help with that."))
                .isInstanceOf(IllegalStateException.class);
    }
}
