package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;


public class IceAdapterPrefs {

  private final BooleanProperty forceTurnRelay = new SimpleBooleanProperty(false);
  private final BooleanProperty consentLogSharing = new SimpleBooleanProperty(true);
  private final BooleanProperty enableDebugLogging = new SimpleBooleanProperty(false);

  public boolean isForceTurnRelay() {
    return forceTurnRelay.get();
  }

  public void setForceTurnRelay(boolean forceTurnRelay) {
    this.forceTurnRelay.set(forceTurnRelay);
  }

  public BooleanProperty forceTurnRelayProperty() {
    return forceTurnRelay;
  }

  public boolean isConsentLogSharing() {
    return consentLogSharing.get();
  }

  public void setConsentLogSharing(boolean consentLogSharing) {
    this.consentLogSharing.set(consentLogSharing);
  }

  public BooleanProperty consentLogSharingProperty() {
    return consentLogSharing;
  }

  public boolean isEnableDebugLogging() {
    return enableDebugLogging.get();
  }

  public void setEnableDebugLogging(boolean enableDebugLogging) {
    this.enableDebugLogging.set(enableDebugLogging);
  }

  public BooleanProperty enableDebugLoggingProperty() {
    return enableDebugLogging;
  }
}
