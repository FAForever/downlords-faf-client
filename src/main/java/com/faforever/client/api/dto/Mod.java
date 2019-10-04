package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mod")
@NoArgsConstructor
public class Mod {

  @Id
  private String id;
  private String displayName;
  private String author;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("uploader")
  private Player uploader;

  @Relationship("versions")
  private List<ModVersion> versions;

  @Relationship("latestVersion")
  private ModVersion latestVersion;

  public Mod(String id, String displayName, String author, OffsetDateTime createTime, OffsetDateTime updateTime, Player uploader, List<ModVersion> versions, ModVersion latestVersion) {
    this.id = id;
    this.displayName = displayName;
    this.author = author;
    this.createTime = createTime;
    this.updateTime = updateTime;
    this.uploader = uploader;
    this.versions = versions;
    this.latestVersion = latestVersion;
  }
}
