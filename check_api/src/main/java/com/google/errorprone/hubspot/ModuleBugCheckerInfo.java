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

import java.lang.reflect.Modifier;

import com.google.common.base.Preconditions;

public class ModuleBugCheckerInfo {

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

  }
}
