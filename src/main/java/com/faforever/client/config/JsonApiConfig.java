package com.faforever.client.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.annotations.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Class.forName;

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
        .map(beanDefinition -> noCatch(() -> forName(beanDefinition.getBeanClassName()))).toArray(Class<?>[]::new);
  }
}
