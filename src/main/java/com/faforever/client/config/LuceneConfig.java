package com.faforever.client.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LuceneConfig {

  @Bean
  Directory directory() {
    return new RAMDirectory();
  }

  @Bean
  IndexWriterConfig indexWriter() throws IOException {
    return new IndexWriterConfig(analyzer());
  }

  @Bean
  Analyzer analyzer() {
    return new StandardAnalyzer();
  }
}
