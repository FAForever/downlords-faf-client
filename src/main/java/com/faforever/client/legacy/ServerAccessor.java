package com.faforever.client.legacy;

import com.faforever.client.util.Callback;
import com.google.gson.Gson;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerAccessor implements ServerReader.OnServerMessageListener {

  private static final int VERSION = 122;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  private Gson gson;

  private final Object writeMonitor = new Object();

  private String localIp;
  private String session;
  private String username;
  private Callback<Void> loginCallback;
  private Socket socket;
  private QStreamWriter socketOut;

  public void connect() throws IOException {
    socket = new Socket(
        environment.getProperty("lobby.host"),
        environment.getProperty("lobby.port", int.class)
    );
    socket.setKeepAlive(true);

    localIp = socket.getLocalAddress().getHostAddress();
    socketOut = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));

    writeToServer(ClientMessage.askSession(username));
    startServerReader(socket);
  }

  private void startServerReader(Socket socket) {
    new ServerReader(gson, socket, this).start();
  }

  @Override
  public void onServerMessage(ServerMessage serverMessage) {
    if (serverMessage != null && serverMessage.session != null) {
      this.session = serverMessage.session;
    }
  }

  @Override
  public void onServerPing() {
    writeToServer(PongMessage.INSTANCE);
  }

  public void login(final String username, final String password, Callback<Void> callback) {
    this.username = username;
    this.loginCallback = callback;

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureConnected();
        writeToServer(ClientMessage.login(username, password, session));
        return null;
      }
    }, null);
  }

  private void ensureConnected() throws IOException {
    if (socket == null || socket.isClosed()) {
      connect();
    }
  }

  private <T> void executeInBackground(final Task<T> task, final Callback<T> callback) {
    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
      @Override
      public void handle(WorkerStateEvent event) {
        if (callback == null) {
          return;
        }

        Throwable exception = event.getSource().getException();
        if (exception != null) {
          callback.error(exception);
        } else {
          callback.success((T) event.getSource().getValue());
        }
      }
    });
    task.setOnFailed(new EventHandler<WorkerStateEvent>() {
      @Override
      public void handle(WorkerStateEvent event) {
        logger.warn("Task failed", event.getSource().getException());
      }
    });

    new Service<T>() {
      @Override
      protected Task<T> createTask() {
        return task;
      }
    }.start();
  }

  private void writeToServer(Serializable serializable) {
    synchronized (writeMonitor) {
      try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializable.serialize(new QStreamWriter(byteArrayOutputStream), username, session);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        logger.debug("Writing to server: {}", new String(byteArray, StandardCharsets.UTF_16BE));

        socketOut.append(byteArray);
        socketOut.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
