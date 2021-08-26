package com.faforever.client.login;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testfx.assertions.api.Assertions.assertThat;

class OfflineServiceControllerTest extends UITest {

  private OfflineServiceController instance;

  @Mock
  private I18n i18n;
  @Mock
  private TimeService timeService;

  @BeforeEach
  void setUp() throws Exception {
    instance = new OfflineServiceController(i18n, timeService);
    loadFxml("theme/login/offline_service.fxml", param -> instance);
  }

  @Test
  void testGetRoot() {
    assertThat(instance.getRoot()).isNotNull();
  }

  @Test
  void setInfo() {
    when(timeService.shortDuration(any())).thenReturn("1 min");
    when(i18n.get("login.offlineService.text", "JUnit", "1 min", "Reason")).thenReturn("Offline Message");

    assertThat(instance.offlineServiceRoot).doesNotHaveText("Offline Message");
    instance.setInfo("JUnit", "Reason", OffsetDateTime.now());
    assertThat(instance.offlineServiceRoot).hasText("Offline Message");
  }
}