package com.vectrasync.agent;

import com.vectrasync.agent.tools.CrmTool;
import com.vectrasync.agent.tools.CsvTool;
import com.vectrasync.agent.tools.SecurityTool;
import com.vectrasync.core.ModelProvider;
import com.vectrasync.core.SecurityChecker;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.csv.CsvProcessor;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class SyncAssistantFactory {

    private final ModelProvider modelProvider;
    private final CsvProcessor csvProcessor;
    private final SecurityChecker securityChecker;

    public SyncAssistantFactory(ModelProvider modelProvider,
                                CsvProcessor csvProcessor,
                                SecurityChecker securityChecker) {
        this.modelProvider = modelProvider;
        this.csvProcessor = csvProcessor;
        this.securityChecker = securityChecker;
    }

    public SyncAssistant build(CrmClient crm, Path activeCsv) {
        return AiServices.builder(SyncAssistant.class)
                .chatLanguageModel(modelProvider.chatModel())
                .streamingChatLanguageModel(modelProvider.streamingChatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(
                        new CsvTool(csvProcessor, activeCsv),
                        new CrmTool(crm),
                        new SecurityTool(securityChecker)
                )
                .build();
    }

    public SyncAssistant build(CrmClient crm, Path activeCsv, String llmApiKey) {
        return AiServices.builder(SyncAssistant.class)
                .chatLanguageModel(modelProvider.chatModel(llmApiKey))
                .streamingChatLanguageModel(modelProvider.streamingChatModel(llmApiKey))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(
                        new CsvTool(csvProcessor, activeCsv),
                        new CrmTool(crm),
                        new SecurityTool(securityChecker)
                )
                .build();
    }
}
