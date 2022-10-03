package com.faforever.client.player;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


public class CountryFlagServiceTest extends ServiceTest {

  @InjectMocks
  private CountryFlagService instance;

  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() {
    when(i18n.getCountryNameLocalized(anyString())).thenReturn(""); //Default result to avoid nullPointers
  }

  @Test
  public void shouldFindOnCountryMatch() {
    //Arrange
    Locale.setDefault(new Locale("da", "DK"));
    String[] inputs = {"d", "D", "dk", "DK"};
    for (String input : inputs) {
      //Act
      List<String> result = instance.getISOCountries(input);

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
    List<String> result = instance.getISOCountries(input);

    //Assert
    assertThat(result, hasItem("DK"));
  }

  @Test
  public void shouldFindAllOnEmptyString() {
    //Arrange
    //Act
    List<String> result = instance.getISOCountries("");

    //Assert
    assertThat(result, hasItems("DK", "DE", "GB", "AU", "BE")); //just a list of countries to match for
  }

  @Test
  public void shouldReturnAllCountriesOnNullInput() {
    //Arrange
    //Act
    List<String> result = instance.getISOCountries(null);

    //Assert
    assertThat(result, hasItems("DK", "DE", "GB", "AU", "BE")); //just a list of countries to match for
  }
}