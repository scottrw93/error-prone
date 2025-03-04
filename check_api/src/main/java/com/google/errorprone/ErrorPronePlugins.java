/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import java.util.ServiceLoader;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

import com.google.common.collect.Iterables;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.hubspot.HubSpotUtils;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

/** Loads custom Error Prone checks from the annotation processor classpath. */
public final class ErrorPronePlugins {

  public static ScannerSupplier loadPlugins(ScannerSupplier scannerSupplier, ErrorProneOptions options, Context context) {
    JavaFileManager fileManager = context.get(JavaFileManager.class);

    ClassLoader loader;

    if (fileManager.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH)) {
      // If the annotation processor path is available, just use that.
      JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
      loader = processingEnvironment.getProcessorClassLoader();
    } else {
      loader = ErrorPronePlugins.class.getClassLoader();
    }

    // The upstream version that creates a list throws an exception
    // by too-eagerly resolving the bug checkers
    Iterable<BugChecker> extraBugCheckers = ServiceLoader.load(BugChecker.class, loader);
    if (Iterables.isEmpty(extraBugCheckers)) {
      return scannerSupplier;
    } else if (HubSpotUtils.isErrorHandlingEnabled(options)) {
       return scannerSupplier.plus(HubSpotUtils.createScannerSupplier(extraBugCheckers));
    } else {
       return scannerSupplier.plus(
           ScannerSupplier.fromBugCheckerClasses(
               Iterables.transform(extraBugCheckers, BugChecker::getClass)));
    }
  }

  private ErrorPronePlugins() {}
}
