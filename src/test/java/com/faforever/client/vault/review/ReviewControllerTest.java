package com.faforever.client.vault.review;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.layout.Pane;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ReviewControllerTest extends AbstractPlainJavaFxTest {

  private ReviewController instance;

  @Mock
  private I18n i18n;
  @Mock
  private ReviewController reviewController;
  @Mock
  private PlayerService playerService;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @Before
  public void setUp() throws Exception {
    instance = new ReviewController(i18n, playerService);

    when(reviewController.getRoot()).thenReturn(new Pane());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));

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
  public void testSetReviewWithVersion() throws Exception {
    when(i18n.get("review.currentVersion")).thenReturn("current");
    Review review = ReviewBuilder.create().defaultValues().latestVersion(new ComparableVersion(("1"))).version(new ComparableVersion("1")).get();

    runOnFxThreadAndWait(() -> instance.setReview(Optional.of(review)));

    assertFalse(instance.versionLabel.isVisible());
    assertEquals("current", instance.versionLabel.getText());
  }

  @Test
  public void testSetReviewNoVersion() throws Exception {
    Review review = ReviewBuilder.create().defaultValues().version(null).get();

    runOnFxThreadAndWait(() -> instance.setReview(Optional.of(review)));

    assertFalse(instance.versionLabel.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.ownReviewRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
