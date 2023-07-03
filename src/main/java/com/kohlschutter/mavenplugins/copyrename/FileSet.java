/*
 * copy-rename-maven-plugin
 *
 * Copyright (c) 2014 Aneesh Joseph
 * Copyright 2023 Christian Kohlschütter
 *
 * SPDX-Identifier: MIT
 */
package com.kohlschutter.mavenplugins.copyrename;

import java.io.File;

/**
 * Specifies source and destination file.
 */
public class FileSet {

  private File sourceFile;
  private File destinationFile;

  /**
   * Creates a new {@link FileSet} instance.
   */
  public FileSet() {
    super();
  }

  /**
   * Gets the source file.
   *
   * @return The source file.
   */
  public File getSourceFile() {
    return sourceFile;
  }

  /**
   * Sets the source file.
   *
   * @param sourceFile The source file.
   */
  public void setSourceFile(File sourceFile) {
    this.sourceFile = sourceFile;
  }

  /**
   * Gets the destination file.
   *
   * @return The destination file.
   */
  public File getDestinationFile() {
    return destinationFile;
  }

  /**
   * Sets the destination file.
   *
   * @param destinationFile The destination file.
   */
  public void setDestinationFile(File destinationFile) {
    this.destinationFile = destinationFile;
  }
}
