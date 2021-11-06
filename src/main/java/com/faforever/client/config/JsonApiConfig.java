package com.faforever.client.config;

import com.faforever.client.exception.ConfigurationException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static java.lang.Class.forName;

@Slf4j
@Configuration
public class JsonApiConfig {

  @Bean
  public ResourceConverter resourceConverter(ObjectMapper objectMapper) {
    return new ResourceConverter(objectMapper.copy().setSerializationInclusion(Include.NON_NULL),
        findJsonApiTypes("com.faforever.commons.api.dto"));
  }

  private Class<?>[] findJsonApiTypes(String scanPackage) {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Type.class));
    return provider.findCandidateComponents(scanPackage).stream()
        .map(beanDefinition -> {
          try {
            return forName(beanDefinition.getBeanClassName());
          } catch (ClassNotFoundException e) {
            throw new ConfigurationException(String.format("Class for bean `%s` not found", beanDefinition.getBeanClassName()), e);
          }
        }).toArray(Class<?>[]::new);
  }
}
