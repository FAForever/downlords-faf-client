package com.faforever.client.login;

import com.faforever.client.legacy.domain.NoticeInfo;

public class LoginFailedException extends RuntimeException {

  public LoginFailedException(NoticeInfo notice) {
    super(notice.text);
  }
}
