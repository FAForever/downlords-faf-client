package com.faforever.client.login;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.testfx.assertions.api.Assertions.assertThat;

class AnnouncementControllerTest extends UITest {

  private AnnouncementController instance;

  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AnnouncementController(timeService, i18n);
    loadFxml("theme/login/announcement.fxml", param -> instance);
  }

  @Test
  void testGetRoot() {
    assertThat(instance.getRoot()).isNotNull();
  }

  @Test
  void setTitle() {
    assertThat(instance.titleLabel).doesNotHaveText("JUnit");
    instance.setTitle("JUnit");
    assertThat(instance.titleLabel).hasText("JUnit");
  }

  @Test
  void setMessage() {
    assertThat(instance.messageLabel).doesNotHaveText("JUnit");
    instance.setMessage("JUnit");
    assertThat(instance.messageLabel).hasText("JUnit");
  }

  @Test
  void setTime() {
    OffsetDateTime start = OffsetDateTime.now();
    OffsetDateTime end = OffsetDateTime.now().plusHours(1);

    when(timeService.asDateTime(start)).thenReturn("2021-08-29 21:34");
    when(timeService.asDateTime(end)).thenReturn("2021-08-29 22:34");
    when(i18n.get("temporalRange", "2021-08-29 21:34", "2021-08-29 22:34")).thenReturn("From .. to ..");

    assertThat(instance.timeLabel).hasText("Time");

    instance.setTime(start, end);

    assertThat(instance.timeLabel).hasText("From .. to ..");
  }
}