package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Runs the real dialog, as what matters here is whether it is on screen. One test at a time, as the dialog's owner is
 * in the static {@link StageHolder}.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class NotificationServiceQuestionTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private ToastDisplayer toastDisplayer;

  private NotificationService instance;
  private Stage owner;

  @BeforeEach
  public void setUp() throws Exception {
    ImmediateNotificationController controller = new ImmediateNotificationController();
    loadFxml("theme/immediate_notification.fxml", clazz -> controller);
    when(uiService.loadFxml("theme/immediate_notification.fxml")).thenReturn(controller);
    instance = new NotificationService(uiService, reportingService, i18n, toastDisplayer, fxApplicationThreadExecutor);
    // Otherwise closing the last window here stops JavaFX for every test after it
    Platform.setImplicitExit(false);
    runOnFxThreadAndWait(() -> {
      owner = new Stage();
      owner.setScene(new Scene(new StackPane(), 800, 600));
      owner.show();
      StageHolder.setStage(owner);
    });
  }

  @AfterEach
  public void tearDown() {
    runOnFxThreadAndWait(() -> owner.close());
  }

  private ImmediateNotification question() {
    return new ImmediateNotification("title", "text", Severity.WARN, List.of(new Action("answer")));
  }

  private boolean dialogIsOpen() {
    return dialog() != null;
  }

  private Window dialog() {
    return Window.getWindows()
                 .stream()
                 .filter(window -> window instanceof Stage stage && stage.getOwner() == owner && stage.isShowing())
                 .findFirst()
                 .orElse(null);
  }

  private void clickNextToTheDialog() {
    runOnFxThreadAndWait(() -> {
      Node pane = dialog().getScene().getRoot().lookup(".dialog-pane");
      assertNotNull(pane);
      Event.fireEvent(pane, new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false,
                                           false, false, true, false, false, true, false, true, null));
    });
  }

  @Test
  public void aQuestionClosesWhenItsTimeRunsOut() throws Exception {
    CompletableFuture<Boolean> answer = new CompletableFuture<>();
    instance.addImmediateQuestion(question(), answer);
    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, this::dialogIsOpen);

    answer.complete(false);

    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, () -> !dialogIsOpen());
    assertFalse(dialogIsOpen());
  }

  @Test
  public void aClickNextToAQuestionLeavesItOpen() throws Exception {
    CompletableFuture<Boolean> answer = new CompletableFuture<>();
    instance.addImmediateQuestion(question(), answer);
    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, this::dialogIsOpen);

    clickNextToTheDialog();
    WaitForAsyncUtils.sleep(2, TimeUnit.SECONDS);

    assertTrue(dialogIsOpen());
    answer.complete(false);
    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, () -> !dialogIsOpen());
  }

  /** The same click does close a plain notification, so the test above can tell the difference. */
  @Test
  public void aClickNextToAPlainNotificationClosesIt() throws Exception {
    instance.addNotification(question());
    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, this::dialogIsOpen);

    clickNextToTheDialog();

    WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, () -> !dialogIsOpen());
    assertFalse(dialogIsOpen());
  }
}
