package com.vectrasync.agent;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@VaadinSessionScope
public class SyncHistoryService {

    private final List<SyncReport> reports = new ArrayList<>();

    public synchronized void record(SyncReport report) {
        reports.add(0, report);
        if (reports.size() > 50) reports.remove(reports.size() - 1);
    }

    public synchronized List<SyncReport> all() {
        return Collections.unmodifiableList(new ArrayList<>(reports));
    }
}
