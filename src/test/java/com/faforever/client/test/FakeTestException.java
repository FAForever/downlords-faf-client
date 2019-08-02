package com.faforever.client.test;

public class FakeTestException extends Exception {
  private static final long serialVersionUID = -2887429654623895910L;

  public FakeTestException() {
    this("Test Exception");
  }

  public FakeTestException(Throwable cause) {
    super("Test Exception", cause);
  }

  public FakeTestException(String message, Throwable cause) {
    super(message, cause);
  }

  public FakeTestException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this; // this exception will not print stack trace
  }
}
