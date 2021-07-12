package com.faforever.client.util;

import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcurrentUtilTest extends ServiceTest {
  
  @Test
  public void testUnwrapIfCompletionException() throws InterruptedException, ExecutionException {
    RuntimeException exception = new FakeTestException();
    Function<Throwable, Void> handler = throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      assertEquals(exception, throwable);
      return null;
    };
    CompletableFuture.failedFuture(exception).exceptionally(handler).get();
    CompletableFuture.completedFuture(null)
        .thenRun(() -> {
          throw exception;
        })
        .exceptionally(handler)
        .get();
  }
}
