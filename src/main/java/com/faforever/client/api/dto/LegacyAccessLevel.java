package com.faforever.client.api.dto;

import lombok.Getter;

/**
 * @deprecated AccessLevel are going to be replaced with role based security
 */
@Getter
@Deprecated
public enum LegacyAccessLevel {
  ROLE_USER,
  ROLE_MODERATOR,
  ROLE_ADMINISTRATOR
}