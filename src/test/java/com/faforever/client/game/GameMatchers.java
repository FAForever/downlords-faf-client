package com.faforever.client.game;

import com.faforever.client.domain.server.GameInfo;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Matchers to assert Game properties
 */
public class GameMatchers {

  static Matcher<GameInfo> hasId(Integer id) {
    return new FeatureMatcher<>(equalTo(id), "Integer", "id") {
      @Override
      protected Integer featureValueOf(GameInfo actual) {
        return actual.getId();
      }
    };
  }

  static Matcher<GameInfo> hasTitle(String title) {
    return new FeatureMatcher<>(equalTo(title), "String", "title") {
      @Override
      protected String featureValueOf(GameInfo actual) {
        return actual.getTitle();
      }
    };
  }
}
