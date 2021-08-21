package com.faforever.client.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;

import java.net.URL;

import static com.github.nocatch.NoCatch.noCatch;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
//TODO figure out best way to refactor so that tests don't have to be lenient due to unnecessary stubbings spam
public abstract class ServiceTest {

  // TODO why are those methods here when they are used only by exactly one subclass and are not "service" specific?
  protected String getThemeFile(String file) {
    return String.format("/%s", file);
  }

  protected URL getThemeFileUrl(String file) {
    return noCatch(() -> new ClassPathResource(getThemeFile(file)).getURL());
  }

}
