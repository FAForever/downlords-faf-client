package com.faforever.client.legacy.domain;

/**
 * The server sends this as "featured mod", however this is confusing with other mods which is why it's called game type
 * here.
 */
public class GameTypeInfo extends ServerObject {

  public boolean join;
  public String name;
  public boolean live;
  public boolean host;
  public String fullname;
  public String desc;
  public Boolean[] options;
  public String icon;
}
