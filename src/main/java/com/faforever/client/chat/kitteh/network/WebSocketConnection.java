package com.faforever.client.chat.kitteh.network;

import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.LineSeparator;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionFailedEvent;
import org.kitteh.irc.client.library.exception.KittehConnectionException;
import org.kitteh.irc.client.library.feature.defaultmessage.DefaultMessageType;
import org.kitteh.irc.client.library.feature.network.ClientConnection;
import org.kitteh.irc.client.library.util.HostWithPort;
import org.kitteh.irc.client.library.util.Sanity;
import org.kitteh.irc.client.library.util.ToStringer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class WebSocketConnection implements ClientConnection {
  private static final int MAX_LINE_LENGTH = 9001; // 8703 is the limit with IRCv3 latest message tags

  private final Client.WithManagement client;

  private final Sinks.Many<String> outboundSink = Sinks.many().unicast().onBackpressureBuffer();
  private final Flux<String> outboundMessages = outboundSink.asFlux().publish().autoConnect();

  private boolean reconnect = true;

  private @Nullable Disposable ping;
  private @Nullable Connection connection;

  private volatile String lastMessage;

  private boolean alive = true;

  /**
   * Constructs a Websocket connection.
   *
   * @param client client for which this exists
   */
  public WebSocketConnection(final Client.@NonNull WithManagement client) {
    this.client = Sanity.nullCheck(client, "Client");
    HostWithPort serverAddress = this.client.getServerAddress();
    HttpClient.newConnection()
              .resolver(DefaultAddressResolverGroup.INSTANCE)
              .doOnConnected(connection -> {
                this.connection = connection;
                connection.addHandlerFirst(new LineEncoder(LineSeparator.UNIX))
                          .addHandlerLast(new LineBasedFrameDecoder(MAX_LINE_LENGTH));
                this.client.getEventManager().callEvent(new ClientConnectionEstablishedEvent(this.client));
                this.client.beginMessageSendingImmediate(
                    message -> outboundSink.emitNext(message, EmitFailureHandler.busyLooping(Duration.ofMinutes(1))));
                this.alive = true;
              })
              .doOnDisconnected(connection -> {
                this.client.getEventManager()
                           .callEvent(new ClientConnectionClosedEvent(this.client, reconnect, null, lastMessage));
                this.client.pauseMessageSending();
                if (this.ping != null) {
                  this.ping.dispose();
                }
                this.alive = false;
                ClientConnectionEndedEvent event = new ClientConnectionClosedEvent(this.client, this.reconnect, null,
                                                                                   this.lastMessage);
                this.client.getEventManager().callEvent(event);
                if (event.willAttemptReconnect()) {
                  this.scheduleReconnect(event.getReconnectionDelay());
                }
              })
              .doOnResolveError((connection, throwable) -> {
                this.client.getEventManager()
                           .callEvent(new ClientConnectionFailedEvent(this.client, reconnect, throwable));
                this.client.getExceptionListener().queue(new KittehConnectionException(throwable, false));
              })
              .websocket()
              .uri(URI.create("wss://%s:%d".formatted(serverAddress.getHost(), serverAddress.getPort())))
              .handle((inbound, outbound) -> {
                outbound.sendString(outboundMessages.doOnNext(message -> this.client.getOutputListener().queue(message))
                                                    .doOnError(this::handleException))
                        .then()
                        .subscribeOn(Schedulers.single())
                        .subscribe();

                return inbound.receive().asString(StandardCharsets.UTF_8).doOnError(this::handleException);
              })
              .subscribe(message -> {
                this.client.getInputListener().queue(message);
                this.client.processLine(message);
                this.lastMessage = message;
              });
  }

  private void scheduleReconnect(int delay) {
    Mono.delay(Duration.ofMillis(delay)).doOnNext(_ -> this.client.connect()).subscribe();
  }

  private void handleException(Throwable thrown) {
    if (thrown instanceof Exception exception) {
      this.client.getExceptionListener().queue(exception);
      if (thrown instanceof IOException) {
        this.shutdown(DefaultMessageType.QUIT_INTERNAL_EXCEPTION, true);
      }
    }
  }

  @Override
  public boolean isAlive() {
    return this.alive;
  }

  @Override
  public void startPing() {
    this.ping = Flux.interval(Duration.ofSeconds(60)).doOnNext(_ -> this.client.ping()).subscribe();
  }

  @Override
  public void shutdown(DefaultMessageType messageType, boolean reconnect) {
    this.shutdown(this.client.getDefaultMessageMap().getDefault(messageType).orElse(null), reconnect);
  }

  @Override
  public void shutdown(@Nullable String message, boolean reconnect) {
    this.reconnect = reconnect;

    this.client.pauseMessageSending();
    outboundSink.emitNext("QUIT" + ((message != null) ? (" :" + message) : ""),
                          EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    if (this.connection != null) {
      this.connection.dispose();
    }
  }

  @Override
  public @NonNull String toString() {
    return new ToStringer(this).add("client", this.client).toString();
  }
}
