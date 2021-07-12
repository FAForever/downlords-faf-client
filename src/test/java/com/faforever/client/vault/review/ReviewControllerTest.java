package com.faforever.client.vault.review;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import javafx.beans.property.SimpleFloatProperty;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ReviewControllerTest extends UITest {

  private ReviewController instance;

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
    instance = new ReviewController(i18n, playerService);

    when(playerService.getCurrentPlayer()).thenReturn(PlayerBuilder.create("junit").defaultValues().get());
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
    Review review = ReviewBuilder.create().defaultValues().latestVersion(new ComparableVersion(("1"))).version(new ComparableVersion("1")).get();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertTrue(instance.versionLabel.isVisible());
    assertEquals("current", instance.versionLabel.getText());
  }

  @Test
  public void testSetReviewNoVersion() throws Exception {
    Review review = ReviewBuilder.create().defaultValues().version(null).get();

    runOnFxThreadAndWait(() -> instance.setReview(review));

    assertFalse(instance.versionLabel.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.ownReviewRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
