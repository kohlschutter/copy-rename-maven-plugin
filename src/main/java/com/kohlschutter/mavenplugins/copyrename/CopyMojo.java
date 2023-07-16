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
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Copy files during build.
 *
 * @author Aneesh Joseph
 * @since 1.0
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CopyMojo extends AbstractMojo {
  /**
   * Skip execution.
   *
   * @since 2.0.0
   */
  @Parameter(property = "copy.skip", defaultValue = "false")
  boolean skip;

  /**
   * The file which has to be copied.
   *
   * @since 1.0
   */
  @Parameter(required = false)
  private File sourceFile;

  /**
   * The target file to which the file should be copied(this shouldn't be a directory but a file
   * which does or does not exist).
   *
   * @since 1.0
   */
  @Parameter(required = false)
  private File destinationFile;

  /**
   * Collection of FileSets to work on (FileSet contains sourceFile and destinationFile). See
   * <a href="./usage.html">Usage</a> for details.
   *
   * @since 1.0
   */
  @Parameter(required = false)
  private List<FileSet> fileSets;

  /**
   * Overwrite existing files.
   *
   * @since 1.0
   */
  @Parameter(property = "copy.overWrite", defaultValue = "true")
  boolean overWrite;

  /**
   * Don't throw an error when overWrite is false and target file already exists.
   *
   * @since 2.0.0
   */
  @Parameter(property = "copy.ignoreExisting", defaultValue = "false")
  boolean ignoreExisting;

  /**
   * Ignore copying this file if it does not exist.
   *
   * @since 2.0.0
   */
  @Parameter(property = "copy.ignoreMissing", defaultValue = "false")
  boolean ignoreMissing;

  /**
   * Ignore File Not Found errors during incremental build.
   *
   * @since 1.0
   */
  @Parameter(property = "copy.ignoreFileNotFoundOnIncremental", defaultValue = "true")
  boolean ignoreFileNotFoundOnIncremental;

  /**
   * Reference to the maven project.
   *
   * @since 1.0
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  /**
   * Creates a new instance of the "copy" mojo.
   */
  public CopyMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException {
    new CopyMojoExecutionContext(this).execute();
  }

  private final class CopyMojoExecutionContext extends MojoExecutionContext {
    protected CopyMojoExecutionContext(CopyMojo mojo) {
      super(mojo);
    }

    @Override
    public void execute() throws MojoExecutionException {
      if (skip) {
        logDebug("Skipping the copy-rename-maven-plugin");
        return;
      }

      logDebug("Executing the copy-rename-maven-plugin");

      if (fileSets != null && !fileSets.isEmpty()) {
        for (FileSet fileSet : fileSets) {
          File srcFile = fileSet.getSourceFile();
          File destFile = fileSet.getDestinationFile();
          if (srcFile != null) {
            copy(srcFile, destFile);
          }
        }
      } else if (sourceFile != null) {
        copy(sourceFile, destinationFile);
      } else {
        logInfo("No Files to process");
      }
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private void copy(File srcFile, File destFile) throws MojoExecutionException {
      if (!srcFile.exists()) {
        if (ignoreMissing) {
          logInfo("Skipping copy of ", srcFile.getAbsolutePath(), " (missing)");
        } else if (ignoreFileNotFoundOnIncremental && buildContext.isIncremental()) {
          logWarn("sourceFile ", srcFile.getAbsolutePath(), " not found during incremental build");
        } else {
          logError("sourceFile ", srcFile.getAbsolutePath(), " does not exist");
        }
      } else if (srcFile.isDirectory()) {
        logError("sourceFile ", srcFile.getAbsolutePath(), " is not a file");
      } else if (destFile == null) {
        logError("destinationFile not specified");
      } else if (destFile.exists() && destFile.isFile() && !overWrite) {
        if (ignoreExisting) {
          logInfo("Skipping ", destFile.getAbsolutePath(), " (already exists)");
        } else {
          logError(destFile.getAbsolutePath(), " already exists and overWrite not set");
        }
      } else {
        try {
          if (buildContext.isIncremental() && destFile.exists() && !buildContext.hasDelta(srcFile)
              && FileUtils.contentEquals(srcFile, destFile)) {
            logInfo("No changes detected in ", srcFile.getAbsolutePath());
            return;
          }
          FileUtils.copyFile(srcFile, destFile);
          logInfo("Copied ", srcFile.getAbsolutePath(), " to " + destFile.getAbsolutePath());
          buildContext.refresh(destFile);
        } catch (IOException e) {
          throw new MojoExecutionException("could not copy " + srcFile.getAbsolutePath() + " to "
              + destFile.getAbsolutePath(), e);
        }
      }
    }
  }
}
