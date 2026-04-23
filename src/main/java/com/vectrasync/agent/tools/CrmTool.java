package com.vectrasync.agent.tools;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.UpsertResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Map;

public class CrmTool {

    private final CrmClient crm;

    public CrmTool(CrmClient crm) {
        this.crm = crm;
    }

    @Tool("Returns the target CRM's field schema: name, type, required. Call before proposing mappings.")
    public List<FieldSchema> getCrmSchema() {
        return crm.getSchema();
    }

    @Tool("Upserts a contact into the target CRM using the already-mapped fields.")
    public UpsertResult upsertContact(
            @P("Field values mapped to CRM field names (e.g. email_addresses, name).") Map<String, Object> fields) {
        return crm.upsertContact(new Contact(fields));
    }
}
