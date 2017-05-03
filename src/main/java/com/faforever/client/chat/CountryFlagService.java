package com.faforever.client.chat;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.Optional;

public interface CountryFlagService {

  Optional<Image> loadCountryFlag(String country);

  Optional<URL> getCountryFlagUrl(String country);
}
