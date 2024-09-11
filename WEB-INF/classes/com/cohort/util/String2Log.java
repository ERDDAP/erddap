/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import org.apache.commons.logging.Log;

/**
 * This class redirects (currently) all messages to Apache's logging system to String2.log. This
 * class responds to and attribute called "level" and an Integer with one of the xxx_LEVEL values.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-08-25
 */
public class String2Log implements Log {

  // ----------------------------------------------------- Logging Properties

  // org.apache.commons.logging.Log defines the severity levels in a relative way.
  // Here are specific ints assigned to them.
  public static final int TRACE_LEVEL = 0;
  public static final int DEBUG_LEVEL = 1;
  public static final int INFO_LEVEL = 2;
  public static final int WARN_LEVEL = 3;
  public static final int ERROR_LEVEL = 4;
  public static final int FATAL_LEVEL = 5;
  private int level; // messages of this level and higher are sent to String2.log.

  /**
   * The constructor.
   *
   * @param level messages of this level and higher are sent to String2.log.
   */
  public String2Log(int level) {
    this.level = level;
    debug("String2Log level=" + level);
  }

  /**
   * Is debug logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than debug.
   */
  @Override
  public boolean isDebugEnabled() {
    return level <= DEBUG_LEVEL;
  }

  /**
   * Is error logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than error.
   */
  @Override
  public boolean isErrorEnabled() {
    return level <= ERROR_LEVEL;
  }

  /**
   * Is fatal logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than fatal.
   */
  @Override
  public boolean isFatalEnabled() {
    return level <= FATAL_LEVEL;
  }

  /**
   * Is info logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than info.
   */
  @Override
  public boolean isInfoEnabled() {
    return level <= INFO_LEVEL;
  }

  /**
   * Is trace logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than trace.
   */
  @Override
  public boolean isTraceEnabled() {
    return level <= TRACE_LEVEL;
  }

  /**
   * Is warn logging currently enabled?
   *
   * <p>Call this method to prevent having to perform expensive operations (for example, <code>
   * String</code> concatenation) when the log level is more than warn.
   */
  @Override
  public boolean isWarnEnabled() {
    return level <= WARN_LEVEL;
  }

  // -------------------------------------------------------- Logging Methods

  /**
   * Log a message with trace log level.
   *
   * @param message log this message
   */
  @Override
  public void trace(Object message) {
    if (level <= TRACE_LEVEL) String2.log("[TRACE] " + message);
  }

  /**
   * Log an error with trace log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void trace(Object message, Throwable t) {
    if (level <= TRACE_LEVEL)
      String2.log("[TRACE] " + message + "\n" + MustBe.throwableToString(t));
  }

  /**
   * Log a message with debug log level.
   *
   * @param message log this message
   */
  @Override
  public void debug(Object message) {
    if (level <= DEBUG_LEVEL) String2.log("[DEBUG] " + message);
  }

  /**
   * Log an error with debug log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void debug(Object message, Throwable t) {
    if (level <= DEBUG_LEVEL)
      String2.log("[DEBUG] " + message + "\n" + MustBe.throwableToString(t));
  }

  /**
   * Log a message with info log level.
   *
   * @param message log this message
   */
  @Override
  public void info(Object message) {
    if (level <= INFO_LEVEL) String2.log("[INFO] " + message);
  }

  /**
   * Log an error with info log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void info(Object message, Throwable t) {
    if (level <= INFO_LEVEL) String2.log("[INFO] " + message + "\n" + MustBe.throwableToString(t));
  }

  /**
   * Log a message with warn log level.
   *
   * @param message log this message
   */
  @Override
  public void warn(Object message) {
    if (level <= WARN_LEVEL) String2.log("[WARN] " + message);
  }

  /**
   * Log an error with warn log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void warn(Object message, Throwable t) {
    if (level <= WARN_LEVEL) String2.log("[WARN] " + message + "\n" + MustBe.throwableToString(t));
  }

  /**
   * Log a message with error log level.
   *
   * @param message log this message
   */
  @Override
  public void error(Object message) {
    if (level <= ERROR_LEVEL) String2.log("[ERROR] " + message);
  }

  /**
   * Log an error with error log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void error(Object message, Throwable t) {
    if (level <= ERROR_LEVEL)
      String2.log("[ERROR] " + message + "\n" + MustBe.throwableToString(t));
  }

  /**
   * Log a message with fatal log level.
   *
   * @param message log this message
   */
  @Override
  public void fatal(Object message) {
    if (level <= FATAL_LEVEL) String2.log("[FATAL] " + message);
  }

  /**
   * Log an error with fatal log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  @Override
  public void fatal(Object message, Throwable t) {
    if (level <= FATAL_LEVEL)
      String2.log("[FATAL] " + message + "\n" + MustBe.throwableToString(t));
  }
}
