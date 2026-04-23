package com.vectrasync.core;

import com.vectrasync.crm.CrmKind;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionStorage implements KeyResolver {

    public static final String API_KEY_ATTR = "vectrasync.apiKey";
    public static final String CRM_KEY_ATTR = "vectrasync.crmKey";
    public static final String CRM_KIND_ATTR = "vectrasync.crmKind";

    public void setApiKey(String key) {
        set(API_KEY_ATTR, key);
    }

    public void setCrmKey(String key) {
        set(CRM_KEY_ATTR, key);
    }

    public CrmKind crmKind() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) return CrmKind.MOCK;
        Object v = session.getAttribute(CRM_KIND_ATTR);
        if (v == null) return CrmKind.MOCK;
        try {
            return CrmKind.valueOf(String.valueOf(v));
        } catch (IllegalArgumentException e) {
            return CrmKind.MOCK;
        }
    }

    public void setCrmKind(CrmKind kind) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) throw new IllegalStateException("No VaadinSession on this thread");
        if (kind == null) session.setAttribute(CRM_KIND_ATTR, null);
        else session.setAttribute(CRM_KIND_ATTR, kind.name());
    }

    public Optional<String> apiKey() {
        return get(API_KEY_ATTR);
    }

    public Optional<String> crmKey() {
        return get(CRM_KEY_ATTR);
    }

    @Override
    public Optional<String> resolve() {
        return apiKey();
    }

    private static void set(String attr, String value) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) throw new IllegalStateException("No VaadinSession on this thread");
        if (value == null || value.isBlank()) session.setAttribute(attr, null);
        else session.setAttribute(attr, value);
    }

    private static Optional<String> get(String attr) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) return Optional.empty();
        Object v = session.getAttribute(attr);
        return Optional.ofNullable(v).map(Object::toString).filter(s -> !s.isBlank());
    }
}
