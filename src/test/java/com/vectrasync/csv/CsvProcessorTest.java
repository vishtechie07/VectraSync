package com.vectrasync.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvProcessorTest {

    private final CsvProcessor processor = new CsvProcessor();

    @Test
    void readsHeadersInOrder(@TempDir Path dir) throws IOException {
        Path p = write(dir, "work_email,first_name,company\na@b.com,Ada,Acme\n");
        assertThat(processor.readHeaders(p)).containsExactly("work_email", "first_name", "company");
    }

    @Test
    void previewReturnsAtMostLimitRows(@TempDir Path dir) throws IOException {
        Path p = write(dir, "a,b\n1,2\n3,4\n5,6\n7,8\n");
        assertThat(processor.preview(p, 2)).hasSize(2);
    }

    @Test
    void processStreamIteratesWithoutMaterializing(@TempDir Path dir) throws IOException {
        StringBuilder sb = new StringBuilder("id,v\n");
        for (int i = 0; i < 10_000; i++) sb.append(i).append(",").append(i * 2).append('\n');
        Path p = write(dir, sb.toString());

        AtomicLong lastId = new AtomicLong(-1);
        long count = processor.processStream(p, rec -> lastId.set(Long.parseLong(rec.get("id"))));

        assertThat(count).isEqualTo(10_000);
        assertThat(lastId.get()).isEqualTo(9_999);
    }

    @Test
    void iteratorYieldsRecordsOneByOne(@TempDir Path dir) throws IOException {
        Path p = write(dir, "name\nada\nlinus\ngrace\n");
        var it = processor.iterator(p);
        List<String> names = new ArrayList<>();
        while (it.hasNext()) names.add(it.next().get("name"));
        assertThat(names).containsExactly("ada", "linus", "grace");
    }

    @Test
    void missingFileThrows(@TempDir Path dir) {
        Path p = dir.resolve("does-not-exist.csv");
        assertThatThrownBy(() -> processor.readHeaders(p)).isInstanceOf(IOException.class);
    }

    private static Path write(Path dir, String content) throws IOException {
        Path p = dir.resolve("in.csv");
        Files.writeString(p, content);
        return p;
    }
}
