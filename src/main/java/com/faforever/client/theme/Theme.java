package com.faforever.client.theme;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.Properties;

public class Theme {

  private static final String DISPLAY_NAME = "displayName";
  private static final String AUTHOR = "author";
  private static final String COMPATIBILITY_VERSION = "compatibilityVersion";
  private static final String THEME_VERSION = "themeVersion";

  private final StringProperty displayName;
  private final StringProperty author;
  private final SimpleObjectProperty<Integer> compatibilityVersion;
  private final StringProperty themeVersion;

  public Theme(String displayName, String author, Integer compatibilityVersion, String themeVersion) {
    this.displayName = new SimpleStringProperty(displayName);
    this.author = new SimpleStringProperty(author);
    this.compatibilityVersion = new SimpleObjectProperty<>(compatibilityVersion);
    this.themeVersion = new SimpleStringProperty(themeVersion);
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public String getAuthor() {
    return author.get();
  }

  public void setAuthor(String author) {
    this.author.set(author);
  }

  public StringProperty authorProperty() {
    return author;
  }

  public int getCompatibilityVersion() {
    return compatibilityVersion.get();
  }

  public void setCompatibilityVersion(int compatibilityVersion) {
    this.compatibilityVersion.set(compatibilityVersion);
  }

  public SimpleObjectProperty<Integer> compatibilityVersionProperty() {
    return compatibilityVersion;
  }

  public String getThemeVersion() {
    return themeVersion.get();
  }

  public void setThemeVersion(String themeVersion) {
    this.themeVersion.set(themeVersion);
  }

  public StringProperty themeVersionProperty() {
    return themeVersion;
  }

  public static Theme fromProperties(Properties properties) {
    return new Theme(
        properties.getProperty(DISPLAY_NAME),
        properties.getProperty(AUTHOR),
        Integer.valueOf(properties.getProperty(COMPATIBILITY_VERSION)),
        properties.getProperty(THEME_VERSION)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, themeVersion);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Theme theme = (Theme) o;
    return Objects.equals(displayName, theme.displayName) &&
        Objects.equals(themeVersion, theme.themeVersion);
  }

  public Properties toProperties() {
    Properties properties = new Properties();
    properties.put(DISPLAY_NAME, displayName.get());
    properties.put(AUTHOR, author.get());
    properties.put(COMPATIBILITY_VERSION, compatibilityVersion.get());
    properties.put(THEME_VERSION, themeVersion.get());
    return properties;
  }

}
