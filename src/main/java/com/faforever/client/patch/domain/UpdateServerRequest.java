package com.faforever.client.patch.domain;

import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.patch.UpdateServerCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UpdateServerRequest implements SerializableMessage {

  private final UpdateServerCommand updateServerCommand;
  private final List<Object> args;

  protected UpdateServerRequest(UpdateServerCommand updateServerCommand) {
    this.updateServerCommand = updateServerCommand;
    args = new ArrayList<>(5);
  }

  public UpdateServerCommand getUpdateServerCommand() {
    return updateServerCommand;
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public List<Object> getArgs() {
    return args;
  }

  protected void addArg(Object arg) {
    args.add(arg);
  }
}
