package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AvatarMessage extends FafServerMessage {

  @SerializedName("avatarlist")
  private List<Avatar> avatarList;

  public List<Avatar> getAvatarList() {
    return avatarList;
  }
}
