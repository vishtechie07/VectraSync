package com.vectrasync.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class CsvProcessor {

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

    public List<String> readHeaders(Path path) throws IOException {
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = FORMAT.parse(r)) {
            return new ArrayList<>(parser.getHeaderMap().keySet());
        }
    }

    public List<String> readHeaders(InputStream in) throws IOException {
        try (CSVParser parser = FORMAT.parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))) {
            return new ArrayList<>(parser.getHeaderMap().keySet());
        }
    }

    public List<Map<String, String>> preview(Path path, int limit) throws IOException {
        List<Map<String, String>> out = new ArrayList<>();
        processStream(path, rec -> {
            if (out.size() < limit) out.add(rec.toMap());
        });
        return out;
    }

    public long processStream(Path path, Consumer<CSVRecord> handler) throws IOException {
        Objects.requireNonNull(handler, "handler");
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = FORMAT.parse(r)) {
            long count = 0;
            for (CSVRecord record : parser) {
                handler.accept(record);
                count++;
            }
            return count;
        }
    }

    public long processStream(InputStream in, Consumer<CSVRecord> handler) throws IOException {
        Objects.requireNonNull(handler, "handler");
        try (CSVParser parser = FORMAT.parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))) {
            long count = 0;
            for (CSVRecord record : parser) {
                handler.accept(record);
                count++;
            }
            return count;
        }
    }

    public Iterator<CSVRecord> iterator(Path path) throws IOException {
        Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        CSVParser parser = FORMAT.parse(r);
        Iterator<CSVRecord> it = parser.iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() {
                boolean has = it.hasNext();
                if (!has) closeQuietly(parser, r);
                return has;
            }
            @Override public CSVRecord next() { return it.next(); }
        };
    }

    private static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            try { if (c != null) c.close(); } catch (Exception ignored) { }
        }
    }
}
