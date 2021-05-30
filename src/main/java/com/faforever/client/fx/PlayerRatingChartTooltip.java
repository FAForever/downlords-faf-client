package com.faforever.client.fx;

import javafx.scene.Node;

public interface PlayerRatingChartTooltip extends Controller<Node> {

  void setXY(long dateValueInSec, int rating);

  void clear();
}
