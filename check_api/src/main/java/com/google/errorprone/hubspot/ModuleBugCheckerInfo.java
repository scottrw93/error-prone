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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.ErrorProneOptions;

public class ModuleBugCheckerInfo {
  private final Class<? extends ModuleBugChecker> checker;
  private final ModuleBugPattern pattern;

  public static ModuleBugCheckerInfo create(Class<? extends ModuleBugChecker> checker) {
    ModuleBugPattern pattern =
        Preconditions.checkNotNull(
            checker.getAnnotation(ModuleBugPattern.class),
            "ModuleBugCheckers must be annotated with @ModuleBugPattern");

    Preconditions.checkArgument(
        !(Modifier.isAbstract(checker.getModifiers())
            || Modifier.isInterface(checker.getModifiers())),
        "%s must be a concrete class",
        checker);

    ModuleBugCheckerPatternValidator.validate(pattern);
    return new ModuleBugCheckerInfo(checker, pattern);
  }

  private ModuleBugCheckerInfo(Class<? extends ModuleBugChecker> checker, ModuleBugPattern pattern) {
    this.checker = checker;
    this.pattern = pattern;
  }

  public Optional<ModuleBugChecker> instantiateChecker(ErrorProneOptions options) {
    try {
      return Optional.of(instantiateCheckerUnsafe(options));
    } catch (Throwable e) {
      // TODO - report errors here
      return Optional.empty();
    }
  }

  private ModuleBugChecker instantiateCheckerUnsafe(ErrorProneOptions options) {
    @SuppressWarnings("unchecked")
    Optional<Constructor<ModuleBugChecker>> flagsConstructor =
        Arrays.stream((Constructor<ModuleBugChecker>[]) checker.getConstructors())
            .filter(
                c -> Arrays.equals(c.getParameterTypes(), new Class<?>[] { ErrorProneFlags.class}))
            .findFirst();

    if (flagsConstructor.isPresent()) {
      try {
        return flagsConstructor.get().newInstance(options.getFlags());
      } catch (ReflectiveOperationException e) {
        throw new LinkageError("Could not instantiate BugChecker.", e);
      }
    }

    // If no flags constructor, invoke default constructor.
    try {
      return checker.getConstructor().newInstance();
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new LinkageError(
          String.format(
              "Could not instantiate ModuleBugChecker %s: Are both the class and the zero-arg"
                  + " constructor public?",
              checker),
          e);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError("Could not instantiate ModuleBugChecker.", e);
    }
  }
}
