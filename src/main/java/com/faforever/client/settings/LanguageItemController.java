package com.faforever.client.settings;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.google.common.base.Strings;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LanguageItemController implements Controller<Node> {

  private static final PseudoClass PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("selected");

  private final I18n i18n;
  private final CountryFlagService countryFlagService;
  public Pane languageItemRoot;
  public Label localLanguageLabel;
  public Label translatedLanguageLabel;
  public ImageView localeImageView;
  private Consumer<Locale> listener;
  private Locale locale;

  public LanguageItemController(I18n i18n, CountryFlagService countryFlagService) {
    this.i18n = i18n;
    this.countryFlagService = countryFlagService;
  }

  @Override
  public void initialize() {
    localeImageView.managedProperty().bind(localeImageView.visibleProperty());
    localeImageView.setVisible(false);
  }

  @Override
  public Node getRoot() {
    return languageItemRoot;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
    localLanguageLabel.setText(locale.getDisplayName(locale));
    translatedLanguageLabel.setText(locale.getDisplayName(i18n.getUserSpecificLocale()));

    Optional.ofNullable(Strings.emptyToNull(locale.getCountry()))
        .flatMap(countryFlagService::loadCountryFlag)
        .ifPresent(image -> {
          localeImageView.setImage(image);
          localeImageView.setVisible(true);
        });
  }

  public void setOnSelectedListener(Consumer<Locale> listener) {
    this.listener = listener;
  }

  public void setSelected(boolean selected) {
    languageItemRoot.pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, selected);
  }

  public void onSelected() {
    listener.accept(locale);
  }
}
