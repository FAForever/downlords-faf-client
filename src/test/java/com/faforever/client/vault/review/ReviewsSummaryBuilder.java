package com.faforever.client.vault.review;


public class ReviewsSummaryBuilder {
  private final ReviewsSummary reviewsSummary = new ReviewsSummary();

  public static ReviewsSummaryBuilder create() {
    return new ReviewsSummaryBuilder();
  }

  public ReviewsSummaryBuilder defaultValues() {
    id("test");
    positive(1.0f);
    negative(0.0f);
    score(.5f);
    reviews(10);
    lowerBound(.5f);
    return this;
  }


  public ReviewsSummaryBuilder id(String id) {
    reviewsSummary.setId(id);
    return this;
  }

  public ReviewsSummaryBuilder positive(float positive) {
    reviewsSummary.setPositive(positive);
    return this;
  }

  public ReviewsSummaryBuilder negative(float negative) {
    reviewsSummary.setNegative(negative);
    return this;
  }

  public ReviewsSummaryBuilder score(float score) {
    reviewsSummary.setScore(score);
    return this;
  }

  public ReviewsSummaryBuilder reviews(int reviews) {
    reviewsSummary.setReviews(reviews);
    return this;
  }

  public ReviewsSummaryBuilder lowerBound(float lowerBound) {
    reviewsSummary.setLowerBound(lowerBound);
    return this;
  }

  public ReviewsSummary get() {
    return reviewsSummary;
  }
}
