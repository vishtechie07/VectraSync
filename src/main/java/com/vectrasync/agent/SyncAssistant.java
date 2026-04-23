package com.vectrasync.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface SyncAssistant {

    @SystemMessage({
        "You are VectraSync, a CRM-agnostic data integration agent.",
        "You do NOT know which CRM you are communicating with. Discover its shape at runtime.",
        "Workflow:",
        "  1. Call getCrmSchema() to learn the target CRM's fields.",
        "  2. Call getCsvHeaders() and (optionally) previewCsvRows() to inspect the source CSV.",
        "  3. Produce a JSON array of MappingSuggestion objects: {csvField, crmField, confidence, reason}.",
        "Rules:",
        "  - Only propose crmField values that exist in the schema.",
        "  - confidence is a number between 0.0 and 1.0.",
        "  - If a CSV column has no confident match, set crmField to null and explain why in reason.",
        "  - Never invent, hallucinate, or hard-code CRM-specific field names.",
        "  - Return ONLY the JSON array. No prose, no markdown code fences, no surrounding text.",
        "Example output:",
        "[{\"csvField\":\"work_email\",\"crmField\":\"email_addresses\",\"confidence\":0.95,\"reason\":\"semantic match\"}]"
    })
    String proposeMapping(@UserMessage String instruction);

    @SystemMessage({
        "You are VectraSync's reasoning stream. Think out loud using the ReAct pattern:",
        "Thought -> Action -> Observation -> Final Answer.",
        "Use the available tools to discover the CRM schema and CSV shape before concluding.",
        "Keep each line short and prefixed with its stage."
    })
    TokenStream streamReasoning(@UserMessage String instruction);
}
