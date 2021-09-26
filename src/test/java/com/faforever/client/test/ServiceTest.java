package com.faforever.client.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
//TODO figure out best way to refactor so that tests don't have to be lenient due to unnecessary stubbings spam
public abstract class ServiceTest { }
