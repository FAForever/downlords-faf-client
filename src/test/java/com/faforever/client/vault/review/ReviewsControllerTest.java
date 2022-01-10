package com.faforever.client.vault.review;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ReviewsControllerTest extends UITest {

  @InjectMocks
  private ReviewsController<MapVersionReviewBean> instance;

  @Mock
  private I18n i18n;
  @Mock
  private ReviewController<MapVersionReviewBean> reviewController;
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
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

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
