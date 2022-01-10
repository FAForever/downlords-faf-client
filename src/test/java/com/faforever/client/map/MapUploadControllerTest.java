package com.faforever.client.map;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MapUploadControllerTest extends UITest {

  @InjectMocks
  private MapUploadController instance;

  @Mock
  private MapService mapService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Spy
  private ClientProperties clientProperties = new ClientProperties();


  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/vault/map/map_upload.fxml", param -> instance);
  }

  @Test
  public void testNoUploadIfRulesNotChecked() {
    instance.onUploadClicked();
    verifyNoMoreInteractions(mapService);
    assertThat(instance.rulesLabel.getStyleClass().contains("bad"), is(true));
  }
}