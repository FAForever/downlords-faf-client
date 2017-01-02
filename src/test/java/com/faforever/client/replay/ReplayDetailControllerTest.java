package com.faforever.client.replay;

import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReplayDetailControllerTest extends AbstractPlainJavaFxTest {
  private ReplayDetailController instance;

  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayService replayService;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayDetailController(timeService, i18n, uiService, replayService);

    loadFxml("theme/vault/replay/replay_detail.fxml", param -> instance);

    assertThat(instance.optionsTable.isVisible(), is(false));
    assertThat(instance.chatTable.isVisible(), is(false));
  }

  @Test
  public void setReplay() throws Exception {
    Replay replay = new Replay();
    replay.setFeaturedMod(new FeaturedMod());
    instance.setReplay(replay);
  }

  @Test
  public void onDownloadMoreInfoClicked() throws Exception {
    Replay replay = new Replay();
    replay.setFeaturedMod(new FeaturedMod());
    instance.setReplay(replay);
    Path tmpPath = Paths.get("foo.tmp");
    when(replayService.downloadReplay(replay.getId())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    instance.onDownloadMoreInfoClicked();

    verify(replayService).enrich(replay, tmpPath);
    assertThat(instance.optionsTable.isVisible(), is(true));
    assertThat(instance.chatTable.isVisible(), is(true));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayDetailRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void onCloseButtonClicked() throws Exception {
    new Pane(instance.getRoot());
    instance.onCloseButtonClicked();
  }

  @Test
  public void onDimmerClicked() throws Exception {
    new Pane(instance.getRoot());
    instance.onDimmerClicked();
  }

  @Test
  public void onContentPaneClicked() throws Exception {
    MouseEvent event = MouseEvents.generateClick(MouseButton.PRIMARY, 1);
    instance.onContentPaneClicked(event);
    assertThat(event.isConsumed(), is(true));
  }
}
