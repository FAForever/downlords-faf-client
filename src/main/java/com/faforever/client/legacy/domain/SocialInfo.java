package com.faforever.client.legacy.domain;

import java.util.List;

/**
 * Other than other server objects, the "social" command is a category of different objects like friend list, foe list
 * and a list of channels to join. Only one field is filled at a time.
 */
public class SocialInfo extends ServerObject {

  /**
   * List of user names that are friends. May be {@code null}.
   */
  public List<String> friends;

  /**
   * List of user names that are foes. May be {@code null}.
   */
  public List<String> foes;

}
