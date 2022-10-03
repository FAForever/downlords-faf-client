package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.GameFilterController;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.map.MapPreviewTableCellController;
import com.faforever.client.vault.replay.LiveReplayController;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SplitPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiveReplayControllerTest extends UITest {

  @Mock
  private GameService gameService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private TimeService timeService;
  @Mock
  private GameFilterController gameFilterController;

  @InjectMocks
  private LiveReplayController instance;

  private final GameBean openedGame = GameBeanBuilder.create().defaultValues().id(1).status(GameStatus.OPEN).get();
  private final GameBean livingGame = GameBeanBuilder.create().defaultValues().id(2).status(GameStatus.PLAYING).get();

  @BeforeEach
  public void setUp() throws Exception {

    when(gameService.getGames()).thenReturn(FXCollections.observableArrayList());
    when(uiService.loadFxml("theme/filter/filter.fxml", GameFilterController.class)).thenReturn(gameFilterController);
    when(gameFilterController.getFilterStateProperty()).thenReturn(new SimpleBooleanProperty());
    when(gameFilterController.getPredicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));
    when(uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml")).thenReturn(mock(MapPreviewTableCellController.class));
    when(i18n.get(any())).thenReturn("test");

    loadFxml("theme/vault/replay/live_replays.fxml", clazz -> instance);
  }

  @Test
  public void testOnTableViewDisplay() {
    ArgumentCaptor<Predicate<GameBean>> argumentCaptor = ArgumentCaptor.forClass(Predicate.class);
    verify(gameFilterController).setDefaultPredicate(argumentCaptor.capture());
    assertTrue(argumentCaptor.getValue().test(livingGame));
    assertFalse(argumentCaptor.getValue().test(openedGame));
  }

  @Test
  public void testOnFilterButtonClicked() {
    when(gameFilterController.getRoot()).thenReturn(new SplitPane());
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.getRoot());
      instance.onFilterButtonClicked();
    });
    Window window = gameFilterController.getRoot().getParent().getScene().getWindow();
    assertTrue(window.getClass().isAssignableFrom(Popup.class));
    assertTrue(window.isShowing());

    runOnFxThreadAndWait(() -> instance.onFilterButtonClicked());
    assertFalse(window.isShowing());
  }
}
