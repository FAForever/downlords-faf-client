package com.faforever.client.vault.review;

import com.faforever.client.remote.FafService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
// TODO remove interfaces from other services as well
public class ReviewService {
  private final FafService fafService;

  public ReviewService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<Void> saveGameReview(Review review, int gameId) {
    return fafService.saveGameReview(review, gameId);
  }

  public CompletableFuture createModVersionReview(Review review, int modVersionId) {
    return fafService.saveModVersionReview(review, modVersionId);
  }

  public CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId) {
    return fafService.saveMapVersionReview(review, mapVersionId);
  }

  public CompletableFuture<Void> deleteGameReview(Review review) {
    return fafService.deleteGameReview(review);
  }

  public CompletableFuture<Void> deleteMapVersionReview(Review review) {
    return fafService.deleteMapVersionReview(review);
  }
}
