package com.faforever.client.ui.statusbar;

import com.faforever.client.chat.ChatService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.UITest;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatusBarControllerTest extends UITest {
  private StatusBarController instance;
  private ObjectProperty<ConnectionState> connectionStateProperty;

  @Mock
  private FafService fafService;
  @Mock
  private I18n i18n;
  @Mock
  private ChatService chatService;
  @Mock
  private TaskService taskService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new StatusBarController(fafService, i18n, chatService, taskService);

    connectionStateProperty = new SimpleObjectProperty<>();
    when(taskService.getActiveWorkers()).thenReturn(FXCollections.emptyObservableList());
    when(fafService.connectionStateProperty()).thenReturn(connectionStateProperty);
    when(chatService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());

    loadFxml("theme/statusbar/status_bar.fxml", param -> instance);
  }

  @Test
  public void testOnFaConnectedDoesntThrowUp() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> textFuture.complete(newValue));

    connectionStateProperty.set(ConnectionState.CONNECTED);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  public void testOnFaConnecting() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> textFuture.complete(newValue));

    connectionStateProperty.set(ConnectionState.CONNECTING);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  public void testOnFafDisconnected() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> textFuture.complete(newValue));

    connectionStateProperty.set(ConnectionState.DISCONNECTED);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  public void testOnFafReconnectClicked() throws Exception {
    instance.onFafReconnectClicked();
    verify(fafService).reconnect();
  }

  @Test
  public void testOnIrcReconnectClicked() throws Exception {
    instance.onChatReconnectClicked();
    verify(chatService).reconnect();
  }
}
