package com.vectrasync.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vectrasync.agent.MappingParser;
import com.vectrasync.agent.SyncAssistant;
import com.vectrasync.agent.SyncAssistantFactory;
import com.vectrasync.agent.SyncHistoryService;
import com.vectrasync.agent.SyncReport;
import com.vectrasync.core.SecurityChecker;
import com.vectrasync.core.SessionStorage;
import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.CrmKind;
import com.vectrasync.crm.HttpStatusException;
import com.vectrasync.crm.UpsertResult;
import com.vectrasync.crm.attio.AttioClient;
import com.vectrasync.crm.mock.MockCrmClient;
import com.vectrasync.crm.resilience.RateLimitedCrmClient;
import com.vectrasync.csv.CsvProcessor;
import com.vectrasync.csv.MappingSuggestion;
import com.vectrasync.ui.MainLayout;
import com.vectrasync.ui.components.LiveTraceConsole;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Sync - VectraSync")
public class SyncView extends VerticalLayout {

    private final SessionStorage session;
    private final SyncAssistantFactory assistantFactory;
    private final CsvProcessor csvProcessor;
    private final SecurityChecker securityChecker;
    private final SyncHistoryService history;
    private final MockCrmClient mockCrmClient;

    private final FileBuffer buffer = new FileBuffer();
    private final Upload upload = new Upload(buffer);
    private final Paragraph uploadSummary = new Paragraph("No file selected.");
    private final Grid<MappingSuggestion> mappingGrid = new Grid<>(MappingSuggestion.class, false);
    private final Button proposeBtn = new Button("Propose Mapping");
    private final Button executeBtn = new Button("Execute Sync");
    private final ProgressBar progress = new ProgressBar();
    private final Span progressLabel = new Span("0 / 0");
    private final Span counters = new Span();
    private final LiveTraceConsole trace = new LiveTraceConsole();

    private Path activeCsv;
    private List<MappingSuggestion> mappings = new ArrayList<>();

    public SyncView(SessionStorage session,
                    SyncAssistantFactory assistantFactory,
                    CsvProcessor csvProcessor,
                    SecurityChecker securityChecker,
                    SyncHistoryService history,
                    MockCrmClient mockCrmClient) {
        this.session = session;
        this.assistantFactory = assistantFactory;
        this.csvProcessor = csvProcessor;
        this.securityChecker = securityChecker;
        this.history = history;
        this.mockCrmClient = mockCrmClient;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Sync Wizard"));
        add(buildUploadStep());
        add(buildMappingStep());
        add(buildExecuteStep());
        add(new H2("Live Trace"));
        add(trace);
    }

    private VerticalLayout buildUploadStep() {
        upload.setAcceptedFileTypes("text/csv", ".csv");
        upload.setMaxFiles(1);
        upload.addSucceededListener(e -> onUploadSucceeded(e.getFileName()));

        VerticalLayout v = card("1. Upload CSV", upload, uploadSummary);
        return v;
    }

    private VerticalLayout buildMappingStep() {
        mappingGrid.addColumn(MappingSuggestion::csvField).setHeader("CSV column").setAutoWidth(true);
        mappingGrid.addColumn(s -> s.crmField() == null ? "—" : s.crmField()).setHeader("CRM field").setAutoWidth(true);
        mappingGrid.addColumn(new NumberRenderer<>(MappingSuggestion::confidence,
                NumberFormat.getPercentInstance(Locale.US))).setHeader("Confidence").setAutoWidth(true);
        mappingGrid.addColumn(MappingSuggestion::reason).setHeader("Reason").setFlexGrow(1);
        mappingGrid.setAllRowsVisible(true);
        mappingGrid.setWidthFull();

        proposeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        proposeBtn.setEnabled(false);
        proposeBtn.addClickListener(e -> runProposeMapping());

        VerticalLayout v = card("2. AI Mapping Preview", proposeBtn, mappingGrid);
        return v;
    }

    private VerticalLayout buildExecuteStep() {
        executeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        executeBtn.setEnabled(false);
        executeBtn.addClickListener(e -> runExecute());

        progress.setWidth("480px");
        progress.setValue(0);
        counters.setText("");

        VerticalLayout v = card("3. Execute Sync",
                executeBtn,
                new HorizontalLayout(progress, progressLabel),
                counters);
        return v;
    }

    private VerticalLayout card(String title, com.vaadin.flow.component.Component... children) {
        VerticalLayout v = new VerticalLayout();
        v.add(new H2(title));
        for (var c : children) v.add(c);
        v.setPadding(true);
        v.setSpacing(true);
        v.addClassName(LumoUtility.Border.ALL);
        v.addClassName(LumoUtility.BorderRadius.MEDIUM);
        v.setWidthFull();
        return v;
    }

    private void onUploadSucceeded(String fileName) {
        try {
            Path tmp = Files.createTempFile("vectrasync-", "-" + sanitize(fileName));
            Files.copy(buffer.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);
            this.activeCsv = tmp;
            long bytes = Files.size(tmp);
            uploadSummary.setText(fileName + " (" + bytes + " bytes) ready.");
            proposeBtn.setEnabled(true);
            trace.appendLine(LiveTraceConsole.Stage.OBSERVATION,
                    "CSV uploaded: " + fileName + " (" + bytes + " bytes)");
        } catch (IOException ex) {
            Notification.show("Upload failed: " + ex.getMessage());
        }
    }

    private void runProposeMapping() {
        if (activeCsv == null) { Notification.show("Upload a CSV first."); return; }
        if (session.crmKind() == CrmKind.ATTIO && session.crmKey().isEmpty()) {
            Notification.show("Set your Attio API key in Settings, or switch to Mock CRM.");
            return;
        }
        if (session.apiKey().isEmpty()) { Notification.show("Set your LLM key in Settings."); return; }

        final String llmApiKey = session.apiKey().orElseThrow();

        proposeBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        trace.appendLine(LiveTraceConsole.Stage.THOUGHT, "Building SyncAssistant and discovering CRM schema...");

        UI ui = UI.getCurrent();
        CrmClient crm = buildCrm();
        Path csv = activeCsv;

        Thread.ofVirtual().name("vectrasync-propose").start(() -> {
            try {
                SyncAssistant assistant = assistantFactory.build(crm, csv, llmApiKey);
                trace.appendLine(LiveTraceConsole.Stage.ACTION, "Calling assistant.proposeMapping()");
                String rawJson = assistant.proposeMapping(
                        "Propose a mapping between the uploaded CSV headers and the target CRM schema.");
                List<MappingSuggestion> result = MappingParser.parse(rawJson);
                ui.access(() -> {
                    this.mappings = result;
                    mappingGrid.setItems(result);
                    executeBtn.setEnabled(!result.isEmpty());
                    proposeBtn.setEnabled(true);
                    trace.appendLine(LiveTraceConsole.Stage.FINAL,
                            "Mapping produced: " + result.size() + " columns");
                });
            } catch (Exception ex) {
                ui.access(() -> {
                    proposeBtn.setEnabled(true);
                    trace.error(securityChecker.maskPii(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
                    Notification.show("Mapping failed: " + ex.getMessage());
                });
            }
        });
    }

    private void runExecute() {
        if (activeCsv == null || mappings.isEmpty()) return;

        executeBtn.setEnabled(false);
        trace.appendLine(LiveTraceConsole.Stage.ACTION, "Executing sync (approved).");

        UI ui = UI.getCurrent();
        CrmClient crm = buildCrm();
        Path csv = activeCsv;
        List<MappingSuggestion> map = List.copyOf(mappings);
        String fileName = activeCsv.getFileName().toString();
        Instant started = Instant.now();

        Thread.ofVirtual().name("vectrasync-execute").start(() -> {
            AtomicLong total = new AtomicLong();
            AtomicLong created = new AtomicLong();
            AtomicLong updated = new AtomicLong();
            AtomicLong failed = new AtomicLong();
            try {
                long count = csvProcessor.processStream(csv, record -> {
                    long n = total.incrementAndGet();
                    Map<String, Object> fields = toCrmFields(record, map);
                    try {
                        UpsertResult r = crm.upsertContact(new Contact(fields));
                        switch (r.status()) {
                            case CREATED -> created.incrementAndGet();
                            case UPDATED -> updated.incrementAndGet();
                            case FAILED -> failed.incrementAndGet();
                            case SKIPPED -> { }
                        }
                        ui.access(() -> updateProgress(n, created.get(), updated.get(), failed.get()));
                    } catch (HttpStatusException ex) {
                        failed.incrementAndGet();
                        ui.access(() -> trace.error("HTTP " + ex.status() + ": "
                                + securityChecker.maskPii(String.valueOf(ex.body()))));
                    } catch (Exception ex) {
                        failed.incrementAndGet();
                        ui.access(() -> trace.error(securityChecker.maskPii(ex.getMessage())));
                    }
                });
                SyncReport report = new SyncReport(started, Instant.now(), fileName, crm.name(),
                        count, created.get(), updated.get(), failed.get());
                history.record(report);
                ui.access(() -> {
                    trace.appendLine(LiveTraceConsole.Stage.FINAL,
                            "Sync complete: " + report.created() + " created, "
                                    + report.updated() + " updated, " + report.failed() + " failed.");
                    trace.markComplete();
                    executeBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                ui.access(() -> {
                    trace.error(securityChecker.maskPii(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
                    executeBtn.setEnabled(true);
                });
            }
        });
    }

    private void updateProgress(long processed, long created, long updated, long failed) {
        progressLabel.setText(processed + " processed");
        counters.setText(String.format("Created: %d | Updated: %d | Failed: %d", created, updated, failed));
        progress.setIndeterminate(true);
    }

    private CrmClient buildCrm() {
        CrmClient base = switch (session.crmKind()) {
            case MOCK -> mockCrmClient;
            case ATTIO -> new AttioClient(session.crmKey().orElseThrow());
        };
        return RateLimitedCrmClient.wrap(base, 5);
    }

    private static Map<String, Object> toCrmFields(CSVRecord record, List<MappingSuggestion> map) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (MappingSuggestion s : map) {
            if (!s.isMapped()) continue;
            if (!record.isMapped(s.csvField())) continue;
            String v = record.get(s.csvField());
            if (v != null && !v.isBlank()) out.put(s.crmField(), v.trim());
        }
        return out;
    }

    private static String sanitize(String s) {
        return s == null ? "upload.csv" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
