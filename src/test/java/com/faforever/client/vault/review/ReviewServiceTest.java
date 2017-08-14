package com.faforever.client.vault.review;

import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReviewServiceTest {
  private ReviewService instance;

  @Mock
  private FafService fafService;

  @Before
  public void setUp() throws Exception {
    instance = new ReviewService(fafService);
  }

  @Test
  public void createGameReview() throws Exception {
    Review review = createReview();
    when(fafService.saveGameReview(review, 2)).thenReturn(CompletableFuture.completedFuture(null));

    instance.saveGameReview(review, 2);
    verify(fafService).saveGameReview(review, 2);
  }

  @Test
  public void createMapVersionReview() throws Exception {
    Review review = createReview();
    when(fafService.saveMapVersionReview(review, "2")).thenReturn(CompletableFuture.completedFuture(null));

    instance.saveMapVersionReview(review, "2");
    verify(fafService).saveMapVersionReview(review, "2");
  }

  @Test
  public void createModVersionReview() throws Exception {
    Review review = createReview();
    when(fafService.saveModVersionReview(review, 2)).thenReturn(CompletableFuture.completedFuture(null));

    instance.createModVersionReview(review, 2);
    verify(fafService).saveModVersionReview(review, 2);
  }

  private Review createReview() {
    Review review = new Review();
    review.setText("something");
    review.setScore(3);
    return review;
  }
}
