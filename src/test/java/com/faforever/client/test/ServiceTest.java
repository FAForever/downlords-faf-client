package com.faforever.client.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith({MockitoExtension.class})
public abstract class ServiceTest {

  @BeforeAll
  public static void prepare() {
    StepVerifier.setDefaultTimeout(Duration.of(5, ChronoUnit.SECONDS));
  }

}
