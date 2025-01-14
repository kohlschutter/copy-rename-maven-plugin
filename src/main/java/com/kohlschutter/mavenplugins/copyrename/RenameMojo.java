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

/*
 * The MIT License
 *
 * Copyright (c) 2004
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

// CPD-OFF

/**
 * Rename files or directories during build.
 *
 * @author Aneesh Joseph
 * @since 1.0
 */
@Mojo(name = "rename", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class RenameMojo extends AbstractMojo {
  /**
   * Skip execution.
   *
   * @since 2.0.0
   */
  @Parameter(property = "copy.skip", defaultValue = "false")
  boolean skip;

  /**
   * The file/directory which has to be renamed.
   *
   * @since 1.0
   */
  @Parameter(required = false)
  private File sourceFile;
  /**
   * The target file/directory name.
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
   * Ignore renaming this file if it does not exist.
   *
   * @since 2.0.0
   */
  @Parameter(property = "copy.ignoreMissing", defaultValue = "false")
  boolean ignoreMissing;

  /**
   * Ignore errors if the source file/directory was not found during incremental build.
   *
   * @since 1.0
   */
  @Parameter(property = "copy.ignoreFileNotFoundOnIncremental", defaultValue = "true")
  boolean ignoreFileNotFoundOnIncremental;

  /**
   * Reference to the Maven project.
   *
   * @since 1.0
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  /**
   * Creates a new instance of the "rename" mojo.
   */
  public RenameMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException {
    new RenameMojoExecutionContext(this).execute();
  }

  private final class RenameMojoExecutionContext extends MojoExecutionContext {
    protected RenameMojoExecutionContext(RenameMojo mojo) {
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
            rename(srcFile, destFile);
          }
        }
      } else if (sourceFile != null) {
        rename(sourceFile, destinationFile);
      } else {
        logInfo("No Files to process");
      }
    }

    private void rename(File srcFile, File destFile) throws MojoExecutionException {
      if (!srcFile.exists()) {
        if (ignoreMissing) {
          logInfo("Skipping rename of ", srcFile.getAbsolutePath(), " (missing)");
        } else if (ignoreFileNotFoundOnIncremental && buildContext.isIncremental()) {
          logWarn("sourceFile ", srcFile.getAbsolutePath(), " not found during incremental build");
        } else {
          logError("sourceFile ", srcFile.getAbsolutePath(), " does not exist");
        }
      } else if (destFile == null) {
        logError("destinationFile not specified");
      } else if (destFile.exists() && (destFile.isFile() == srcFile.isFile()) && !overWrite) {
        if (ignoreExisting) {
          logInfo("Skipping ", destFile.getAbsolutePath(), " (already exists)");
        } else {
          logError(destFile.getAbsolutePath(), " already exists and overWrite not set");
        }
      } else {
        try {
          FileUtils.rename(srcFile, destFile);
          logInfo("Renamed ", srcFile.getAbsolutePath(), " to ", destFile.getAbsolutePath());
          buildContext.refresh(destFile);
        } catch (IOException e) {
          throw new MojoExecutionException("could not rename " + srcFile.getAbsolutePath() + " to "
              + destFile.getAbsolutePath(), e);
        }
      }
    }
  }
}
