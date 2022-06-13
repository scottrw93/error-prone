/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "The first argument to nCopies is the number of copies, and the second is the item to copy",
    severity = ERROR)
public class NCopiesOfChar extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("java.util.Collections").named("nCopies");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    Symtab syms = state.getSymtab();
    Types types = state.getTypes();
    if (types.isSameType(types.unboxedTypeOrType(getType(arguments.get(1))), syms.intType)
        && types.isSameType(types.unboxedTypeOrType(getType(arguments.get(0))), syms.charType)) {
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .replace(arguments.get(0), state.getSourceForNode(arguments.get(1)))
              .replace(arguments.get(1), state.getSourceForNode(arguments.get(0)))
              .build());
    }
    return NO_MATCH;
  }
}
