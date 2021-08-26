package com.faforever.client.login;

import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.assertions.api.Assertions.assertThat;

class OfflineServicesControllerTest extends UITest {
  private OfflineServicesController instance;

  @Mock
  private UiService uiService;
  @Mock
  private OfflineServiceController offlineServiceController;

  @BeforeEach
  void setUp() throws Exception {
    when(offlineServiceController.getRoot()).thenReturn(new Label());

    instance = new OfflineServicesController(uiService);
    loadFxml("theme/login/offline_services.fxml", param -> instance);
  }

  @Test
  void testGetRoot() {
    assertThat(instance.getRoot()).isNotNull();
  }

  @Test
  void addService() {
    when(uiService.loadFxml("theme/login/offline_service.fxml")).thenReturn(offlineServiceController);

    assertThat(instance.offlineServicesContainer).hasNoChildren();

    OffsetDateTime now = OffsetDateTime.now();
    instance.addService("JUnit", "Reason", now);

    verify(offlineServiceController).setInfo("JUnit", "Reason", now);
    assertThat(instance.offlineServicesContainer).hasExactlyNumChildren(1);
  }
}