package com.faforever.client.vault.review;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ReviewControllerTest extends PlatformTest {

  @InjectMocks
  private ReviewController<MapVersionReviewBean> instance;

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
    when(i18n.get(eq("review.version"), any())).thenAnswer(invocation -> invocation.getArgument(1));
    MapVersionBean mapVersion = Instancio.create(MapVersionBean.class);
    MapVersionReviewBean review = Instancio.of(MapVersionReviewBean.class)
                                           .set(field(MapVersionReviewBean::subject), mapVersion)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertTrue(instance.versionLabel.isVisible());
    assertEquals(review.version().toString(), instance.versionLabel.getText());
  }

  @Test
  public void testSetReviewNoVersion() throws Exception {
    MapBean map = Instancio.create(MapBean.class);
    MapVersionBean mapVersion = Instancio.of(MapVersionBean.class).ignore(field(MapVersionBean::version)).create();
    MapVersionReviewBean review = Instancio.of(MapVersionReviewBean.class)
                                           .set(field(MapVersionReviewBean::subject), mapVersion)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertNull(instance.versionLabel.getText());
  }
}
