package com.faforever.client.vault.review;

import com.faforever.client.remote.FafService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {
  private ReviewService instance;

  @Mock
  private FafService fafService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ReviewService(fafService);
  }

  @Test
  public void saveGameReview() throws Exception {
    Review review = createReview();
    when(fafService.saveGameReview(review, 2)).thenReturn(CompletableFuture.completedFuture(null));

    instance.saveGameReview(review, 2);
    verify(fafService).saveGameReview(review, 2);
  }

  @Test
  public void saveMapVersionReview() throws Exception {
    Review review = createReview();
    when(fafService.saveMapVersionReview(review, "2")).thenReturn(CompletableFuture.completedFuture(null));

    instance.saveMapVersionReview(review, "2");
    verify(fafService).saveMapVersionReview(review, "2");
  }

  @Test
  public void saveModVersionReview() throws Exception {
    Review review = createReview();
    when(fafService.saveModVersionReview(review, "2")).thenReturn(CompletableFuture.completedFuture(null));

    instance.saveModVersionReview(review, "2");
    verify(fafService).saveModVersionReview(review, "2");
  }

  private Review createReview() {
    Review review = new Review();
    review.setText("something");
    review.setScore(3);
    return review;
  }
}
