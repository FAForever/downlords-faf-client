package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class UserListSettingsController implements Controller<VBox> {

  private final PreferencesService preferencesService;

  public VBox root;
  public CheckBox showMapNameCheckBox;
  public CheckBox showMapPreviewCheckBox;

  private InvalidationListener selectedPropertyListener;

  @Override
  public void initialize() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.bindBidirectional(showMapNameCheckBox.selectedProperty(), chatPrefs.showMapNameProperty());
    JavaFxUtil.bindBidirectional(showMapPreviewCheckBox.selectedProperty(), chatPrefs.showMapPreviewProperty());

    selectedPropertyListener = observable -> preferencesService.storeInBackground();
    WeakInvalidationListener selectedPropertyWeakListener = new WeakInvalidationListener(selectedPropertyListener);
    JavaFxUtil.addListener(showMapNameCheckBox.selectedProperty(), selectedPropertyWeakListener);
    JavaFxUtil.addListener(showMapPreviewCheckBox.selectedProperty(), selectedPropertyWeakListener);
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
