package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.image.ImageView;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class GamesTableControllerTest extends AbstractPlainJavaFxTest {

  private GamesTableController instance;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private UiService uiService;
  @Mock
  private MapService mapService;

  @Before
  public void setUp() throws Exception {
    instance = new GamesTableController(mapService, joinGameHelper, i18n, uiService);

    loadFxml("theme/play/games_table.fxml", param -> instance);

    Platform.runLater(() -> getRoot().getChildren().addAll(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void test() throws Exception {
    instance.initializeGameTable(FXCollections.observableArrayList(
        GameBuilder.create().defaultValues().get(),
        GameBuilder.create().defaultValues().state(GameState.CLOSED).get()
    ));
  }

  @Test
  public void testUpdate() throws Exception {
    when(uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml")).thenReturn((Controller<ImageView>) ImageView::new);
    when(i18n.get(anyString())).thenReturn("abc");
    Game game = GameBuilder.create().get();
    Platform.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList());
      instance.updateTable(FXCollections.observableArrayList(game));
    });

    WaitForAsyncUtils.waitForFxEvents();
    Assert.assertThat(instance.gamesTable.getItems().get(0), CoreMatchers.equalTo(game));
  }
}
