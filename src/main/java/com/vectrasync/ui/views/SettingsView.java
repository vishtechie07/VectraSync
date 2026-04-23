package com.vectrasync.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vectrasync.core.ModelProvider;
import com.vectrasync.core.SessionStorage;
import com.vectrasync.crm.CrmKind;
import com.vectrasync.ui.MainLayout;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings - VectraSync")
public class SettingsView extends VerticalLayout {

    private final SessionStorage session;
    private final Span providerLabel = new Span();

    public SettingsView(SessionStorage session) {
        this.session = session;
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(new H2("Settings"));
        add(new Paragraph("Keys are held in memory for this browser session only. Nothing is written to disk."));

        add(buildLlmSection());
        add(buildCrmSection());
        refreshProviderLabel();
    }

    private VerticalLayout buildLlmSection() {
        PasswordField keyField = new PasswordField("LLM API Key");
        keyField.setPlaceholder("sk-... (OpenAI) or AIza... (Gemini)");
        keyField.setWidth("420px");
        keyField.setValue(session.apiKey().orElse(""));

        Button save = new Button("Save LLM key", e -> {
            String v = keyField.getValue();
            if (v == null || v.isBlank()) {
                Notification.show("Key cannot be empty");
                return;
            }
            try {
                ModelProvider.detect(v);
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
                return;
            }
            session.setApiKey(v.trim());
            refreshProviderLabel();
            Notification.show("LLM key saved for this session");
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clear = new Button("Clear", e -> {
            session.setApiKey(null);
            keyField.clear();
            refreshProviderLabel();
            Notification.show("LLM key cleared");
        });

        VerticalLayout box = new VerticalLayout(
                new H2("LLM Provider"),
                providerLabel,
                keyField,
                new HorizontalLayout(save, clear)
        );
        box.addClassName(LumoUtility.Border.ALL);
        box.addClassName(LumoUtility.BorderRadius.MEDIUM);
        box.setPadding(true);
        box.setSpacing(true);
        box.setWidth("480px");
        return box;
    }

    private VerticalLayout buildCrmSection() {
        Paragraph attioNote = new Paragraph(
                "Attio’s website may reject public webmail (Gmail, etc.) for sign-in. "
                        + "That does not affect this app if you already have an API key from your workspace. "
                        + "Use Mock CRM below to try VectraSync without any Attio account.");

        RadioButtonGroup<CrmKind> kind = new RadioButtonGroup<>("Target CRM");
        kind.setItems(CrmKind.MOCK, CrmKind.ATTIO);
        kind.setItemLabelGenerator(k -> k == CrmKind.MOCK
                ? "Mock (in-memory, no account)"
                : "Attio (live API)");
        kind.setValue(session.crmKind());
        PasswordField crmKey = new PasswordField("Attio API Key");
        crmKey.setWidth("420px");
        crmKey.setValue(session.crmKey().orElse(""));

        Button save = new Button("Save CRM key", e -> {
            String v = crmKey.getValue();
            if (v == null || v.isBlank()) {
                Notification.show("Key cannot be empty");
                return;
            }
            session.setCrmKey(v.trim());
            Notification.show("CRM key saved for this session");
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clear = new Button("Clear", e -> {
            session.setCrmKey(null);
            crmKey.clear();
            Notification.show("CRM key cleared");
        });

        Runnable syncAttioUi = () -> {
            boolean attio = kind.getValue() == CrmKind.ATTIO;
            crmKey.setVisible(attio);
            save.setVisible(attio);
            clear.setVisible(attio);
        };
        kind.addValueChangeListener(e -> {
            session.setCrmKind(e.getValue());
            syncAttioUi.run();
        });
        syncAttioUi.run();

        VerticalLayout box = new VerticalLayout(
                new H2("CRM Connection"),
                attioNote,
                kind,
                crmKey,
                new HorizontalLayout(save, clear)
        );
        box.addClassName(LumoUtility.Border.ALL);
        box.addClassName(LumoUtility.BorderRadius.MEDIUM);
        box.setPadding(true);
        box.setSpacing(true);
        box.setWidth("480px");
        return box;
    }

    private void refreshProviderLabel() {
        providerLabel.setText(session.apiKey()
                .map(k -> {
                    try { return "Active: " + ModelProvider.detect(k); }
                    catch (Exception ex) { return "Active: invalid key"; }
                })
                .orElse("No LLM key configured"));
    }
}
