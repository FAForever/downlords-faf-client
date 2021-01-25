package com.faforever.client.clan;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import javafx.collections.ObservableList;

import java.time.Instant;

public class ClanBuilder {
  public static final String TEST_CLAN_TAG = "TC";

  private final Clan clan = new Clan();

  public static ClanBuilder create() {
    return new ClanBuilder();
  }

  public ClanBuilder defaultValues() {
    Player founder = PlayerBuilder.create("junit").get();
    id("e");
    description("Fun Place");
    founder(founder);
    leader(founder);
    name("test");
    tag(TEST_CLAN_TAG);
    tagColor("red");
    websiteUrl("http:\\awesome.com");
    createTime(Instant.EPOCH);
    return this;
  }

  public ClanBuilder id(String id) {
    clan.setId(id);
    return this;
  }

  public ClanBuilder description(String description) {
    clan.setDescription(description);
    return this;
  }

  public ClanBuilder founder(Player founder) {
    clan.setFounder(founder);
    return this;
  }

  public ClanBuilder leader(Player leader) {
    clan.setLeader(leader);
    return this;
  }

  public ClanBuilder name(String name) {
    clan.setName(name);
    return this;
  }

  public ClanBuilder tag(String tag) {
    clan.setTag(tag);
    return this;
  }

  public ClanBuilder tagColor(String tagColor) {
    clan.setTagColor(tagColor);
    return this;
  }

  public ClanBuilder websiteUrl(String websiteUrl) {
    clan.setWebsiteUrl(websiteUrl);
    return this;
  }

  public ClanBuilder members(ObservableList<Player> members) {
    clan.getMembers().addAll(members);
    return this;
  }

  public ClanBuilder createTime(Instant createTime) {
    clan.setCreateTime(createTime);
    return this;
  }

  public Clan get() {
    return clan;
  }

}
