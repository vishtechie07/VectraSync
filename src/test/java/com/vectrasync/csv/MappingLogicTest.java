package com.vectrasync.csv;

import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.FieldSchema.FieldType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MappingLogicTest {

    private final MappingLogic logic = new MappingLogic();

    private final List<FieldSchema> schema = List.of(
            new FieldSchema("email_addresses", FieldType.EMAIL, true),
            new FieldSchema("name", FieldType.STRING, true),
            new FieldSchema("phone_numbers", FieldType.PHONE, false),
            new FieldSchema("company_domain", FieldType.URL, false)
    );

    @Test
    void exactMatchGetsHighestConfidence() {
        var s = logic.suggest(List.of("name"), schema);
        assertThat(s).singleElement()
                .satisfies(m -> {
                    assertThat(m.crmField()).isEqualTo("name");
                    assertThat(m.confidence()).isGreaterThanOrEqualTo(0.95);
                });
    }

    @Test
    void semanticEmailMatchesEmailField() {
        var s = logic.suggest(List.of("work_email"), schema);
        assertThat(s).singleElement()
                .satisfies(m -> {
                    assertThat(m.crmField()).isEqualTo("email_addresses");
                    assertThat(m.confidence()).isGreaterThanOrEqualTo(0.5);
                });
    }

    @Test
    void unknownColumnReturnsUnmapped() {
        var s = logic.suggest(List.of("favourite_color"), schema);
        assertThat(s).singleElement()
                .satisfies(m -> {
                    assertThat(m.isMapped()).isFalse();
                    assertThat(m.reason()).contains("No confident");
                });
    }

    @Test
    void crmFieldUsedAtMostOnce() {
        var s = logic.suggest(List.of("email", "work_email"), schema);
        long mapped = s.stream().filter(MappingSuggestion::isMapped)
                .map(MappingSuggestion::crmField)
                .distinct().count();
        long mappedTotal = s.stream().filter(MappingSuggestion::isMapped).count();
        assertThat(mapped).isEqualTo(mappedTotal);
    }
}
