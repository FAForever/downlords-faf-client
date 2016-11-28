package com.faforever.client.vault.map;

import com.faforever.client.fx.Controller;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapPreviewTableCellController implements Controller<ImageView> {
  public ImageView mapPreviewTableCellRoot;

  @Override
  public ImageView getRoot() {
    return mapPreviewTableCellRoot;
  }
}
