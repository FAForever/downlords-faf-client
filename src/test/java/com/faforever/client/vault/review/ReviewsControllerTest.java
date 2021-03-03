package com.faforever.client.vault.review;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ReviewsControllerTest extends AbstractPlainJavaFxTest {

  private ReviewsController instance;

  @Mock
  private I18n i18n;
  @Mock
  private ReviewController reviewController;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @Before
  public void setUp() throws Exception {
    instance = new ReviewsController(i18n, uiService, playerService);

    when(reviewController.getRoot()).thenReturn(new Pane());
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBuilder.create("junit").defaultValues().get());

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

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.reviewsRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void onCreateReviewButtonClicked() throws Exception {
    instance.onCreateReviewButtonClicked();
  }
}
