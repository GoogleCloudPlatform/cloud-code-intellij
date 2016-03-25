/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.common.base.Objects;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Represents the data of a single deployed application version
 */
public class AppEngineModuleListItem implements Comparable<AppEngineModuleListItem> {

  private String moduleName;
  private String version;
  private DateTime versionDate;
  private Long versionTime;
  private Double trafficSplit;

  private static final String VERSION_DATE_PATTERN = "yyyyMMdd";

  public AppEngineModuleListItem(
      String moduleName,
      String version,
      String versionDate,
      String versionTime,
      String trafficSplit) {
    this.moduleName = moduleName;
    this.version = version;

    DateTimeFormatter fmt = DateTimeFormat.forPattern(VERSION_DATE_PATTERN);
    this.versionDate = fmt.parseDateTime(versionDate);

    this.versionTime = Long.parseLong(versionTime);
    this.trafficSplit = Double.parseDouble(trafficSplit);
  }

  public Double getTrafficSplit() {
    return trafficSplit;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getVersion() {
    return version;
  }

  /**
   * Compares instances of this class using the following criteria evaluated in order:
   * 1) Compare traffic split, if one is 0 then its lower, otherwise:
   * 2) Compare dates, if equal then:
   * 3) Compare time stamp
   *
   * @param that the object to compare to
   * @return @see {@link Comparable#compareTo(Object)}
   */
  @Override
  public int compareTo(@NotNull AppEngineModuleListItem that) {
    int trafficSplitCompare;
    if(this.trafficSplit == 0) {
      trafficSplitCompare = -1;
    } else if(that.trafficSplit == 0) {
      trafficSplitCompare = 1;
    } else {
      trafficSplitCompare = 0;
    }

    int dateCompare = this.versionDate.compareTo(that.versionDate);

    if (trafficSplitCompare != 0) {
      return trafficSplitCompare;
    } else if (dateCompare != 0) {
        return dateCompare;
    } else {
      return this.versionTime.compareTo(that.versionTime);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AppEngineModuleListItem that = (AppEngineModuleListItem) o;

    return Objects.equal(this.moduleName, that.moduleName)
        && Objects.equal(this.version, that.version)
        && Objects.equal(this.versionDate, that.versionDate)
        && Objects.equal(this.versionTime, that.versionTime)
        && Objects.equal(this.trafficSplit, that.trafficSplit);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        moduleName,
        version,
        versionDate,
        versionTime,
        trafficSplit
    );
  }
}
