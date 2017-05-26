package com.faforever.client.connectivity;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.upnp.UpnpService;
import com.faforever.client.util.Assert;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UpnpPortForwardingTask extends CompletableTask<Void> {

  @Resource
  I18n i18n;
  @Resource
  UpnpService upnpService;

  private Integer port;

  public UpnpPortForwardingTask() {
    super(Priority.LOW);
  }

  @Override
  protected Void call() throws Exception {
    Assert.checkNullIllegalState(port, "port has not been set");
    updateTitle(i18n.get("portCheckTask.tryingUpnp"));
    upnpService.forwardPort(port);
    return null;
  }

  public void setPort(Integer port) {
    this.port = port;
  }
}
