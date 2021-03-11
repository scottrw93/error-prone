/*
 * Copyright 2011 The Error Prone Authors.
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;

/**
 * Describes a bug pattern detected by error-prone. Used to generate compiler error messages, for
 * {@code @}SuppressWarnings, and to generate the documentation that we host on our web site.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@Retention(RUNTIME)
public @interface BugPattern {

  /** A collection of standardized tags that can be applied to BugPatterns. */
  final class StandardTags {
    private StandardTags() {}

    /**
     * This check, for reasons of backwards compatibility or difficulty in cleaning up, should be
     * considered very likely to represent a real error in the vast majority ({@code >99.9%}) of
     * cases, but couldn't otherwise be turned on as an ERROR.
     *
     * <p>Systems trying to determine the set of likely errors from a collection of BugPatterns
     * should act as if any BugPattern with {@link #severity()} of {@link SeverityLevel#ERROR} also
     * has this tag.
     */
    public static final String LIKELY_ERROR = "LikelyError";

    /**
     * This check detects a coding pattern that is valid within the Java language and doesn't
     * represent a runtime defect, but is otherwise discouraged for reasons of consistency within a
     * project or ease of understanding by other programmers.
     *
     * <p>Checks using this tag should limit their replacements to those that don't change the
     * behavior of the code (for example: adding clarifying parentheses, reordering modifiers in a
     * single declaration, removing implicit modifiers like {@code public} for members in an {@code
     * interface}).
     */
    public static final String STYLE = "Style";

    /**
     * This check detects a potential performance issue, where an easily-identifiable replacement
     * for the code being made will always result in a net positive performance improvement.
     */
    public static final String PERFORMANCE = "Performance";

    /**
     * This check detects code that may technically be working within a limited domain, but is
     * fragile, or violates generally-accepted assumptions of behavior.
     *
     * <p>Examples: DefaultCharset, where code implicitly uses the JVM default charset, will work in
     * circumstances where data being fed to the system happens to be compatible with the Charset,
     * but breaks down if fed data outside.
     */
    public static final String FRAGILE_CODE = "FragileCode";

    /**
     * This check points out potential issues when operating in a concurrent context
     *
     * <p>The code may work fine when accessed by 1 thread at a time, but may have some unintended
     * behavior when running in multiple threads.
     */
    public static final String CONCURRENCY = "Concurrency";

    /**
     * This check points out a coding pattern that, while functional, has an easier-to-read or
     * faster alternative.
     */
    public static final String SIMPLIFICATION = "Simplification";

    /** This check performs a refactoring, for example migrating to a new version of an API. */
    public static final String REFACTORING = "Refactoring";
  }

  /**
   * A unique identifier for this bug, used for @SuppressWarnings and in the compiler error message.
   */
  String name();

  /** Alternate identifiers for this bug, which may also be used in @SuppressWarnings. */
  String[] altNames() default {};

  /** The type of link to generate in the compiler error message. */
  LinkType linkType() default LinkType.AUTOGENERATED;

  /** The link URL to use if linkType() is LinkType.CUSTOM. */
  String link() default "";

  /** The type of link to generate in the compiler error message. */
  enum LinkType {
    /** Link to autogenerated documentation, hosted on the error-prone web site. */
    AUTOGENERATED,
    /** Custom string. */
    CUSTOM,
    /** No link should be displayed. */
    NONE
  }

  /**
   * A list of Stringly-typed tags to apply to this check. These tags can be consumed by tools
   * aggregating Error Prone checks (for example: a git pre-commit hook could clean up Java source
   * by finding any checks tagged "Style", run an Error Prone compile over the code with those
   * checks enabled, collect the fixes suggested and apply them).
   *
   * <p>To allow for sharing of tags across systems, a number of standard tags are available as
   * static constants in {@link StandardTags}. It is strongly encouraged to extract any custom tags
   * used in annotation property to constants that are shared by your codebase.
   */
  String[] tags() default {};

  /**
   * A short summary of the problem that this checker detects. Used for the default compiler error
   * message and for the short description in the generated docs. Should not end with a period, to
   * match javac warning/error style.
   *
   * <p>Markdown syntax is not allowed for this element.
   */
  String summary();

  /**
   * A longer explanation of the problem that this checker detects. Used as the main content in the
   * generated documentation for this checker.
   *
   * <p>Markdown syntax is allowed for this element.
   */
  String explanation() default "";

  SeverityLevel severity();

  /**
   * The severity of the diagnostic.
   */
  enum SeverityLevel {
    ERROR,
    WARNING,
    /** Note that this level generally disables the bug checker. */
    SUGGESTION,
    HIDDEN
  }

  /** True if the check can be disabled using command-line flags. */
  boolean disableable() default true;

  /**
   * A set of annotation types that can be used to suppress the check.
   *
   * <p>Includes only {@link SuppressWarnings} by default.
   *
   * <p>To make a check unsuppressible, set {@code suppressionAnnotations} to empty. Note that
   * unsuppressible checks may still be disabled using command line flags (see {@link
   * #disableable}).
   */
  Class<? extends Annotation>[] suppressionAnnotations() default SuppressWarnings.class;

  /** True if this check should be invoked on generated code */
  boolean inspectGeneratedCode() default false;

  /**
   * Generate an explanation of how to suppress the check.
   *
   * <p>This should only be disabled if the check has a non-standard suppression mechanism that
   * requires additional explanation. For example, {@link SuppressWarnings} cannot be applied to
   * packages, so checks that operate at the package level need special treatment.
   */
  boolean documentSuppression() default true;

  /** @deprecated this is a no-op that will be removed in the future */
  @Deprecated
  boolean generateExamplesFromTestCases() default true;
}
