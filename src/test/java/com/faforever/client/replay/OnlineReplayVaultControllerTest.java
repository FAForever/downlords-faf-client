package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnlineReplayVaultControllerTest extends AbstractPlainJavaFxTest {
  private static final int MAX_RESULTS = 100;
  private OnlineReplayVaultController instance;

  @Mock
  private ReplayService replayService;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayDetailController replayDetailController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/query/logical_node.fxml")).thenAnswer(invocation -> {
      LogicalNodeController controller = mock(LogicalNodeController.class);
      controller.logicalOperatorField = new ChoiceBox<>();
      controller.specificationController = mock(SpecificationController.class);
      controller.specificationController.propertyField = new ComboBox<>();
      controller.specificationController.operationField = new ChoiceBox<>();
      controller.specificationController.valueField = new ComboBox<>();
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });
    when(uiService.loadFxml("theme/vault/replay/replay_card.fxml")).thenAnswer(invocation -> {
      ReplayCardController controller = mock(ReplayCardController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });

    instance = new OnlineReplayVaultController(replayService, uiService, notificationService, i18n);

    loadFxml("theme/vault/replay/online_replays.fxml", clazz -> {
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    });
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplayPopulatesReplays() throws Exception {
    List<Replay> replays = Arrays.asList(new Replay(), new Replay());
    when(replayService.getNewestReplays(anyInt())).thenReturn(CompletableFuture.completedFuture(replays));
    when(replayService.getHighestRatedReplays(anyInt())).thenReturn(CompletableFuture.completedFuture(replays));
    when(replayService.getMostWatchedReplays(anyInt())).thenReturn(CompletableFuture.completedFuture(replays));

    instance.onDisplay();

    verify(replayService).getNewestReplays(anyInt());
    verify(replayService).getHighestRatedReplays(anyInt());
    verify(replayService).getMostWatchedReplays(anyInt());
  }

  @Test
  public void testOnSearchButtonClicked() throws Exception {
    instance.queryTextField.setText("query");
    when(replayService.findByQuery("query", MAX_RESULTS))
        .thenReturn(CompletableFuture.completedFuture(Arrays.asList(new Replay(), new Replay())));

    instance.onSearchButtonClicked();

    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
    verify(replayService).findByQuery("query", MAX_RESULTS);
  }

  @Test
  public void testOnSearchButtonClickedHandlesException() throws Exception {
    instance.queryTextField.setText("query");
    CompletableFuture<List<Replay>> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new RuntimeException("JUnit test exception"));
    when(replayService.findByQuery("query", MAX_RESULTS)).thenReturn(completableFuture);

    instance.onSearchButtonClicked();

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testBuildQuery() throws Exception {
    instance.onAddCriteriaButtonClicked();

    Condition condition = mock(Condition.class);
    when(specificationController.appendTo(any())).thenReturn(Optional.of(condition));
    when(condition.query(any(RSQLVisitor.class))).thenReturn("name==JUnit");

    specificationController.propertyField.setValue("name");
    specificationController.operationField.getSelectionModel().select(0);
    specificationController.valueField.setValue("JUnit");

    assertThat(instance.queryTextField.getText(), is("name==JUnit"));
  }
}
