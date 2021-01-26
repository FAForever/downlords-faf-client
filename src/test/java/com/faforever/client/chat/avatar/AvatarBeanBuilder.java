package com.faforever.client.chat.avatar;

import lombok.SneakyThrows;

import java.net.URL;

public class AvatarBeanBuilder {
  private final AvatarBean avatarBean = new AvatarBean(null, null);

  public static AvatarBeanBuilder create() {
    return new AvatarBeanBuilder();
  }

  @SneakyThrows
  public AvatarBeanBuilder defaultValues() {
    avatarBean.setUrl(new URL("http://avatar.png"));
    avatarBean.setDescription("fancy");
    return this;
  }

  public AvatarBeanBuilder url(URL url) {
    avatarBean.setUrl(url);
    return this;
  }

  public AvatarBeanBuilder description(String description) {
    avatarBean.setDescription(description);
    return this;
  }

  public AvatarBean get() {
    return avatarBean;
  }
}