package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("featuredModFile")
public class FeaturedModFile {
  @Id
  private String id;
  private String version;
  private String group;
  private String name;
  private String md5;
  private String url;
}
