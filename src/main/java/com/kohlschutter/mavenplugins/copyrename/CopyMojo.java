/*
 * copy-rename-maven-plugin
 *
 * Copyright (c) 2014 Aneesh Joseph
 * Copyright 2023 Christian Kohlschütter
 *
 * SPDX-Identifier: MIT
 */
package com.kohlschutter.mavenplugins.copyrename;

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
 * @author <a href="aneesh@coderplus.com">Aneesh Joseph</a>
 * @since 1.0
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CopyMojo extends AbstractMojo {
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
   * Overwrite files.
   *
   * @since 1.0
   */
  @Parameter(property = "copy.overWrite", defaultValue = "true")
  boolean overWrite;

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
        if (ignoreFileNotFoundOnIncremental && buildContext.isIncremental()) {
          logWarn("sourceFile ", srcFile.getAbsolutePath(), " not found during incremental build");
        } else {
          logError("sourceFile ", srcFile.getAbsolutePath(), " does not exist");
        }
      } else if (srcFile.isDirectory()) {
        logError("sourceFile ", srcFile.getAbsolutePath(), " is not a file");
      } else if (destFile == null) {
        logError("destinationFile not specified");
      } else if (destFile.exists() && destFile.isFile() && !overWrite) {
        logError(destFile.getAbsolutePath(), " already exists and overWrite not set");
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
