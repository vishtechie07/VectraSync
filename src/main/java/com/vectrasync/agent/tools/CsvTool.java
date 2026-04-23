package com.vectrasync.agent.tools;

import com.vectrasync.csv.CsvProcessor;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CsvTool {

    private final CsvProcessor processor;
    private final Path activeFile;

    public CsvTool(CsvProcessor processor, Path activeFile) {
        this.processor = processor;
        this.activeFile = activeFile;
    }

    @Tool("Returns the column headers from the currently loaded CSV file.")
    public List<String> getCsvHeaders() {
        try {
            return processor.readHeaders(activeFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV headers: " + e.getMessage(), e);
        }
    }

    @Tool("Returns up to N sample rows from the loaded CSV so you can inspect value formats.")
    public List<Map<String, String>> previewCsvRows(
            @P("How many rows to preview (1-10).") int limit) {
        try {
            return processor.preview(activeFile, Math.max(1, Math.min(limit, 10)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to preview CSV: " + e.getMessage(), e);
        }
    }
}
