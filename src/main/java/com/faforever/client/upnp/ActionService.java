package com.faforever.client.upnp;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.protocol.sync.SendingAction;

import java.net.URL;

public class ActionService {

  private final RemoteService remoteService;
  private final ControlPoint controlPoint;

  public ActionService(final RemoteService remoteService, final ControlPoint controlPoint) {
    this.remoteService = remoteService;
    this.controlPoint = controlPoint;
  }

  public <T> T run(final ClingAction<T> action) {
    // Figure out the remote URL where we'd like to send the action request to
    final URL controLURL = remoteService.getDevice().normalizeURI(remoteService.getControlURI());

    // Do it
    final ActionInvocation<RemoteService> actionInvocation = action.getActionInvocation();
    final SendingAction prot = controlPoint.getProtocolFactory().createSendingAction(actionInvocation, controLURL);
    prot.run();

    final IncomingActionResponseMessage response = prot.getOutputMessage();
    if (response == null) {
      throw new IllegalStateException("Got null response");
    } else if (response.getOperation().isFailed()) {
      throw new IllegalStateException("Invocation " + actionInvocation + " failed with operation '"
          + response.getOperation() + "', body '" + response.getBodyString() + "'");
    }
    return action.convert(actionInvocation);
  }

  public RemoteService getService() {
    return remoteService;
  }
}
