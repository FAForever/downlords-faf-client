package com.faforever.client.vault.review;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.MapVersionReviewBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ReviewControllerTest extends PlatformTest {

  @InjectMocks
  private ReviewController<MapVersionReviewBean> instance;

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @BeforeEach
  public void setUp() throws Exception {
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(PlayerBeanBuilder.create()
        .defaultValues()
        .get()));
    when(starsController.valueProperty()).thenReturn(new SimpleFloatProperty());

    loadFxml("theme/vault/review/review.fxml", param -> {
      if (param == StarsController.class) {
        return starsController;
      }
      if (param == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void testSetReviewWithVersion() throws Exception {
    when(i18n.get("review.currentVersion")).thenReturn("current");
    MapBean map = MapBeanBuilder.create().defaultValues().get();
    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().map(map).get();
    map.setLatestVersion(mapVersion);
    MapVersionReviewBean review = MapVersionReviewBeanBuilder.create()
        .defaultValues()
        .id(1)
        .mapVersion(mapVersion)
        .get();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertTrue(instance.versionLabel.isVisible());
    assertEquals("current", instance.versionLabel.getText());
  }

  @Test
  public void testSetReviewNoVersion() throws Exception {
    MapBean map = MapBeanBuilder.create().defaultValues().get();
    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().map(map).version(null).get();
    map.setLatestVersion(mapVersion);
    MapVersionReviewBean review = MapVersionReviewBeanBuilder.create().defaultValues().mapVersion(mapVersion).get();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertFalse(instance.versionLabel.isVisible());
  }
}
