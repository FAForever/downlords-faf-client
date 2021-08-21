package com.faforever.client.builders;

import com.faforever.client.domain.NameRecordBean;

import java.time.OffsetDateTime;


public class NameRecordBeanBuilder {
  public static NameRecordBeanBuilder create() {
    return new NameRecordBeanBuilder();
  }

  private final NameRecordBean nameRecordBean = new NameRecordBean();

  public NameRecordBeanBuilder defaultValues() {
    id(0);
    name("old");
    changeTime(OffsetDateTime.now());
    return this;
  }

  public NameRecordBeanBuilder id(Integer id) {
    nameRecordBean.setId(id);
    return this;
  }

  public NameRecordBeanBuilder name(String name) {
    nameRecordBean.setName(name);
    return this;
  }

  public NameRecordBeanBuilder changeTime(OffsetDateTime changeTime) {
    nameRecordBean.setChangeTime(changeTime);
    return this;
  }

  public NameRecordBean get() {
    return nameRecordBean;
  }

}

