/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.hubspot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.descriptionlistener.DescriptionListeners;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

public class ModuleBugCheckerAnalyzer {
  private static final String MODULE_CHECKS_FLAG = "hubspot:module-checks";

  private final Context context;
  private final List<ModuleBugCheckerInfo> moduleBugCheckers;
  private final Map<String, Severity> moduleBugCheckerSeverity;
  private final Set<String> keys;

  public ModuleBugCheckerAnalyzer(Context context, ErrorProneOptions options) {
    this.context = context;
    this.moduleBugCheckers = loadModuleChecks(context, options);
    this.moduleBugCheckerSeverity = getModuleBugCheckerSeverities(this.moduleBugCheckers, options);
    this.keys = new TreeSet<>();
  }

  public void addContext(TaskEvent event) {
    TreePath path = JavacTrees.instance(context).getPath(event.getTypeElement());
    if (path == null) {
      path = new TreePath(event.getCompilationUnit());
    }

    String name = path.getCompilationUnit().getSourceFile().getName();
    keys.add(name);
  }

  public void runChecks() {
    for (ModuleBugCheckerInfo info : moduleBugCheckers) {
      for (String key : keys) {
      }
    }
  }

  private static List<ModuleBugCheckerInfo> loadModuleChecks(Context context, ErrorProneOptions options) {
    JavaFileManager fileManager = context.get(JavaFileManager.class);

    ClassLoader loader;
    if (fileManager.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH)) {
      // If the annotation processor path is available, just use that.
      JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
      loader = processingEnvironment.getProcessorClassLoader();
    } else {
      loader = HubSpotErrorProneAnalyzer.class.getClassLoader();
    }

    Iterable<ModuleBugChecker> moduleBugCheckers = ServiceLoader.load(ModuleBugChecker.class, loader);
    if (Iterables.isEmpty(moduleBugCheckers)) {
      return Collections.emptyList();
    } else {
      ImmutableList.Builder<ModuleBugCheckerInfo> builder = ImmutableList.builder();
      Iterator<ModuleBugChecker> iter = moduleBugCheckers.iterator();
      while (iter.hasNext()) {
        try {
          Class<? extends ModuleBugChecker> checker = iter.next().getClass();
          builder.add(ModuleBugCheckerInfo.create(checker));
        } catch (Throwable e) {
          if (HubSpotUtils.isErrorHandlingEnabled(options)) {
            HubSpotUtils.recordCheckLoadError(e);
          } else {
            throw e;
          }
        }
      }

      return builder.build();
    }
  }

  private static Map<String, Severity> getModuleBugCheckerSeverities(
      List<ModuleBugCheckerInfo> moduleBugCheckers,
      ErrorProneOptions options
  ) {
    Map<String, Severity> severityOverrides = new HashMap<>();
    options.getFlags()
        .get(MODULE_CHECKS_FLAG)
        .map(s -> Arrays.asList(s.split(",")))
        .orElse(Collections.emptyList())
        .forEach(arg -> parseSeverity(arg, severityOverrides));

    if (severityOverrides.isEmpty()
        && options.getFlags().isEmpty()
        && !options.isEnableAllChecksAsWarnings()
        && !options.isDropErrorsToWarnings()
        && !options.isDisableAllChecks()) {
      return Collections.emptyMap();
    }

    return Collections.emptyMap();
  }

  private static void parseSeverity(String arg, Map<String, Severity> severityMap) {
    List<String> parts = Splitter.on(':').splitToList(arg);
    if (parts.size() > 2 || parts.get(0).isEmpty()) {
      throw new InvalidCommandLineOptionException("invalid flag: " + arg);
    }

    String checkName = parts.get(0);
    Severity severity;
    if (parts.size() == 1) {
      severity = Severity.DEFAULT;
    } else {
      try {
        severity = Severity.valueOf(parts.get(1));
      } catch (IllegalArgumentException e) {
        throw new InvalidCommandLineOptionException("invalid flag: " + arg);
      }
    }
    severityMap.put(checkName, severity);
  }
}
