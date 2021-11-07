package com.faforever.client.builders;

import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.SubdivisionBean;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;

public class SubdivisionBeanBuilder {
  private final SubdivisionBean subdivisionBean = new SubdivisionBean();

  public static SubdivisionBeanBuilder create() {
    return new SubdivisionBeanBuilder();
  }

  public SubdivisionBeanBuilder defaultValues() {
    leagueSeasonId(0);
    index(1);
    nameKey("I");
    descriptionKey("test_description");
    highestScore(10);
    maxRating(100);
    minRating(-100);
    division(DivisionBeanBuilder.create().defaultValues().get());
    try {
      imageUrl(new URL("https:example.com/test_nameI.png"));
    } catch (MalformedURLException ignored) {}
    try {
      mediumImageUrl(new URL("https:example.com/medium/test_nameI.png"));
    } catch (MalformedURLException ignored) {}
    try {
      smallImageUrl(new URL("https:example.com/small/test_nameI.png"));
    } catch (MalformedURLException ignored) {}
    id(1);
    return this;
  }

  public SubdivisionBeanBuilder leagueSeasonId(int leagueSeasonId) {
    subdivisionBean.setLeagueSeasonId(leagueSeasonId);
    return this;
  }

  public SubdivisionBeanBuilder index(int index) {
    subdivisionBean.setIndex(index);
    return this;
  }

  public SubdivisionBeanBuilder nameKey(String nameKey) {
    subdivisionBean.setNameKey(nameKey);
    return this;
  }

  public SubdivisionBeanBuilder descriptionKey(String descriptionKey) {
    subdivisionBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public SubdivisionBeanBuilder highestScore(int highestScore) {
    subdivisionBean.setHighestScore(highestScore);
    return this;
  }

  public SubdivisionBeanBuilder maxRating(int maxRating) {
    subdivisionBean.setMaxRating(maxRating);
    return this;
  }

  public SubdivisionBeanBuilder minRating(int minRating) {
    subdivisionBean.setMinRating(minRating);
    return this;
  }

  public SubdivisionBeanBuilder division(DivisionBean division) {
    subdivisionBean.setDivision(division);
    return this;
  }

  public SubdivisionBeanBuilder imageUrl(URL imageUrl) {
    subdivisionBean.setImageUrl(imageUrl);
    return this;
  }

  public SubdivisionBeanBuilder mediumImageUrl(URL mediumImageUrl) {
    subdivisionBean.setMediumImageUrl(mediumImageUrl);
    return this;
  }

  public SubdivisionBeanBuilder smallImageUrl(URL smallImageUrl) {
    subdivisionBean.setSmallImageUrl(smallImageUrl);
    return this;
  }

  public SubdivisionBeanBuilder id(Integer id) {
    subdivisionBean.setId(id);
    return this;
  }

  public SubdivisionBeanBuilder createTime(OffsetDateTime createTime) {
    subdivisionBean.setCreateTime(createTime);
    return this;
  }

  public SubdivisionBeanBuilder updateTime(OffsetDateTime updateTime) {
    subdivisionBean.setUpdateTime(updateTime);
    return this;
  }

  public SubdivisionBean get() {
    return subdivisionBean;
  }


}
