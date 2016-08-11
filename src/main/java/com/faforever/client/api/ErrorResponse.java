package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.util.List;

public class ErrorResponse {
  @Key("errors")
  private List<Error> errors;

  public List<Error> getErrors() {
    return errors;
  }

  public static class Error {
    @Key("code")
    private int code;
    @Key("title")
    private String title;
    @Key("detail")
    private String detail;
    @Key("meta")
    private java.util.Map<String, Object> meta;

    public int getCode() {
      return code;
    }

    public String getTitle() {
      return title;
    }

    public String getDetail() {
      return detail;
    }

    public java.util.Map<String, Object> getMeta() {
      return meta;
    }
  }
}
