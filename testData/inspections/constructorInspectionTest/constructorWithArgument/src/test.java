package com.example.app;

import com.google.api.server.spi.config.Api;

import java.lang.String;


@Api
public class Foo {
  String name;
  public Foo (String name) {
    this.name = name;
  }
}