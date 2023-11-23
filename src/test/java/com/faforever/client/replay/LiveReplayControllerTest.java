package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.LiveGamesFilterController;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenLiveReplayViewEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiveReplayControllerTest extends PlatformTest {

  @Mock
  private GameService gameService;
  @Mock
  private UiService uiService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private TimeService timeService;
  @Mock
  private LiveGamesFilterController liveGamesFilterController;

  @InjectMocks
  private LiveReplayController instance;

  @BeforeEach
  public void setUp() throws Exception {

    when(gameService.getGames()).thenReturn(FXCollections.observableArrayList());
    when(uiService.loadFxml("theme/filter/filter.fxml", LiveGamesFilterController.class)).thenReturn(liveGamesFilterController);
    when(liveGamesFilterController.filterActiveProperty()).thenReturn(new SimpleBooleanProperty());
    when(liveGamesFilterController.predicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));
    when(i18n.get(any())).thenReturn("test");

    loadFxml("theme/vault/replay/live_replays.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> instance.display(new OpenLiveReplayViewEvent()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFilterOnlyLiveGames() {
    ArgumentCaptor<Predicate<GameBean>> argumentCaptor = ArgumentCaptor.forClass(Predicate.class);
    verify(liveGamesFilterController).setDefaultPredicate(argumentCaptor.capture());
    assertFalse(argumentCaptor.getValue().test(GameBeanBuilder.create().defaultValues().id(1).status(GameStatus.OPEN).get()));
    assertTrue(argumentCaptor.getValue().test(GameBeanBuilder.create().defaultValues().id(2).status(GameStatus.PLAYING).get()));
  }
}
