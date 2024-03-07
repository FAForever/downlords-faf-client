package com.faforever.client.vault.review;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class ReviewsControllerTest extends PlatformTest {

  @InjectMocks
  private ReviewsController<MapVersionReview> instance;

  @Mock
  private I18n i18n;
  @Mock
  private ReviewController<MapVersionReview> reviewController;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @BeforeEach
  public void setUp() throws Exception {
    when(reviewController.getRoot()).thenReturn(new Pane());
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());

    loadFxml("theme/vault/review/reviews.fxml", param -> {
      if (param == ReviewController.class) {
        return reviewController;
      }
      if (param == StarsController.class) {
        return starsController;
      }
      if (param == StarController.class) {
        return starController;
      }
      return instance;
    });
  }
}
