package com.faforever.client.login;

import com.faforever.client.legacy.domain.Notice;

public class LoginFailedException extends RuntimeException {

  public LoginFailedException(Notice notice) {
    super(notice.text);
  }
}
