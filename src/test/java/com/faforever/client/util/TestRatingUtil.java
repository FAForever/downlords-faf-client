package com.faforever.client.util;


import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestRatingUtil extends ServiceTest {
  @Test
  public void roundingDownWorks() {
    assertThat(RatingUtil.roundRatingToNextLowest100(99.99), is(0));
    assertThat(RatingUtil.roundRatingToNextLowest100(0.0), is(0));
    assertThat(RatingUtil.roundRatingToNextLowest100(42.0), is(0));

    assertThat(RatingUtil.roundRatingToNextLowest100(-99.99), is(-100));
    assertThat(RatingUtil.roundRatingToNextLowest100(-1), is(-100));
    assertThat(RatingUtil.roundRatingToNextLowest100(-100.1), is(-200));

    assertThat(RatingUtil.roundRatingToNextLowest100(199.99), is(100));
    assertThat(RatingUtil.roundRatingToNextLowest100(242), is(200));
    assertThat(RatingUtil.roundRatingToNextLowest100(2000.1), is(2000));
    assertThat(RatingUtil.roundRatingToNextLowest100(2099.9), is(2000));
  }
}
