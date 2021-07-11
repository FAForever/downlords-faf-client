package com.faforever.client.replay;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalReplayVaultControllerTest extends AbstractPlainJavaFxTest {

  private LocalReplayVaultController instance;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReplayService replayService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private UiService uiService;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SearchController searchController;
  @Mock
  private SpecificationController specificationController;

  private ReplayDetailController replayDetailController;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new LocalReplayVaultController(replayService, uiService, notificationService, i18n, preferencesService,
        reportingService);

    when(preferencesService.getPreferences()).thenReturn(new Preferences());

    doAnswer(invocation -> {
      replayDetailController = mock(ReplayDetailController.class);
      when(replayDetailController.getRoot()).then(invocation1 -> new Pane());
      return replayDetailController;
    }).when(uiService).loadFxml("theme/vault/replay/replay_detail.fxml");

    loadFxml("theme/vault/vault_entity.fxml", clazz -> {
      if (SearchController.class.isAssignableFrom(clazz)) {
        return searchController;
      }
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    }, instance);
  }

  @Test
  public void testSetSupplier() throws IOException {
    instance.setSupplier(null);

    verify(replayService).loadLocalReplayPage(instance.pageSize, 1);
  }

  @Test
  public void testShowLocalReplayDetail() {
    Replay replay = ReplayBuilder.create().get();
    JavaFxUtil.runLater(() -> instance.onDisplayDetails(replay));
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayDetailController).setReplay(replay);
    assertThat(replayDetailController.getRoot().isVisible(), is(true));
  }
}
