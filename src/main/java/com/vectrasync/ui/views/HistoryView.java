package com.vectrasync.ui.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vectrasync.agent.SyncHistoryService;
import com.vectrasync.agent.SyncReport;
import com.vectrasync.ui.MainLayout;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@Route(value = "history", layout = MainLayout.class)
@PageTitle("History - VectraSync")
public class HistoryView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public HistoryView(SyncHistoryService history) {
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(new H2("Sync History"));

        var reports = history.all();
        if (reports.isEmpty()) {
            add(new Paragraph("No syncs yet in this session."));
            return;
        }

        Grid<SyncReport> grid = new Grid<>(SyncReport.class, false);
        grid.addColumn(r -> FMT.format(r.startedAt())).setHeader("Started").setAutoWidth(true);
        grid.addColumn(SyncReport::fileName).setHeader("File").setAutoWidth(true);
        grid.addColumn(SyncReport::crmName).setHeader("CRM").setAutoWidth(true);
        grid.addColumn(SyncReport::total).setHeader("Rows").setAutoWidth(true);
        grid.addColumn(SyncReport::created).setHeader("Created").setAutoWidth(true);
        grid.addColumn(SyncReport::updated).setHeader("Updated").setAutoWidth(true);
        grid.addColumn(SyncReport::failed).setHeader("Failed").setAutoWidth(true);
        grid.addColumn(r -> humanDuration(Duration.between(r.startedAt(), r.finishedAt())))
                .setHeader("Elapsed").setAutoWidth(true);
        grid.setItems(reports);
        grid.setWidthFull();
        add(grid);
    }

    private static String humanDuration(Duration d) {
        long s = d.toSeconds();
        if (s < 60) return s + "s";
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
