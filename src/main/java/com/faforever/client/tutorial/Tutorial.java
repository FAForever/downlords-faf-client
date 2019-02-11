package com.faforever.client.tutorial;

import com.faforever.client.map.MapBean;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Optional;

public class Tutorial {
  private final IntegerProperty id;
  private final StringProperty description;
  private final StringProperty title;
  private final ObjectProperty<TutorialCategory> category;
  private final StringProperty image;
  private final StringProperty imageUrl;
  private final IntegerProperty ordinal;
  private final BooleanProperty launchable;
  private final ObjectProperty<MapBean> mapVersion;
  private final StringProperty technicalName;


  public Tutorial() {
    id = new SimpleIntegerProperty();
    description = new SimpleStringProperty();
    title = new SimpleStringProperty();
    category = new SimpleObjectProperty<>();
    image = new SimpleStringProperty();
    ordinal = new SimpleIntegerProperty();
    launchable = new SimpleBooleanProperty();
    mapVersion = new SimpleObjectProperty<>();
    technicalName = new SimpleStringProperty();
    imageUrl = new SimpleStringProperty();
  }

  public static Tutorial fromDto(com.faforever.client.api.dto.Tutorial dto) {
    return fromDto(dto, null);
  }

  public static Tutorial fromDto(com.faforever.client.api.dto.Tutorial dto, TutorialCategory tutorialCategory) {
    Tutorial tutorial = new Tutorial();
    tutorial.setId(Integer.parseInt(dto.getId()));
    tutorial.setDescription(dto.getDescription());
    tutorial.setTitle(dto.getTitle());
    tutorial.setCategory(tutorialCategory == null ? TutorialCategory.fromDto(dto.getCategory()) : tutorialCategory);
    tutorial.setImage(dto.getImage());
    tutorial.setImageUrl(dto.getImageUrl());
    tutorial.setOrdinal(dto.getOrdinal());
    tutorial.setLaunchable(dto.isLaunchable());
    Optional.ofNullable(dto.getMapVersion()).ifPresent(mapVersionOptional -> tutorial.setMapVersion(MapBean.fromMapVersionDto(mapVersionOptional)));
    tutorial.setTechnicalName(dto.getTechnicalName());
    return tutorial;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

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

  public IntegerProperty idProperty() {
    return id;
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public TutorialCategory getCategory() {
    return category.get();
  }

  public void setCategory(TutorialCategory category) {
    this.category.set(category);
  }

  public ObjectProperty<TutorialCategory> categoryProperty() {
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

  public boolean isLaunchable() {
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

  public MapBean getMapVersion() {
    return mapVersion.get();
  }

  public void setMapVersion(MapBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  public ObjectProperty<MapBean> mapVersionProperty() {
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
