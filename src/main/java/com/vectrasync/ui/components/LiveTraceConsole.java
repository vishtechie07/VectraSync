package com.vectrasync.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class LiveTraceConsole extends Composite<VerticalLayout> {

    public enum Stage { THOUGHT, ACTION, OBSERVATION, FINAL }

    private final Pre buffer = new Pre();
    private final Span caret = new Span();
    private final UI ui;

    public LiveTraceConsole() {
        this.ui = UI.getCurrent();
        getContent().addClassName("thought-console");
        getContent().setSizeFull();
        getContent().setPadding(false);
        getContent().setSpacing(false);

        buffer.getStyle().set("margin", "0").set("white-space", "pre-wrap");
        caret.setText("");
        caret.addClassName("trace-caret");

        Div wrap = new Div(buffer, caret);
        wrap.setSizeFull();
        wrap.addClassName("thought-console-body");
        getContent().add(wrap);
    }

    public void appendToken(String token) {
        if (token == null || token.isEmpty()) return;
        runOnUi(() -> buffer.setText(buffer.getText() + token));
    }

    public void appendLine(Stage stage, String text) {
        String prefix = switch (stage) {
            case THOUGHT -> "thought> ";
            case ACTION -> "action> ";
            case OBSERVATION -> "obs> ";
            case FINAL -> "final> ";
        };
        runOnUi(() -> buffer.setText(buffer.getText() + prefix + text + "\n"));
    }

    public void clear() {
        runOnUi(() -> buffer.setText(""));
    }

    public void markComplete() {
        runOnUi(() -> caret.setText(""));
    }

    public void error(String message) {
        runOnUi(() -> buffer.setText(buffer.getText() + "error> " + message + "\n"));
    }

    private void runOnUi(Runnable r) {
        if (ui == null || !ui.isAttached()) { r.run(); return; }
        ui.access(r::run);
    }
}
