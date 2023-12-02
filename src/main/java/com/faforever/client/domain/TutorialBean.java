package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class TutorialBean extends AbstractEntityBean {

  @ToString.Include
  private final StringProperty title = new SimpleStringProperty();
  private final StringProperty description = new SimpleStringProperty();
  private final ObjectProperty<TutorialCategoryBean> category = new SimpleObjectProperty<>();
  private final StringProperty image = new SimpleStringProperty();
  private final StringProperty imageUrl = new SimpleStringProperty();
  private final IntegerProperty ordinal = new SimpleIntegerProperty();
  private final BooleanProperty launchable = new SimpleBooleanProperty();
  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  private final StringProperty technicalName = new SimpleStringProperty();

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public TutorialCategoryBean getCategory() {
    return category.get();
  }

  public void setCategory(TutorialCategoryBean category) {
    this.category.set(category);
  }

  public ObjectProperty<TutorialCategoryBean> categoryProperty() {
    return category;
  }

  public String getImage() {
    return image.get();
  }

  public void setImage(String image) {
    this.image.set(image);
  }

  public StringProperty imageProperty() {
    return image;
  }

  public String getImageUrl() {
    return imageUrl.get();
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl.set(imageUrl);
  }

  public int getOrdinal() {
    return ordinal.get();
  }

  public void setOrdinal(int ordinal) {
    this.ordinal.set(ordinal);
  }

  public boolean getLaunchable() {
    return launchable.get();
  }

  public void setLaunchable(boolean launchable) {
    this.launchable.set(launchable);
  }

  public BooleanProperty launchableProperty() {
    return launchable;
  }

  public StringProperty imageUrlProperty() {
    return imageUrl;
  }

  public MapVersionBean getMapVersion() {
    return mapVersion.get();
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  public ObjectProperty<MapVersionBean> mapVersionProperty() {
    return mapVersion;
  }

  public IntegerProperty ordinalProperty() {
    return ordinal;
  }

  public String getTechnicalName() {
    return technicalName.get();
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName.set(technicalName);
  }

  public StringProperty technicalNameProperty() {
    return technicalName;
  }

}
