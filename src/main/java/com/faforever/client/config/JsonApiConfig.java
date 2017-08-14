package com.faforever.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.annotations.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Class.forName;

@Configuration
public class JsonApiConfig {

  @Bean
  public ResourceConverter resourceConverter(ObjectMapper objectMapper) {
    return new ResourceConverter(objectMapper, findJsonApiTypes("com.faforever.client.api.dto"));
  }

  private Class<?>[] findJsonApiTypes(String scanPackage) {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Type.class));
    List<Class> classes = provider.findCandidateComponents(scanPackage).stream()
        .map(beanDefinition -> noCatch(() -> (Class) forName(beanDefinition.getBeanClassName())))
        .collect(Collectors.toList());
    return classes.toArray(new Class<?>[classes.size()]);
  }
}
