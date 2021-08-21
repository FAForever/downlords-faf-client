package com.faforever.client.domain;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public abstract class ReviewsSummaryBean {
  @EqualsAndHashCode.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  FloatProperty positive = new SimpleFloatProperty();
  FloatProperty negative = new SimpleFloatProperty();
  FloatProperty score = new SimpleFloatProperty();
  IntegerProperty reviews = new SimpleIntegerProperty();
  FloatProperty lowerBound = new SimpleFloatProperty();

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
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
