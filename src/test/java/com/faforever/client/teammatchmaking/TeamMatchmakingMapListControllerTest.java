package com.faforever.client.teammatchmaking;

import com.faforever.client.builders.MatchmakerQueueInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.VetoKey;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class TeamMatchmakingMapListControllerTest extends PlatformTest {

  @Mock
  private MapService mapService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Spy
  private MatchmakerPrefs matchmakerPrefs;

  @InjectMocks
  private TeamMatchmakingMapListController instance;

  private MatchmakerQueueInfo queue;
  private MatchmakerQueueMapPool bracket1;
  private MatchmakerQueueMapPool bracket2;
  private MatchmakerQueueMapPool bracket3;
  private List<MapPoolAssignment> maps1;
  private List<MapPoolAssignment> maps2;
  private List<MapPoolAssignment> maps3;

  @BeforeEach
  public void setUp() throws Exception {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    Leaderboard leaderboard = Instancio.create(Leaderboard.class);
    queue = MatchmakerQueueInfoBuilder.create().defaultValues().leaderboard(leaderboard).get();

    bracket1 = new MatchmakerQueueMapPool(1, null, 1000.0, queue, 3, 2, 0.5f);
    bracket2 = new MatchmakerQueueMapPool(2, 1000.0, 1500.0, queue, 5, 3, 0.5f);
    bracket3 = new MatchmakerQueueMapPool(3, 1500.0, null, queue, 0, 0, 0.5f);

    maps1 = List.of(Instancio.create(MapPoolAssignment.class), Instancio.create(MapPoolAssignment.class));
    maps2 = List.of(Instancio.create(MapPoolAssignment.class), Instancio.create(MapPoolAssignment.class), Instancio.create(MapPoolAssignment.class));
    maps3 = List.of(Instancio.create(MapPoolAssignment.class));

    matchmakerPrefs.getAppliedVetoes().clear();

    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(i18n.get("game.rating")).thenReturn("Rating");
    lenient().when(i18n.get("teammatchmaking.applyVetoes")).thenReturn("Apply Vetoes");
    lenient().when(i18n.get("teammatchmaking.noVetoes")).thenReturn("No Vetoes");
    lenient().when(i18n.get("teammatchmaking.bracket.anyRating")).thenReturn("Any Rating");
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(reactor.core.scheduler.Schedulers.immediate());
    lenient().doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any(Runnable.class));

    loadFxml("theme/play/teammatchmaking/matchmaking_maplist_popup.fxml", clazz -> instance);
}

  @Test
  public void testBracketComboBoxPopulated() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(
        bracket1, maps1,
        bracket2, maps2,
        bracket3, maps3
    );

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getItems().size(), is(3));
  }

  @Test
  public void testBracketTitleFormattingUpperBound() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getItems().get(0), is("Rating < 1000"));
  }

  @Test
  public void testBracketTitleFormattingRange() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket2, maps2);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getItems().get(0), is("Rating 1000 - 1500"));
  }

  @Test
  public void testBracketTitleFormattingLowerBound() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket3, maps3);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getItems().get(0), is("Rating > 1500"));
  }

  @Test
  public void testLoadingPaneShown() {
    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> {
      instance.loadingPane.setVisible(false);
      instance.setQueue(queue);
    });

    assertTrue(instance.loadingPane.isVisible());
  }

  @Test
  public void testVetoModeOffByDefault() {
    assertFalse(instance.vetoTokensWallet.isVisible());
  }

  @Test
  public void testVetoModeToggle() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    assertTrue(instance.vetoTokensWallet.isVisible());
    assertFalse(instance.applyVetoesButton.isVisible());
  }

  @Test
  public void testApplyVetoesButtonVisibleWhenModeOff() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertFalse(instance.vetoTokensWallet.isVisible());
    assertTrue(instance.applyVetoesButton.isVisible());
  }

  @Test
  public void testVetoWalletVisibleWhenModeOn() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    assertTrue(instance.vetoTokensWallet.isVisible());
    assertFalse(instance.applyVetoesButton.isVisible());
  }

  @Test
  public void testVetoButtonDisabledWhenNoTokens() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket3, maps3);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertTrue(instance.applyVetoesButton.isDisabled());
  }

  @Test
  public void testVetoLabelChangesWithTokens() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.applyVetoesLabel.getText(), is("Apply Vetoes"));
  }

  @Test
  public void testVetoLabelChangesWithoutTokens() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket3, maps3);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.applyVetoesLabel.getText(), is("No Vetoes"));
  }

  @Test
  public void testTokenWalletDisplay() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    assertThat(instance.vetoTokensViewer.getChildren().size(), is(3));
  }

  @Test
  public void testTokensAppliedCalculation() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setTokensForMap(new VetoKey(bracket1.id(), maps1.get(0).id()), 2);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    assertThat(instance.vetoTokensViewer.getChildren().size(), is(3));
  }

  @Test
  public void testNoBracketsHandled() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of();

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getItems().size(), is(0));
  }

  @Test
  public void testBracketSwitching() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(
        bracket1, maps1,
        bracket2, maps2
    );

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> instance.setQueue(queue));
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.bracketComboBox.getSelectionModel().select(1));
    waitFxEvents();

    assertThat(instance.bracketComboBox.getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testVetoTokenUsedStyleApplied() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setTokensForMap(new VetoKey(bracket1.id(), maps1.get(0).id()), 2);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    assertThat(instance.vetoTokensViewer.getChildren().size(), is(3));

    long usedTokenCount = instance.vetoTokensViewer.getChildren().stream()
        .filter(node -> node.getStyleClass().contains("tmm-maplist-palm_used"))
        .count();

    assertThat(usedTokenCount, is(2L));
  }

  @Test
  public void testVetoTokenUsedStyleRemoved() {
    Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets = Map.of(bracket1, maps1);

    when(mapService.getMatchmakerBrackets(queue)).thenReturn(Mono.just(brackets));

    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setTokensForMap(new VetoKey(bracket1.id(), maps1.get(0).id()), 2);
    });
    waitFxEvents();

    runOnFxThreadAndWait(() -> instance.toggleVetoMode());

    long usedTokenCount = instance.vetoTokensViewer.getChildren().stream()
        .filter(node -> node.getStyleClass().contains("tmm-maplist-palm_used"))
        .count();

    assertThat(usedTokenCount, is(2L));

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setTokensForMap(new VetoKey(bracket1.id(), maps1.get(0).id()), 0);
    });
    waitFxEvents();

    usedTokenCount = instance.vetoTokensViewer.getChildren().stream()
        .filter(node -> node.getStyleClass().contains("tmm-maplist-palm_used"))
        .count();

    assertThat(usedTokenCount, is(0L));
  }
}