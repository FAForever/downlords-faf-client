package com.faforever.client.remote.domain.outbound.gpg;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.outbound.OutboundMessage;
import com.fasterxml.jackson.annotation.JsonTypeId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class GpgOutboundMessage extends OutboundMessage {

  @JsonTypeId
  private final String command;
  private final List<Object> args;

  public GpgOutboundMessage(String command) {
    this(command, List.of());
  }

  public GpgOutboundMessage(String command, List<Object> args) {
    super(MessageTarget.GAME);
    this.command = command;
    this.args = args;
  }

  protected GpgOutboundMessage(String command, int numberOfArgs) {
    this(command, new ArrayList<>(Collections.nCopies(numberOfArgs, null)));
  }
}
