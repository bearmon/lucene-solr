package org.apache.lucene.document;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import org.apache.lucene.search.NumericRangeQuery; // for javadocs
import org.apache.lucene.util.NumericUtils; // for javadocs

/**
 * Provides support for converting dates to strings and vice-versa.
 * The strings are structured so that lexicographic sorting orders 
 * them by date, which makes them suitable for use as field values 
 * and search terms.
 * 
 * <P>This class also helps you to limit the resolution of your dates. Do not
 * save dates with a finer resolution than you really need, as then
 * RangeQuery and PrefixQuery will require more memory and become slower.
 * 
 * <P>
 * Another approach is {@link NumericUtils}, which provides
 * a sortable binary representation (prefix encoded) of numeric values, which
 * date/time are.
 * For indexing a {@link Date} or {@link Calendar}, just get the unix timestamp as
 * <code>long</code> using {@link Date#getTime} or {@link Calendar#getTimeInMillis} and
 * index this as a numeric value with {@link NumericField}
 * and use {@link NumericRangeQuery} to query it.
 */
public class DateTools {
  
  private final static TimeZone GMT = TimeZone.getTimeZone("GMT");

  private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.US);
  private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyyMM", Locale.US);
  private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);
  private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("yyyyMMddHH", Locale.US);
  private static final SimpleDateFormat MINUTE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
  private static final SimpleDateFormat SECOND_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  private static final SimpleDateFormat MILLISECOND_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);
  static {
    // times need to be normalized so the value doesn't depend on the 
    // location the index is created/used:
    YEAR_FORMAT.setTimeZone(GMT);
    MONTH_FORMAT.setTimeZone(GMT);
    DAY_FORMAT.setTimeZone(GMT);
    HOUR_FORMAT.setTimeZone(GMT);
    MINUTE_FORMAT.setTimeZone(GMT);
    SECOND_FORMAT.setTimeZone(GMT);
    MILLISECOND_FORMAT.setTimeZone(GMT);
  }

  private static final Calendar calInstance = Calendar.getInstance(GMT);
  
  // cannot create, the class has static methods only
  private DateTools() {}

  /**
   * Converts a Date to a string suitable for indexing.
   * 
   * @param date the date to be converted
   * @param resolution the desired resolution, see
   *  {@link #round(Date, DateTools.Resolution)}
   * @return a string in format <code>yyyyMMddHHmmssSSS</code> or shorter,
   *  depending on <code>resolution</code>; using GMT as timezone 
   */
  public static synchronized String dateToString(Date date, Resolution resolution) {
    return timeToString(date.getTime(), resolution);
  }

  /**
   * Converts a millisecond time to a string suitable for indexing.
   * 
   * @param time the date expressed as milliseconds since January 1, 1970, 00:00:00 GMT
   * @param resolution the desired resolution, see
   *  {@link #round(long, DateTools.Resolution)}
   * @return a string in format <code>yyyyMMddHHmmssSSS</code> or shorter,
   *  depending on <code>resolution</code>; using GMT as timezone
   */
  public static synchronized String timeToString(long time, Resolution resolution) {
    calInstance.setTimeInMillis(round(time, resolution));
    Date date = calInstance.getTime();
    
    if (resolution == Resolution.YEAR) {
      return YEAR_FORMAT.format(date);
    } else if (resolution == Resolution.MONTH) {
      return MONTH_FORMAT.format(date);
    } else if (resolution == Resolution.DAY) {
      return DAY_FORMAT.format(date);
    } else if (resolution == Resolution.HOUR) {
      return HOUR_FORMAT.format(date);
    } else if (resolution == Resolution.MINUTE) {
      return MINUTE_FORMAT.format(date);
    } else if (resolution == Resolution.SECOND) {
      return SECOND_FORMAT.format(date);
    } else if (resolution == Resolution.MILLISECOND) {
      return MILLISECOND_FORMAT.format(date);
    }
    
    throw new IllegalArgumentException("unknown resolution " + resolution);
  }
  
  /**
   * Converts a string produced by <code>timeToString</code> or
   * <code>dateToString</code> back to a time, represented as the
   * number of milliseconds since January 1, 1970, 00:00:00 GMT.
   * 
   * @param dateString the date string to be converted
   * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * @throws ParseException if <code>dateString</code> is not in the 
   *  expected format 
   */
  public static synchronized long stringToTime(String dateString) throws ParseException {
    return stringToDate(dateString).getTime();
  }

  /**
   * Converts a string produced by <code>timeToString</code> or
   * <code>dateToString</code> back to a time, represented as a
   * Date object.
   * 
   * @param dateString the date string to be converted
   * @return the parsed time as a Date object 
   * @throws ParseException if <code>dateString</code> is not in the 
   *  expected format 
   */
  public static synchronized Date stringToDate(String dateString) throws ParseException {
    if (dateString.length() == 4) {
      return YEAR_FORMAT.parse(dateString);
    } else if (dateString.length() == 6) {
      return MONTH_FORMAT.parse(dateString);
    } else if (dateString.length() == 8) {
      return DAY_FORMAT.parse(dateString);
    } else if (dateString.length() == 10) {
      return HOUR_FORMAT.parse(dateString);
    } else if (dateString.length() == 12) {
      return MINUTE_FORMAT.parse(dateString);
    } else if (dateString.length() == 14) {
      return SECOND_FORMAT.parse(dateString);
    } else if (dateString.length() == 17) {
      return MILLISECOND_FORMAT.parse(dateString);
    }
    throw new ParseException("Input is not valid date string: " + dateString, 0);
  }
  
  /**
   * Limit a date's resolution. For example, the date <code>2004-09-21 13:50:11</code>
   * will be changed to <code>2004-09-01 00:00:00</code> when using
   * <code>Resolution.MONTH</code>. 
   * 
   * @param resolution The desired resolution of the date to be returned
   * @return the date with all values more precise than <code>resolution</code>
   *  set to 0 or 1
   */
  public static synchronized Date round(Date date, Resolution resolution) {
    return new Date(round(date.getTime(), resolution));
  }
  
  /**
   * Limit a date's resolution. For example, the date <code>1095767411000</code>
   * (which represents 2004-09-21 13:50:11) will be changed to 
   * <code>1093989600000</code> (2004-09-01 00:00:00) when using
   * <code>Resolution.MONTH</code>.
   * 
   * @param resolution The desired resolution of the date to be returned
   * @return the date with all values more precise than <code>resolution</code>
   *  set to 0 or 1, expressed as milliseconds since January 1, 1970, 00:00:00 GMT
   */
  public static synchronized long round(long time, Resolution resolution) {
    calInstance.setTimeInMillis(time);
    
    if (resolution == Resolution.YEAR) {
      calInstance.set(Calendar.MONTH, 0);
      calInstance.set(Calendar.DAY_OF_MONTH, 1);
      calInstance.set(Calendar.HOUR_OF_DAY, 0);
      calInstance.set(Calendar.MINUTE, 0);
      calInstance.set(Calendar.SECOND, 0);
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.MONTH) {
      calInstance.set(Calendar.DAY_OF_MONTH, 1);
      calInstance.set(Calendar.HOUR_OF_DAY, 0);
      calInstance.set(Calendar.MINUTE, 0);
      calInstance.set(Calendar.SECOND, 0);
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.DAY) {
      calInstance.set(Calendar.HOUR_OF_DAY, 0);
      calInstance.set(Calendar.MINUTE, 0);
      calInstance.set(Calendar.SECOND, 0);
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.HOUR) {
      calInstance.set(Calendar.MINUTE, 0);
      calInstance.set(Calendar.SECOND, 0);
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.MINUTE) {
      calInstance.set(Calendar.SECOND, 0);
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.SECOND) {
      calInstance.set(Calendar.MILLISECOND, 0);
    } else if (resolution == Resolution.MILLISECOND) {
      // don't cut off anything
    } else {
      throw new IllegalArgumentException("unknown resolution " + resolution);
    }
    return calInstance.getTimeInMillis();
  }

  /** Specifies the time granularity. */
  public static class Resolution {
    
    public static final Resolution YEAR = new Resolution("year");
    public static final Resolution MONTH = new Resolution("month");
    public static final Resolution DAY = new Resolution("day");
    public static final Resolution HOUR = new Resolution("hour");
    public static final Resolution MINUTE = new Resolution("minute");
    public static final Resolution SECOND = new Resolution("second");
    public static final Resolution MILLISECOND = new Resolution("millisecond");

    private String resolution;

    private Resolution() {
    }
    
    private Resolution(String resolution) {
      this.resolution = resolution;
    }
    
    @Override
    public String toString() {
      return resolution;
    }

  }

}
