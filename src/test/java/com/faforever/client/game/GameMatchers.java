package com.faforever.client.game;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Matchers to assert Game properties
 */
public class GameMatchers {

  static Matcher<Game> hasId(Integer id) {
    return new FeatureMatcher<>(equalTo(id), "Integer", "id") {
      @Override
      protected Integer featureValueOf(Game actual) {
        return actual.getId();
      }
    };
  }

  static Matcher<Game> hasTitle(String title) {
    return new FeatureMatcher<>(equalTo(title), "String", "title") {
      @Override
      protected String featureValueOf(Game actual) {
        return actual.getTitle();
      }
    };
  }
}
