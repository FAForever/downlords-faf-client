package com.faforever.client.fx;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.scene.control.Control;

public class WindowPaneSkin extends BehaviorSkinBase {

  /**
   * Constructor for all BehaviorSkinBase instances.
   *
   * @param control The control for which this Skin should attach to.
   * @param behavior The behavior for which this Skin should defer to.
   */
  protected WindowPaneSkin(Control control, BehaviorBase behavior) {
    super(control, behavior);
  }
}
