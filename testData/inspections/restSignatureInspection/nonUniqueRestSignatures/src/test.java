/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.app;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.CollectionResponse;

import java.lang.Boolean;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;


@Api
public class MyClass {
  // "POST boo"
  public void boo(Byte[] param) {
    // do nothing
  }

  // "POST boo"
  public void boo(Boolean param) {
    // do nothing
  }

  // "GET function1/{}"
  @ApiMethod(path="", httpMethod = "GET")
  public Foo function1(@Nullable @Named("id") int id){
    return  null;
  }

  // "GET function1/{}"
  @ApiMethod(path="", httpMethod = "GET")
  public Foo function1(@Nullable @Named("id") double id){
    return  null;
  }

  // "POST function2/{}"
  @ApiMethod(path="", httpMethod = "")
  public Foo function2(@Named("id") int id){
    return  null;
  }

  // "POST function2/{}"
  @ApiMethod(path="", httpMethod = "")
  public Foo function2(@Named("id") double id){
    return  null;
  }

  // "GET foo/{}"
  @ApiMethod(path="", httpMethod = "GET")
  public List<Foo> list1(@Named("id") int id){
    return  null;
  }

  // "GET foo/{}"
  @ApiMethod(path="", httpMethod = "GET")
  public List<Foo> list1(@Named("id") double id){
    return  null;
  }

  // GET "list2"
  @ApiMethod(path="", httpMethod = "")
  public ArrayListOfString list2(@DefaultValue @Named("id") int id){
    return  null;
  }

  // GET "list2"
  @ApiMethod(path="", httpMethod = "")
  public ArrayListOfString list2(@DefaultValue @Named("id") double id){
    return  null;
  }

  // GET "list3"
  @ApiMethod(path="", httpMethod = "")
  public Param<String> list3(@DefaultValue @Named("id") int id){
    return  null;
  }

  // GET "list3"
  @ApiMethod(path="", httpMethod = "")
  public Param<String> list3(@DefaultValue @Named("id") double id){
    return  null;
  }

  // "GET foocollection/{}"
  @ApiMethod( path="", httpMethod = "GET")
  public List<Foo> get1(@Named("id") int id){
    return  null;
  }

  // "GET foocollection/{}"
  @ApiMethod( path="", httpMethod = "GET")
  public List<Foo> get1(@Named("id") double id){
    return  null;
  }

  // "GET foocollection"
  @ApiMethod(path="", httpMethod = "GET")
  public List<Foo> insert1(@DefaultValue @Named("id") int id){
    return  null;
  }

  // "GET foocollection"
  @ApiMethod(path="", httpMethod = "GET")
  public List<Foo> insert1(@DefaultValue @Named("id") double id){
    return  null;
  }

  // "GET foocollection"
  @ApiMethod(path="", httpMethod = "POST")
  public List<Foo> update1(@DefaultValue @Named("id") int id){
    return  null;
  }

  // "GET foocollection"
  @ApiMethod(path="", httpMethod = "POST")
  public List<Foo> update1(@DefaultValue @Named("id") double id){
    return  null;
  }

  // "GET 1"
  @ApiMethod(path="", httpMethod = "GET")
  public Collection<Foo> delete1(@DefaultValue @Named("id") String id){
    return  null;
  }

  // "GET 1"
  @ApiMethod(path="", httpMethod = "GET")
  public Collection<Foo> delete1(@DefaultValue @Named("id") double id){
    return  null;
  }

  // "DELETE 1"
  @ApiMethod(path="", httpMethod = "")
  public Collection<Foo> remove1(@DefaultValue @Named("id") String id){
    return  null;
  }

  // "DELETE 1"
  @ApiMethod(path="", httpMethod = "")
  public Collection<Foo> remove1(@DefaultValue @Named("id") double id){
    return  null;
  }

  // "PUT collectionresponse_foo"
  @ApiMethod(path="", httpMethod = "")
  public CollectionResponse<Foo> update2(@DefaultValue @Named("id") String id){
    return  null;
  }

  // "PUT collectionresponse_foo"
  @ApiMethod(path="", httpMethod = "")
  public CollectionResponse<Foo> update2(@DefaultValue @Named("id") int id){
    return  null;
  }

  public class Foo{}
  public class ArrayListOfString extends ArrayList<String> {}
  public class Param<T> {}

}