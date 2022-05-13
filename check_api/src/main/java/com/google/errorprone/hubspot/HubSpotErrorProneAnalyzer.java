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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.ErrorProneAnalyzer;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;

public class HubSpotErrorProneAnalyzer implements TaskListener {
  private static final String MODULE_CHECKS_FLAG = "hubspot:module-checks";
  private final Context context;
  private final ErrorProneOptions options;
  private final ErrorProneAnalyzer delegate;
  private final ModuleBugCheckerAnalyzer analyzer;

  public static TaskListener wrap(Context context, ErrorProneOptions options, ErrorProneAnalyzer analyzer) {
    return new HubSpotErrorProneAnalyzer(context, options, analyzer);
  }

  private HubSpotErrorProneAnalyzer(Context context, ErrorProneOptions options, ErrorProneAnalyzer delegate) {
    this.context = context;
    this.options = options;
    this.delegate = delegate;
    this.analyzer = new ModuleBugCheckerAnalyzer(context, options, getCompilationUnitSupplier(delegate));
  }

  @Override
  public void started(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleStartup();
    }

    try {
      delegate.started(taskEvent);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(options)) {
        HubSpotUtils.recordUncaughtException(t);
      }
      throw t;
    }
  }

  @Override
  public void finished(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      analyzer.runChecks();
      HubSpotLifecycleManager.instance(context).handleShutdown();
    }

    try {
      delegate.finished(taskEvent);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(options)) {
        HubSpotUtils.recordUncaughtException(t);
      }
      throw t;
    }
  }

  private Supplier<Set<JCCompilationUnit>> getCompilationUnitSupplier(ErrorProneAnalyzer analyzer) {
    return () -> analyzer.getSeen().stream()
        .filter(t -> t instanceof JCCompilationUnit)
        .map(t -> (JCCompilationUnit) t)
        .filter(compilationUnit -> !shouldExcludeSourceFile(compilationUnit))
        .collect(ImmutableSet.toImmutableSet());
  }

  private boolean shouldExcludeSourceFile(CompilationUnitTree tree) {
    Pattern excludedPattern = options.getExcludedPattern();
    return excludedPattern != null && excludedPattern.matcher(ASTHelpers.getFileName(tree)).matches();
  }
}
