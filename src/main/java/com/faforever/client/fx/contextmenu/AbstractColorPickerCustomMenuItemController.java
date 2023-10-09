package com.faforever.client.fx.contextmenu;

import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public abstract class AbstractColorPickerCustomMenuItemController<T> extends AbstractCustomMenuItemController<T> {


  public ColorPicker colorPicker;
  public Button removeCustomColorButton;
}
