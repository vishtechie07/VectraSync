package com.vectrasync.core;

import com.vectrasync.csv.CsvProcessor;
import com.vectrasync.csv.MappingLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsvProcessorConfig {

    @Bean
    CsvProcessor csvProcessor() {
        return new CsvProcessor();
    }

    @Bean
    MappingLogic mappingLogic() {
        return new MappingLogic();
    }
}
