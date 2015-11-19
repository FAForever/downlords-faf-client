package com.faforever.client.api;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.common.io.Resources;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starts a local server that listens for the verification code. <p> After the user authorized the application, google
 * redirects to a URL specified by the application (http://localhost:####) to send the verification code there.</p>
 */
public class LocalHttpVerificationCodeReceiver implements VerificationCodeReceiver {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String HTTP_LOCALHOST = "http://localhost:";
  private static final Pattern AUTHORIZATION_CODE_PATTERN = Pattern.compile("[?&]code=(.*?)[& ]");

  public Future<String> codeFuture;
  @Resource
  ExecutorService executorService;
  private ServerSocket verificationCodeServerSocket;

  @PreDestroy
  void shutDown() {
    IOUtils.closeQuietly(verificationCodeServerSocket);
  }

  @Override
  public String getRedirectUri() throws IOException {
    CompletableFuture<Integer> portFuture = startReceiver();

    try {
      return HTTP_LOCALHOST + portFuture.get(5, TimeUnit.MINUTES);
    } catch (TimeoutException | InterruptedException | ExecutionException e) {
      throw new IOException("Receiver could not be started", e);
    }
  }

  @Override
  public String waitForCode() throws IOException {
    try {
      return codeFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException("Code could not be received", e);
    }
  }

  @Override
  public void stop() throws IOException {
  }

  private CompletableFuture<Integer> startReceiver() throws IOException {
    CompletableFuture<Integer> portFuture = new CompletableFuture<>();
    Callable<String> callable = () -> {
      try (ServerSocket serverSocket = new ServerSocket(0)) {
        LocalHttpVerificationCodeReceiver.this.verificationCodeServerSocket = serverSocket;
        logger.debug("Started verification code listener at port {}", serverSocket.getLocalPort());
        portFuture.complete(serverSocket.getLocalPort());

        try (Socket socket = serverSocket.accept()) {
          logger.debug("Accepted connection from browser");
          BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          String line = reader.readLine();

          Matcher matcher = AUTHORIZATION_CODE_PATTERN.matcher(line);
          if (!matcher.find()) {
            throw new IOException("Could not extract code from: " + line);
          }

          String code = matcher.group(1);
          logger.debug("Received code: {}", code);

          socket.getOutputStream().write(Resources.toByteArray(getClass().getResource("/google_auth_answer.txt")));

          return code;
        }
      }
    };

    codeFuture = executorService.submit(callable);
    return portFuture;
  }
}
