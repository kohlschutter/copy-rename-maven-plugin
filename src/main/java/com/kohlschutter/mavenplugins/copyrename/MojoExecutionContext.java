/*
 * copy-rename-maven-plugin
 *
 * Copyright (c) 2014 Aneesh Joseph
 * Copyright 2023 Christian Kohlschütter
 *
 * SPDX-Identifier: MIT
 */
package com.kohlschutter.mavenplugins.copyrename;

import java.util.function.Supplier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Helper class to simplify logging and encapsulating the execution context.
 * 
 * @author Christian Kohlschütter
 */
abstract class MojoExecutionContext {
  private final Log log; // NOPMD.ProperLogger

  protected MojoExecutionContext(AbstractMojo mojo) {
    this.log = mojo.getLog();
  }

  public abstract void execute() throws MojoExecutionException;

  protected void logDebug(Object... msg) {
    if (log.isDebugEnabled()) {
      log.debug(concatenate(msg));
    }
  }

  protected void logInfo(Object... msg) {
    if (log.isInfoEnabled()) {
      log.info(concatenate(msg));
    }
  }

  protected void logWarn(Object... msg) {
    if (log.isWarnEnabled()) {
      log.warn(concatenate(msg));
    }
  }

  protected void logError(Object... msg) {
    if (log.isErrorEnabled()) {
      log.error(concatenate(msg));
    }
  }

  private static String concatenate(Object... parts) {
    switch (parts.length) {
      case 0:
        return "";
      case 1:
        Object p1 = parts[0];
        if (p1 instanceof Supplier<?>) {
          p1 = ((Supplier<?>) p1).get();
        }
        return String.valueOf(p1);
      default:
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
          if (p instanceof Supplier<?>) {
            p = ((Supplier<?>) p).get();
          }
          sb.append(p);
        }
        return sb.toString();
    }
  }
}
