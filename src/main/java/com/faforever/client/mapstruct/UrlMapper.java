package com.faforever.client.mapstruct;

import org.mapstruct.Mapper;

import java.net.MalformedURLException;
import java.net.URL;

@Mapper(componentModel = "spring", config = MapperConfiguration.class)
public interface UrlMapper {
    default URL map(String string) throws MalformedURLException {
        return new URL(string);
    }

    default String map(URL url) throws MalformedURLException {
        if (url == null) {
            return null;
        }
        return url.toExternalForm();
    }
}