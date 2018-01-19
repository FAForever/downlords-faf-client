package com.faforever.client.mod;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ReviewsSummary {
  private final StringProperty id;
  private final FloatProperty positive;
  private final FloatProperty negative;
  private final FloatProperty score;
  private final IntegerProperty reviews;
  private final FloatProperty lowerBound;

  public ReviewsSummary() {
    id = new SimpleStringProperty();
    positive = new SimpleFloatProperty();
    negative = new SimpleFloatProperty();
    score = new SimpleFloatProperty();
    reviews = new SimpleIntegerProperty();
    lowerBound = new SimpleFloatProperty();
  }

  public static ReviewsSummary fromDto(com.faforever.client.api.dto.ReviewsSummary dto) {
    ReviewsSummary reviewsSummary = new ReviewsSummary();
    if (dto != null) {
      reviewsSummary.setId(dto.getId());
      reviewsSummary.setPositive(dto.getPositive());
      reviewsSummary.setNegative(dto.getNegative());
      reviewsSummary.setScore(dto.getScore());
      reviewsSummary.setLowerBound(dto.getLowerBound());
    }
    return reviewsSummary;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }

  public float getPositive() {
    return positive.get();
  }

  public void setPositive(float positive) {
    this.positive.set(positive);
  }

  public FloatProperty positiveProperty() {
    return positive;
  }

  public float getNegative() {
    return negative.get();
  }

  public void setNegative(float negative) {
    this.negative.set(negative);
  }

  public FloatProperty negativeProperty() {
    return negative;
  }

  public float getScore() {
    return score.get();
  }

  public void setScore(float score) {
    this.score.set(score);
  }

  public FloatProperty scoreProperty() {
    return score;
  }

  public int getReviews() {
    return reviews.get();
  }

  public void setReviews(int reviews) {
    this.reviews.set(reviews);
  }

  public IntegerProperty reviewsProperty() {
    return reviews;
  }

  public float getLowerBound() {
    return lowerBound.get();
  }

  public void setLowerBound(float lowerBound) {
    this.lowerBound.set(lowerBound);
  }

  public FloatProperty lowerBoundProperty() {
    return lowerBound;
  }
}
