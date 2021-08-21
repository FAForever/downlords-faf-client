package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Matchers to assert Game properties
 */
public class GameMatchers {

  static Matcher<GameBean> hasId(Integer id) {
    return new FeatureMatcher<>(equalTo(id), "Integer", "id") {
      @Override
      protected Integer featureValueOf(GameBean actual) {
        return actual.getId();
      }
    };
  }

  static Matcher<GameBean> hasTitle(String title) {
    return new FeatureMatcher<>(equalTo(title), "String", "title") {
      @Override
      protected String featureValueOf(GameBean actual) {
        return actual.getTitle();
      }
    };
  }
}
