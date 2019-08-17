package com.faforever.client.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.junit.Test;

import com.faforever.client.test.FakeTestException;

public class ConcurrentUtilTest {
  
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
