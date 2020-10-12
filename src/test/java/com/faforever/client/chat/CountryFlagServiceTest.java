package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CountryFlagServiceTest {

  private CountryFlagService service;

  @Mock
  private I18n i18n;

  @Before
  public void setUp() {
    when(i18n.getCountryNameLocalized(anyString())).thenReturn(""); //Default result to avoid nullPointers
    service = new CountryFlagService(i18n);
  }

  @Test
  public void shouldFindOnCountryMatch() {
    //Arrange
    Locale.setDefault(new Locale("da", "DK"));
    String[] inputs = {"d", "D", "dk", "DK"};
    for (String input : inputs) {
      //Act
      List<String> result = service.getCountries(input);

      //Assert
      assertThat(result, hasItem("DK"));
    }
  }

  @Test
  public void shouldFindOnDisplayNameMatch() {
    //Arrange
    final String input = "DE";
    final String displayName = "Denmark";
    when(i18n.getCountryNameLocalized("DK")).thenReturn(displayName);
    //Act
    List<String> result = service.getCountries(input);

    //Assert
    assertThat(result, hasItem("DK"));
  }

  @Test
  public void shouldFindAllOnEmptyString() {
    //Arrange
    //Act
    List<String> result = service.getCountries("");

    //Assert
    assertThat(result, hasItems("DK", "DE", "GB", "AU", "BE")); //just a list of countries to match for
  }

  @Test
  public void shouldReturnAllCountriesOnNullInput() {
    //Arrange
    //Act
    List<String> result = service.getCountries(null);

    //Assert
    assertThat(result, hasItems("DK", "DE", "GB", "AU", "BE")); //just a list of countries to match for
  }
}