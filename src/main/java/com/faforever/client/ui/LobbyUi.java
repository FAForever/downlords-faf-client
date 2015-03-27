package com.faforever.client.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Paint;

public class LobbyUi extends Scene {

  public LobbyUi(Parent parent) {
    super(parent);
  }

  public LobbyUi(Parent parent, double v, double v1) {
    super(parent, v, v1);
  }

  public LobbyUi(Parent parent, Paint paint) {
    super(parent, paint);
  }

  public LobbyUi(Parent parent, double v, double v1, Paint paint) {
    super(parent, v, v1, paint);
  }

  public LobbyUi(Parent parent, double v, double v1, boolean b) {
    super(parent, v, v1, b);
  }
}
