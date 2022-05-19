/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.anarres.cpp;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A handler for preprocessor events, primarily errors and warnings.
 *
 * If no PreprocessorListener is installed in a Preprocessor, all
 * error and warning events will throw an exception. Installing a
 * listener allows more intelligent handling of these events.
 */
public interface IPreprocessorControlListener {

    /**
     * Called before a macro is expanded, to check if it should be expanded
     *
     * @param m Macro to be expanded
     * @param source current Source for Tokens in the Preprocessor
     * @param line in text
     * @param column in text
     * @param isInIf ture if macros is part of #if condition, false if part of C code
     * @return true if macro should be expanded, false if not
     */
    boolean expandMacro(Macro m, Source source, int line, int column, boolean isInIf);

    /**
     * Called before a macro is defined, to check if it should be defined
     *
     * @param m Macro to be defined
     * @param source current Source for Tokens in the Preprocessor
     * @return true if macro should be defined, false if not
     */
    boolean addMacro(Macro m, Source source);

    /**
     * Called before a macro is undefined, to check if it should be undefined
     *
     * @param m Macro to be undefined
     * @param source current Source for Tokens in the Preprocessor
     * @return true if macro should be undefined, false if not
     */
    boolean removeMacro(Macro m, Source source);

    /**
     * Called before an include directive is processed, to check if it should be processed
     *
     * @param source current Source for Tokens in the Preprocessor
     * @param line in text
     * @param name of the file to include
     * @param quoted true if file is in quotes (""), false otherwise (<>)
     * @param next true if include next directive, false otherwise
     * @return true if include should be processed, false if not
     */
    boolean include(@Nonnull Source source, int line, @Nonnull String name, boolean quoted, boolean next);

    enum IfType{
        IF, IFDEF, IFNDEF, ELSIF
    }

    /**
     * Called before any #if, #ifdef, etc. directive is processed, to check if it should be processed
     *
     * @param condition List of Tokens in the condition
     * @param source current Source for Tokens in the Preprocessor
     * @param type IfType of the directive
     * @return true if directive should be processed, false if not
     */
    boolean processIf(List<Token> condition, Source source, IfType type);

    /**
     * This allows for users to exchange #if conditions during processing.
     * Note: use pp.expand(List<Token>) or pp.expand(String) to expand macro calls
     * Note: use pp.expr(String) to evaluate an expression
     *
     * @param condition List of Tokens in the condition
     * @param source current Source for Tokens in the Preprocessor
     * @param type IfType of the directive
     * @param pp Preprocessor that called the method, to enable partial processing
     * @return String of the exchanged condition
     */
    String getPartiallyProcessedCondition(List<Token> condition, Source source, IfType type, Preprocessor pp);
}
