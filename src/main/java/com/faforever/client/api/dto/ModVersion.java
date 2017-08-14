package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("modVersion")
public class ModVersion {

  @Id
  private String id;
  private String uid;
  private ModType type;
  private String description;
  private short version;
  private String filename;
  private String icon;
  private boolean ranked;
  private boolean hidden;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private URL thumbnailUrl;
  private URL downloadUrl;

  @Relationship("mod")
  private Mod mod;
}
