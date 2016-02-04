package com.faforever.client.legacy.domain;

/**
 * The server sends this as "featured mod", however this is confusing with other mods which is why it's called game type
 * here.
 */
public class GameTypeMessage extends FafServerMessage {

  private boolean join;
  private String name;
  private int publish;
  private boolean host;
  private String fullname;
  private String desc;
  private Boolean[] options;
  private String icon;

  public GameTypeMessage() {
    super(FafServerMessageType.GAME_TYPE_INFO);
  }

  public boolean isJoin() {
    return join;
  }

  public void setJoin(boolean join) {
    this.join = join;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isPublish() {
    return publish == 1;
  }

  public void setPublish(boolean publish) {
    this.publish = publish ? 1 : 0;
  }

  public boolean isHost() {
    return host;
  }

  public void setHost(boolean host) {
    this.host = host;
  }

  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Boolean[] getOptions() {
    return options;
  }

  public void setOptions(Boolean[] options) {
    this.options = options;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }
}
