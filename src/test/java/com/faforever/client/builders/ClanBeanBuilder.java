package com.faforever.client.builders;

import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;
import java.util.List;


public class ClanBeanBuilder {
  public static ClanBeanBuilder create() {
    return new ClanBeanBuilder();
  }

  private final ClanBean clanBean = new ClanBean();

  public ClanBeanBuilder defaultValues() {
    id(0);
    description("test clan");
    founder(PlayerBeanBuilder.create().defaultValues().id(100).username("founder").get());
    leader(PlayerBeanBuilder.create().defaultValues().id(100).username("leader").get());
    name("test");
    tag("tst");
    members(List.of());
    return this;
  }

  public ClanBeanBuilder description(String description) {
    clanBean.setDescription(description);
    return this;
  }

  public ClanBeanBuilder founder(PlayerBean founder) {
    clanBean.setFounder(founder);
    return this;
  }

  public ClanBeanBuilder leader(PlayerBean leader) {
    clanBean.setLeader(leader);
    return this;
  }

  public ClanBeanBuilder name(String name) {
    clanBean.setName(name);
    return this;
  }

  public ClanBeanBuilder tag(String tag) {
    clanBean.setTag(tag);
    return this;
  }

  public ClanBeanBuilder tagColor(String tagColor) {
    clanBean.setTagColor(tagColor);
    return this;
  }

  public ClanBeanBuilder websiteUrl(String websiteUrl) {
    clanBean.setWebsiteUrl(websiteUrl);
    return this;
  }

  public ClanBeanBuilder members(List<PlayerBean> members) {
    clanBean.setMembers(members);
    return this;
  }

  public ClanBeanBuilder id(Integer id) {
    clanBean.setId(id);
    return this;
  }

  public ClanBeanBuilder createTime(OffsetDateTime createTime) {
    clanBean.setCreateTime(createTime);
    return this;
  }

  public ClanBeanBuilder updateTime(OffsetDateTime updateTime) {
    clanBean.setUpdateTime(updateTime);
    return this;
  }

  public ClanBean get() {
    return clanBean;
  }

}

