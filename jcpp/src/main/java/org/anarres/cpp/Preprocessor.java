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

import org.anarres.cpp.PreprocessorListener.SourceChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.anarres.cpp.PreprocessorCommand.*;
import static org.anarres.cpp.Token.*;

/**
 * A C Preprocessor.
 * The Preprocessor outputs a token stream which does not need
 * re-lexing for C or C++. Alternatively, the output text may be
 * reconstructed by concatenating the {@link Token#getText() text}
 * values of the returned {@link Token Tokens}. (See
 * {@link CppReader}, which does this.)
 */
/*
 * Source file name and line number information is conveyed by lines of the form
 *
 * # linenum filename flags
 *
 * These are called linemarkers. They are inserted as needed into
 * the output (but never within a string or character constant). They
 * mean that the following line originated in file filename at line
 * linenum. filename will never contain any non-printing characters;
 * they are replaced with octal escape sequences.
 *
 * After the file name comes zero or more flags, which are `1', `2',
 * `3', or `4'. If there are multiple flags, spaces separate them. Here
 * is what the flags mean:
 *
 * `1'
 * This indicates the start of a new file.
 * `2'
 * This indicates returning to a file (after having included another
 * file).
 * `3'
 * This indicates that the following text comes from a system header
 * file, so certain warnings should be suppressed.
 * `4'
 * This indicates that the following text should be treated as being
 * wrapped in an implicit extern "C" block.
 */
public class Preprocessor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);

    private static final Source INTERNAL = new Source() {
        @Override
        public Token nextToken()
                throws IOException,
                LexerException {
            throw new LexerException("Cannot read from " + getName());
        }

        @Override
        public String getPath() {
            return "<internal-data>";
        }

        @Override
        public String getName() {
            return "internal data";
        }
    };
    private static final Macro __LINE__ = new Macro(INTERNAL, "__LINE__");
    private static final Macro __FILE__ = new Macro(INTERNAL, "__FILE__");
    private static final Macro __COUNTER__ = new Macro(INTERNAL, "__COUNTER__");

    private final List<Source> inputs;

    /* The fundamental engine. */
    private final Map<String, Macro> macros;
    private final Stack<State> states;
    private Source source;

    /* Miscellaneous support. */
    private int counter;
    private final Set<String> onceseenpaths = new HashSet<String>();
    private final List<VirtualFile> includes = new ArrayList<VirtualFile>();

    /* Support junk to make it work like cpp */
    private List<String> quoteincludepath;    /* -iquote */

    private List<String> sysincludepath;        /* -I */

    private List<String> frameworkspath;
    private final Set<Feature> features;
    private final Set<Warning> warnings;
    private VirtualFileSystem filesystem;
    private PreprocessorListener listener;
    private IPreprocessorControlListener controlListener;

    public Preprocessor() {
        this.inputs = new ArrayList<Source>();

        this.macros = new HashMap<String, Macro>();
        macros.put(__LINE__.getName(), __LINE__);
        macros.put(__FILE__.getName(), __FILE__);
        macros.put(__COUNTER__.getName(), __COUNTER__);
        this.states = new Stack<State>();
        states.push(new State());
        this.source = null;

        this.counter = 0;

        this.quoteincludepath = new ArrayList<String>();
        this.sysincludepath = new ArrayList<String>();
        this.frameworkspath = new ArrayList<String>();
        this.features = EnumSet.noneOf(Feature.class);
        this.warnings = EnumSet.noneOf(Warning.class);
        this.filesystem = new JavaFileSystem();
        this.listener = null;
        this.controlListener = null;
    }

    public Preprocessor(@Nonnull Source initial) {
        this();
        addInput(initial);
    }

    /**
     * Equivalent to
     * 'new Preprocessor(new {@link FileLexerSource}(file))'
     */
    public Preprocessor(@Nonnull File file)
            throws IOException {
        this(new FileLexerSource(file));
    }

    /**
     * Sets the VirtualFileSystem used by this Preprocessor.
     */
    public void setFileSystem(@Nonnull VirtualFileSystem filesystem) {
        this.filesystem = filesystem;
    }

    /**
     * Returns the VirtualFileSystem used by this Preprocessor.
     */
    @Nonnull
    public VirtualFileSystem getFileSystem() {
        return filesystem;
    }

    /**
     * Sets the PreprocessorListener which handles events for
     * this Preprocessor.
     * <p>
     * The listener is notified of warnings, errors and source
     * changes, amongst other things.
     */
    public void setListener(@Nonnull PreprocessorListener listener) {
        this.listener = listener;
        Source s = source;
        while (s != null) {
            // s.setListener(listener);
            s.init(this);
            s = s.getParent();
        }
    }

    /**
     * Sets the IPreprocessorControlListener which handles events for
     * this Preprocessor and allows to control the processing of directives.
     * <p>
     * The listener is notified when include, define, macro expansion, ifs,
     * and other things happen and asks if it should proceed with it.
     */
    public void setControlListener(@Nonnull IPreprocessorControlListener controlListener) {
        this.controlListener = controlListener;
    }

    /**
     * Returns the PreprocessorListener which handles events for
     * this Preprocessor.
     */
    @Nonnull
    public PreprocessorListener getListener() {
        return listener;
    }

    /**
     * Returns the feature-set for this Preprocessor.
     * <p>
     * This set may be freely modified by user code.
     */
    @Nonnull
    public Set<Feature> getFeatures() {
        return features;
    }

    /**
     * Adds a feature to the feature-set of this Preprocessor.
     */
    public void addFeature(@Nonnull Feature f) {
        features.add(f);
    }

    /**
     * Adds features to the feature-set of this Preprocessor.
     */
    public void addFeatures(@Nonnull Collection<Feature> f) {
        features.addAll(f);
    }

    /**
     * Adds features to the feature-set of this Preprocessor.
     */
    public void addFeatures(Feature... f) {
        addFeatures(Arrays.asList(f));
    }

    /**
     * Returns true if the given feature is in
     * the feature-set of this Preprocessor.
     */
    public boolean getFeature(@Nonnull Feature f) {
        return features.contains(f);
    }

    /**
     * Returns the warning-set for this Preprocessor.
     * <p>
     * This set may be freely modified by user code.
     */
    @Nonnull
    public Set<Warning> getWarnings() {
        return warnings;
    }

    /**
     * Adds a warning to the warning-set of this Preprocessor.
     */
    public void addWarning(@Nonnull Warning w) {
        warnings.add(w);
    }

    /**
     * Adds warnings to the warning-set of this Preprocessor.
     */
    public void addWarnings(@Nonnull Collection<Warning> w) {
        warnings.addAll(w);
    }

    /**
     * Returns true if the given warning is in
     * the warning-set of this Preprocessor.
     */
    public boolean getWarning(@Nonnull Warning w) {
        return warnings.contains(w);
    }

    /**
     * Adds input for the Preprocessor.
     * <p>
     * Inputs are processed in the order in which they are added.
     */
    public void addInput(@Nonnull Source source) {
        source.init(this);
        inputs.add(source);
    }

    /**
     * Adds input for the Preprocessor.
     *
     * @see #addInput(Source)
     */
    public void addInput(@Nonnull File file)
            throws IOException {
        addInput(new FileLexerSource(file));
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     */
    protected void error(int line, int column, @Nonnull String msg)
            throws LexerException {
        if (listener != null)
            listener.handleError(source, line, column, msg);
        else
            throw new LexerException("Error at " + line + ":" + column + ": " + msg);
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     *
     * @see #error(int, int, String)
     */
    protected void error(@Nonnull Token tok, @Nonnull String msg)
            throws LexerException {
        error(tok.getLine(), tok.getColumn(), msg);
    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     */
    protected void warning(int line, int column, @Nonnull String msg)
            throws LexerException {
        if (warnings.contains(Warning.ERROR))
            error(line, column, msg);
        else if (listener != null)
            listener.handleWarning(source, line, column, msg);
        else
            throw new LexerException("Warning at " + line + ":" + column + ": " + msg);
    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     *
     * @see #warning(int, int, String)
     */
    protected void warning(@Nonnull Token tok, @Nonnull String msg)
            throws LexerException {
        warning(tok.getLine(), tok.getColumn(), msg);
    }

    /**
     * Adds a Macro to this Preprocessor.
     * <p>
     * The given {@link Macro} object encapsulates both the name
     * and the expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@Nonnull Macro m) throws LexerException {
        // System.out.println("Macro " + m);
        String name = m.getName();
        /* Already handled as a source error in macro(). */
        if ("defined".equals(name))
            throw new LexerException("Cannot redefine name 'defined'");
        macros.put(m.getName(), m);
    }

    /**
     * Defines the given name as a macro.
     * <p>
     * The String value is lexed into a token stream, which is
     * used as the macro expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@Nonnull String name, @Nonnull String value)
            throws LexerException {
        try {
            Macro m = new Macro(name);
            StringLexerSource s = new StringLexerSource(value);
            for (; ; ) {
                Token tok = s.token();
                if (tok.getType() == EOF)
                    break;
                m.addToken(tok);
            }
            addMacro(m);
        } catch (IOException e) {
            throw new LexerException(e);
        }
    }

    /**
     * Defines the given name as a macro, with the value <code>1</code>.
     * <p>
     * This is a convnience method, and is equivalent to
     * <code>addMacro(name, "1")</code>.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@Nonnull String name)
            throws LexerException {
        addMacro(name, "1");
    }

    public boolean isDefined(@Nonnull String name){
        return macros.containsKey(name);
    }

    /**
     * Sets the user include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setQuoteIncludePath(@Nonnull List<String> path) {
        this.quoteincludepath = path;
    }

    /**
     * Returns the user include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @Nonnull
    public List<String> getQuoteIncludePath() {
        return quoteincludepath;
    }

    /**
     * Sets the system include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setSystemIncludePath(@Nonnull List<String> path) {
        this.sysincludepath = path;
    }

    /**
     * Returns the system include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @Nonnull
    public List<String> getSystemIncludePath() {
        return sysincludepath;
    }

    /**
     * Sets the Objective-C frameworks path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setFrameworksPath(@Nonnull List<String> path) {
        this.frameworkspath = path;
    }

    /**
     * Returns the Objective-C frameworks path used by this
     * Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @Nonnull
    public List<String> getFrameworksPath() {
        return frameworkspath;
    }

    /**
     * Returns the Map of Macros parsed during the run of this
     * Preprocessor.
     *
     * @return The {@link Map} of macros currently defined.
     */
    @Nonnull
    public Map<String, Macro> getMacros() {
        return macros;
    }

    /**
     * Returns the named macro.
     * <p>
     * While you can modify the returned object, unexpected things
     * might happen if you do.
     *
     * @return the Macro object, or null if not found.
     */
    @CheckForNull
    public Macro getMacro(@Nonnull String name) {
        return macros.get(name);
    }

    /**
     * Returns the list of {@link VirtualFile VirtualFiles} which have been
     * included by this Preprocessor.
     * <p>
     * This does not include any {@link Source} provided to the constructor
     * or {@link #addInput(java.io.File)} or {@link #addInput(Source)}.
     */
    @Nonnull
    public List<? extends VirtualFile> getIncludes() {
        return includes;
    }

    public Stack<State> getStates(){
        return states;
    }

    /* States */
    private void push_state(List<Token> tokens) {
        State top = states.peek();
        states.push(new State(top, tokens));
    }

    private void pop_state()
            throws LexerException {
        State s = states.pop();
        if (states.isEmpty()) {
            error(0, 0, "#" + "endif without #" + "if");
            states.push(s);
        }
    }

    private boolean isActive() {
        State state = states.peek();
        return state.isParentActive() && state.isActive();
    }


    /* Sources */

    /**
     * Returns the top Source on the input stack.
     *
     * @return the top Source on the input stack.
     * @see Source
     * @see #push_source(Source, boolean)
     * @see #pop_source()
     */
    // @CheckForNull
    protected Source getSource() {
        return source;
    }

    /**
     * Pushes a Source onto the input stack.
     *
     * @param source  the new Source to push onto the top of the input stack.
     * @param autopop if true, the Source is automatically removed from the input stack at EOF.
     * @see #getSource()
     * @see #pop_source()
     */
    protected void push_source(@Nonnull Source source, boolean autopop) {
        source.init(this);
        source.setParent(this.source, autopop);
        // source.setListener(listener);
        if (listener != null)
            listener.handleSourceChange(this.source, SourceChangeEvent.SUSPEND);
        this.source = source;
        if (listener != null)
            listener.handleSourceChange(this.source, SourceChangeEvent.PUSH);
    }

    /**
     * Pops a Source from the input stack.
     *
     * @param linemarker TODO: currently ignored, might be a bug?
     * @throws IOException if an I/O error occurs.
     * @see #getSource()
     * @see #push_source(Source, boolean)
     */
    @CheckForNull
    protected Token pop_source(boolean linemarker)
            throws IOException {
        if (listener != null)
            listener.handleSourceChange(this.source, SourceChangeEvent.POP);
        Source s = this.source;
        this.source = s.getParent();
        /* Always a noop unless called externally. */
        s.close();
        if (listener != null && this.source != null)
            listener.handleSourceChange(this.source, SourceChangeEvent.RESUME);

        Source t = getSource();
        if (getFeature(Feature.LINEMARKERS)
                && s.isNumbered()
                && t != null) {
            /* We actually want 'did the nested source
             * contain a newline token', which isNumbered()
             * approximates. This is not perfect, but works. */
            return line_token(t.getLine(), t.getName(), " 2");
        }

        return null;
    }

    protected void pop_source()
            throws IOException {
        pop_source(false);
    }

    @Nonnull
    private Token next_source() {
        if (inputs.isEmpty())
            return new Token(EOF);
        Source s = inputs.remove(0);
        push_source(s, true);
        return line_token(s.getLine(), s.getName(), " 1");
    }

    /* Source tokens */
    private Token source_token;

    /* XXX Make this include the NL, and make all cpp directives eat
     * their own NL. */
    @Nonnull
    private Token line_token(int line, @CheckForNull String name, @Nonnull String extra) {
        StringBuilder buf = new StringBuilder();
        buf.append("#line ").append(line)
                .append(" \"");
        /* XXX This call to escape(name) is correct but ugly. */
        if (name == null)
            buf.append("<no file>");
        else
            MacroTokenSource.escape(buf, name);
        buf.append("\"").append(extra).append("\n");
        return new Token(P_LINE, line, 0, buf.toString(), null);
    }

    @Nonnull
    private Token source_token()
            throws IOException,
            LexerException {
        if (source_token != null) {
            Token tok = source_token;
            source_token = null;
            if (getFeature(Feature.DEBUG))
                LOG.debug("Returning unget token " + tok);
            return tok;
        }

        for (; ; ) {
            Source s = getSource();
            if (s == null) {
                Token t = next_source();
                if (t.getType() == P_LINE && !getFeature(Feature.LINEMARKERS))
                    continue;
                return t;
            }
            Token tok = s.token();
            /* XXX Refactor with skipline() */
            if (tok.getType() == EOF && s.isAutopop()) {
                // System.out.println("Autopop " + s);
                Token mark = pop_source(true);
                if (mark != null)
                    return mark;
                continue;
            }
            if (getFeature(Feature.DEBUG))
                LOG.debug("Returning fresh token " + tok);
            return tok;
        }
    }

    private void source_untoken(Token tok) {
        if (this.source_token != null)
            throw new IllegalStateException("Cannot return two tokens");
        this.source_token = tok;
    }

    private boolean isWhite(Token tok) {
        int type = tok.getType();
        return (type == WHITESPACE)
                || (type == CCOMMENT)
                || (type == CPPCOMMENT);
    }

    private Token source_token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = source_token();
        } while (isWhite(tok));
        return tok;
    }

    /**
     * Returns an NL or an EOF token.
     * <p>
     * The metadata on the token will be correct, which is better
     * than generating a new one.
     * <p>
     * This method can, as of recent patches, return a P_LINE token.
     */
    private Token source_skipline(boolean white)
            throws IOException,
            LexerException {
        // (new Exception("skipping line")).printStackTrace(System.out);
        Source s = getSource();
        Token tok = s.skipline(white);
        /* XXX Refactor with source_token() */
        if (tok.getType() == EOF && s.isAutopop()) {
            // System.out.println("Autopop " + s);
            Token mark = pop_source(true);
            if (mark != null)
                return mark;
        }
        return tok;
    }

    /* processes and expands a macro. */
    private boolean macro(Macro m, Token orig)
            throws IOException,
            LexerException {
        Token tok;
        List<Argument> args;

        // System.out.println("pp: expanding " + m);
        if (m.isFunctionLike()) {
            OPEN:
            for (; ; ) {
                tok = source_token();
                // System.out.println("pp: open: token is " + tok);
                switch (tok.getType()) {
                    case WHITESPACE:    /* XXX Really? */

                    case CCOMMENT:
                    case CPPCOMMENT:
                    case NL:
                        break;    /* continue */

                    case '(':
                        break OPEN;
                    default:
                        source_untoken(tok);
                        return false;
                }
            }

            // tok = expanded_token_nonwhite();
            tok = source_token_nonwhite();

            /* We either have, or we should have args.
             * This deals elegantly with the case that we have
             * one empty arg. */
            if (tok.getType() != ')' || m.numberOfArgs() > 0) {
                args = new ArrayList<Argument>();

                Argument arg = new Argument();
                int depth = 0;
                boolean space = false;

                ARGS:
                for (; ; ) {
                    // System.out.println("pp: arg: token is " + tok);
                    switch (tok.getType()) {
                        case EOF:
                            error(tok, "EOF in macro args");
                            return false;

                        case ',':
                            if (depth == 0) {
                                if (m.isVariadic()
                                        && /* We are building the last arg. */ args.size() == m.numberOfArgs() - 1) {
                                    /* Just add the comma. */
                                    arg.addToken(tok);
                                } else {
                                    args.add(arg);
                                    arg = new Argument();
                                }
                            } else {
                                arg.addToken(tok);
                            }
                            space = false;
                            break;
                        case ')':
                            if (depth == 0) {
                                args.add(arg);
                                break ARGS;
                            } else {
                                depth--;
                                arg.addToken(tok);
                            }
                            space = false;
                            break;
                        case '(':
                            depth++;
                            arg.addToken(tok);
                            space = false;
                            break;

                        case WHITESPACE:
                        case CCOMMENT:
                        case CPPCOMMENT:
                        case NL:
                            /* Avoid duplicating spaces. */
                            space = true;
                            break;

                        default:
                            /* Do not put space on the beginning of
                             * an argument token. */
                            if (space && !arg.isEmpty())
                                arg.addToken(Token.space);
                            arg.addToken(tok);
                            space = false;
                            break;

                    }
                    // tok = expanded_token();
                    tok = source_token();
                }
                /* space may still be true here, thus trailing space
                 * is stripped from arguments. */

                if (args.size() != m.numberOfArgs()) {
                    if (m.isVariadic()) {
                        if (args.size() == m.numberOfArgs() - 1) {
                            args.add(new Argument());
                        } else {
                            error(tok,
                                    "variadic macro " + m.getName()
                                            + " has at least " + (m.numberOfArgs() - 1) + " parameters "
                                            + "but given " + args.size() + " args");
                            return false;
                        }
                    } else {
                        error(tok,
                                "macro " + m.getName()
                                        + " has " + m.numberOfArgs() + " parameters "
                                        + "but given " + args.size() + " args");
                        /* We could replay the arg tokens, but I
                         * note that GNU cpp does exactly what we do,
                         * i.e. output the macro name and chew the args.
                         */
                        return false;
                    }
                }

                for (Argument a : args) {
                    a.expand(this);
                }

                // System.out.println("Macro " + m + " args " + args);
            } else {
                /* nargs == 0 and we (correctly) got () */
                args = null;
            }

        } else {
            /* Macro without args. */
            args = null;
        }

        if (m == __LINE__) {
            push_source(new FixedTokenSource(
                    new Token(NUMBER,
                            orig.getLine(), orig.getColumn(),
                            Integer.toString(orig.getLine()),
                            new NumericValue(10, Integer.toString(orig.getLine())))
            ), true);
        } else if (m == __FILE__) {
            StringBuilder buf = new StringBuilder("\"");
            String name = getSource().getName();
            if (name == null)
                name = "<no file>";
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                switch (c) {
                    case '\\':
                        buf.append("\\\\");
                        break;
                    case '"':
                        buf.append("\\\"");
                        break;
                    default:
                        buf.append(c);
                        break;
                }
            }
            buf.append("\"");
            String text = buf.toString();
            push_source(new FixedTokenSource(
                    new Token(STRING,
                            orig.getLine(), orig.getColumn(),
                            text, text)
            ), true);
        } else if (m == __COUNTER__) {
            /* This could equivalently have been done by adding
             * a special Macro subclass which overrides getTokens(). */
            int value = this.counter++;
            push_source(new FixedTokenSource(
                    new Token(NUMBER,
                            orig.getLine(), orig.getColumn(),
                            Integer.toString(value),
                            new NumericValue(10, Integer.toString(value)))
            ), true);
        } else {
            push_source(new MacroTokenSource(m, args), true);
        }

        return true;
    }

    public List<Token> expand(@Nonnull String macroCall) throws IOException, LexerException {
        StringLexerSource source = new StringLexerSource(macroCall);
        List<Token> toks = new LinkedList<Token>();
        Token tok = source.token();
        while (tok != null && tok.getType() != Token.EOF) {
            toks.add(tok);
            tok = source.token();
        }
        return expand(toks);
    }

    public List<Token> expand(Macro m, @Nonnull String macroCall) throws IOException, LexerException {
        Macro before = getMacro(m.getName());
        addMacro(m);
        StringLexerSource source = new StringLexerSource(macroCall);
        List<Token> toks = new LinkedList<Token>();
        Token tok = source.token();
        while (tok != null && tok.getType() != Token.EOF) {
            toks.add(tok);
            tok = source.token();
        }
        List<Token> expanded = expand(toks);
        if(before != null){
            addMacro(before);
        } else {
            macros.remove(m.getName());
        }
        return expanded;
    }

    /**
     * Expands an argument.
     */
    /* I'd rather this were done lazily, but doing so breaks spec. */
    @Nonnull
    /* pp */ List<Token> expand(@Nonnull List<Token> arg)
            throws IOException,
            LexerException {
        List<Token> expansion = new ArrayList<Token>();
        boolean space = false;

        push_source(new FixedTokenSource(arg), false);

        EXPANSION:
        for (; ; ) {
            Token tok = expanded_token();
            switch (tok.getType()) {
                case EOF:
                    break EXPANSION;

                case WHITESPACE:
                case CCOMMENT:
                case CPPCOMMENT:
                    space = true;
                    break;

                default:
                    if (space && !expansion.isEmpty())
                        expansion.add(Token.space);
                    expansion.add(tok);
                    space = false;
                    break;
            }
        }

        // Always returns null.
        pop_source(false);

        return expansion;
    }

    /* processes a #define directive */
    private Token define()
            throws IOException,
            LexerException {
        Token tok = source_token_nonwhite();
        if (tok.getType() != IDENTIFIER) {
            error(tok, "Expected identifier");
            return source_skipline(false);
        }
        /* if predefined */

        String name = tok.getText();
        if ("defined".equals(name)) {
            error(tok, "Cannot redefine name 'defined'");
            return source_skipline(false);
        }

        Macro m = new Macro(getSource(), name);
        List<String> args;

        //parse macro arguments
        tok = source_token();
        if (tok.getType() == '(') {
            tok = source_token_nonwhite();
            if (tok.getType() != ')') {
                args = new ArrayList<String>();
                ARGS:
                for (; ; ) {
                    switch (tok.getType()) {
                        case IDENTIFIER:
                            args.add(tok.getText());
                            break;
                        case ELLIPSIS:
                            // Unnamed Variadic macro
                            args.add("__VA_ARGS__");
                            // We just named the ellipsis, but we unget the token
                            // to allow the ELLIPSIS handling below to process it.
                            source_untoken(tok);
                            break;
                        case NL:
                        case EOF:
                            error(tok,
                                    "Unterminated macro parameter list");
                            return tok;
                        default:
                            error(tok,
                                    "error in macro parameters: "
                                            + tok.getText());
                            return source_skipline(false);
                    }
                    tok = source_token_nonwhite();
                    switch (tok.getType()) {
                        case ',':
                            break;
                        case ELLIPSIS:
                            tok = source_token_nonwhite();
                            if (tok.getType() != ')')
                                error(tok,
                                        "ellipsis must be on last argument");
                            m.setVariadic(true);
                            break ARGS;
                        case ')':
                            break ARGS;

                        case NL:
                        case EOF:
                            /* Do not skip line. */
                            error(tok,
                                    "Unterminated macro parameters");
                            return tok;
                        default:
                            error(tok,
                                    "Bad token in macro parameters: "
                                            + tok.getText());
                            return source_skipline(false);
                    }
                    tok = source_token_nonwhite();
                }
            } else {
                assert tok.getType() == ')' : "Expected ')'";
                args = Collections.emptyList();
            }

            m.setArgs(args);
        } else {
            /* For searching. */
            args = Collections.emptyList();
            source_untoken(tok);
        }

        /* Get an expansion for the macro, using indexOf. */
        boolean space = false;
        boolean paste = false;
        int idx;

        /* Ensure no space at start. */
        tok = source_token_nonwhite();
        EXPANSION:
        for (; ; ) {
            switch (tok.getType()) {
                case EOF:
                    break EXPANSION;
                case NL:
                    break EXPANSION;

                case CCOMMENT:
                case CPPCOMMENT:
                    /* XXX This is where we implement GNU's cpp -CC. */
                    // break;
                case WHITESPACE:
                    if (!paste)
                        space = true;
                    break;

                /* Paste. */
                case PASTE:
                    space = false;
                    paste = true;
                    m.addPaste(new Token(M_PASTE,
                            tok.getLine(), tok.getColumn(),
                            "#" + "#", null));
                    break;

                /* Stringify. */
                case '#':
                    if (space)
                        m.addToken(Token.space);
                    space = false;
                    Token la = source_token_nonwhite();
                    if (la.getType() == IDENTIFIER
                            && ((idx = args.indexOf(la.getText())) != -1)) {
                        m.addToken(new Token(M_STRING,
                                la.getLine(), la.getColumn(),
                                "#" + la.getText(),
                                Integer.valueOf(idx)));
                    } else {
                        m.addToken(tok);
                        /* Allow for special processing. */
                        source_untoken(la);
                    }
                    break;

                case IDENTIFIER:
                    if (space)
                        m.addToken(Token.space);
                    space = false;
                    paste = false;
                    idx = args.indexOf(tok.getText());
                    if (idx == -1)
                        m.addToken(tok);
                    else
                        m.addToken(new Token(M_ARG,
                                tok.getLine(), tok.getColumn(),
                                tok.getText(),
                                Integer.valueOf(idx)));
                    break;

                default:
                    if (space)
                        m.addToken(Token.space);
                    space = false;
                    paste = false;
                    m.addToken(tok);
                    break;
            }
            tok = source_token();
        }

        if (getFeature(Feature.DEBUG))
            LOG.debug("Defined macro " + m);

        if (this.controlListener == null || this.controlListener.addMacro(m, this.source)) {
            if (this.listener != null) {
                this.listener.handleDefine(m, this.source);
            }
            addMacro(m);
        }
        return tok;    /* NL or EOF. */

    }

    @Nonnull
    private Token undef()
            throws IOException,
            LexerException {
        Token tok = source_token_nonwhite();
        if (tok.getType() != IDENTIFIER) {
            error(tok,
                    "Expected identifier, not " + tok.getText());
            if (tok.getType() == NL || tok.getType() == EOF)
                return tok;
        } else {
            Macro m = getMacro(tok.getText());
            if (m != null) {
                /* XXX error if predefined */
                if (this.controlListener == null || this.controlListener.removeMacro(m, this.source)) {
                    if (this.listener != null) {
                        this.listener.handleUndefine(m, this.source);
                    }
                    macros.remove(m.getName());
                }
            } else {
                m = new Macro(tok.getText());
                if (this.controlListener == null || this.controlListener.removeMacro(m, this.source)) {
                    if (this.listener != null) {
                        this.listener.handleUndefine(m, this.source);
                    }
                }
            }
        }
        return source_skipline(true);
    }

    /**
     * Attempts to include the given file.
     * <p>
     * User code may override this method to implement a virtual
     * file system.
     *
     * @param file The VirtualFile to attempt to include.
     * @return true if the file was successfully included, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    protected boolean include(@Nonnull VirtualFile file)
            throws IOException {
        // System.out.println("Try to include " + ((File)file).getAbsolutePath());
        if (!file.isFile())
            return false;

        if (getFeature(Feature.DEBUG))
            LOG.debug("pp: including " + file);
        includes.add(file);
        push_source(file.getSource(), true);
        return true;
    }

    /**
     * Attempts to include a file from an include path, by name.
     *
     * @param path The list of virtual directories to search for the given name.
     * @param name The name of the file to attempt to include.
     * @return true if the file was successfully included, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    protected boolean include(@Nonnull Iterable<String> path, @Nonnull String name)
            throws IOException {
        for (String dir : path) {
            VirtualFile file = getFileSystem().getFile(dir, name);
            if (include(file))
                return true;
        }
        return false;
    }

    /**
     * Handles an include directive.
     *
     * @throws IOException    if an I/O error occurs.
     * @throws LexerException if the include fails, and the error handler is fatal.
     */
    private void include(
            @CheckForNull String parent, int line,
            @Nonnull String name, boolean quoted, boolean next)
            throws IOException,
            LexerException {
        if (name.startsWith("/")) {
            VirtualFile file = filesystem.getFile(name);
            if (include(file))
                return;
            StringBuilder buf = new StringBuilder();
            buf.append("File not found: ").append(name);
            error(line, 0, buf.toString());
            return;
        }

        VirtualFile pdir = null;
        if (quoted) {
            if (parent != null) {
                VirtualFile pfile = filesystem.getFile(parent);
                pdir = pfile.getParentFile();
            }
            if (pdir != null) {
                VirtualFile ifile = pdir.getChildFile(name);
                if (include(ifile))
                    return;
            }
            if (include(quoteincludepath, name))
                return;
        } else {
            int idx = name.indexOf('/');
            if (idx != -1) {
                String frameworkName = name.substring(0, idx);
                String headerName = name.substring(idx + 1);
                String headerPath = frameworkName + ".framework/Headers/" + headerName;
                if (include(frameworkspath, headerPath))
                    return;
            }
        }
       // if(!sysincludepath.contains("C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin\\Marlin"))
        //    sysincludepath.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin\\Marlin");
        if (include(sysincludepath, name))
            return;

        StringBuilder buf = new StringBuilder();
        buf.append("File not found: ").append(name);
        buf.append(" in");
        if (quoted) {
            buf.append(" .").append('(').append(pdir).append(')');
            for (String dir : quoteincludepath)
                buf.append(" ").append(dir);
        }
        for (String dir : sysincludepath)
            buf.append(" ").append(dir);
        error(line, 0, buf.toString());
    }

    @Nonnull
    private Token include(boolean next)
            throws IOException,
            LexerException {
        LexerSource lexer = (LexerSource) source;
        try {
            lexer.setInclude(true);
            Token tok = token_nonwhite();

            String name;
            boolean quoted;

            //add listener for include
            String includeText = tok.getText();
            Source currSource = source;

            if (tok.getType() == STRING) {
                /* XXX Use the original text, not the value.
                 * Backslashes must not be treated as escapes here. */
                StringBuilder buf = new StringBuilder((String) tok.getValue());
                HEADER:
                for (; ; ) {
                    tok = token_nonwhite();
                    switch (tok.getType()) {
                        case STRING:
                            buf.append((String) tok.getValue());
                            break;
                        case NL:
                        case EOF:
                            break HEADER;
                        default:
                            warning(tok,
                                    "Unexpected token on #" + "include line");
                            return source_skipline(false);
                    }
                }
                name = buf.toString();
                quoted = true;
            } else if (tok.getType() == HEADER) {
                name = (String) tok.getValue();
                quoted = false;
                tok = source_skipline(true);
            } else {
                error(tok,
                        "Expected string or header, not " + tok.getText());
                switch (tok.getType()) {
                    case NL:
                    case EOF:
                        return tok;
                    default:
                        /* Only if not a NL or EOF already. */
                        return source_skipline(false);
                }
            }

            /* Do the inclusion. */
            if (this.controlListener == null || this.controlListener.include(source, tok.getLine(), name, quoted, next)) {
                include(source.getPath(), tok.getLine(), name, quoted, next);

                if (this.listener != null) {
                    //fire event
                    this.listener.handleInclude(includeText, next, currSource, source);
                }
            }

            /* 'tok' is the 'nl' after the include. We use it after the
             * #line directive. */
            if (getFeature(Feature.LINEMARKERS))
                return line_token(1, source.getName(), " 1");
            return tok;
        } finally {
            lexer.setInclude(false);
        }
    }

    protected void pragma_once(@Nonnull Token name)
            throws IOException, LexerException {
        Source s = this.source;
        if (!onceseenpaths.add(s.getPath())) {
            Token mark = pop_source(true);
            // FixedTokenSource should never generate a linemarker on exit.
            if (mark != null)
                push_source(new FixedTokenSource(Arrays.asList(mark)), true);
        }
    }

    protected void pragma(@Nonnull Token name, @Nonnull List<Token> value)
            throws IOException,
            LexerException {
        if (getFeature(Feature.PRAGMA_ONCE)) {
            if ("once".equals(name.getText())) {
                pragma_once(name);
                return;
            }
        }
        warning(name, "Unknown #" + "pragma: " + name.getText());
    }

    @Nonnull
    private Token pragma()
            throws IOException,
            LexerException {
        Token name;

        NAME:
        for (; ; ) {
            Token tok = source_token();
            switch (tok.getType()) {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    warning(tok,
                            "End of file in #" + "pragma");
                    return tok;
                case NL:
                    /* This may contain one or more newlines. */
                    warning(tok,
                            "Empty #" + "pragma");
                    return tok;
                case CCOMMENT:
                case CPPCOMMENT:
                case WHITESPACE:
                    continue NAME;
                case IDENTIFIER:
                    name = tok;
                    break NAME;
                default:
                    warning(tok,
                            "Illegal #" + "pragma " + tok.getText());
                    return source_skipline(false);
            }
        }

        Token tok;
        List<Token> value = new ArrayList<Token>();
        VALUE:
        for (; ; ) {
            tok = source_token();
            switch (tok.getType()) {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    warning(tok,
                            "End of file in #" + "pragma");
                    break VALUE;
                case NL:
                    /* This may contain one or more newlines. */
                    break VALUE;
                case CCOMMENT:
                case CPPCOMMENT:
                    break;
                case WHITESPACE:
                    value.add(tok);
                    break;
                default:
                    value.add(tok);
                    break;
            }
        }

        pragma(name, value);

        return tok;    /* The NL. */

    }

    /* For #error and #warning. */
    private void error(@Nonnull Token pptok, boolean is_error)
            throws IOException,
            LexerException {
        StringBuilder buf = new StringBuilder();
        buf.append('#').append(pptok.getText()).append(' ');
        /* Peculiar construction to ditch first whitespace. */
        Token tok = source_token_nonwhite();
        ERROR:
        for (; ; ) {
            switch (tok.getType()) {
                case NL:
                case EOF:
                    break ERROR;
                default:
                    buf.append(tok.getText());
                    break;
            }
            tok = source_token();
        }
        if (is_error)
            error(pptok, buf.toString());
        else
            warning(pptok, buf.toString());
    }

    /* This bypasses token() for #elif expressions.
     * If we don't do this, then isActive() == false
     * causes token() to simply chew the entire input line. */
    @Nonnull
    private Token expanded_token()
            throws IOException,
            LexerException {
        for (; ; ) {
            Token tok = source_token();
            // System.out.println("Source token is " + tok);
            if (tok.getType() == IDENTIFIER) {
                Macro m = getMacro(tok.getText());
                if (m == null)
                    return tok;
                if (source.isExpanding(m))
                    return tok;
                if ((this.controlListener == null || this.controlListener.expandMacro(m, source, tok.getLine(), tok.getColumn(), true)) && macro(m, tok))
                    continue;
            }
            return tok;
        }
    }

    @Nonnull
    private Token expanded_token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = expanded_token();
            // System.out.println("expanded token is " + tok);
        } while (isWhite(tok));
        return tok;
    }

    @CheckForNull
    private Token expr_token = null;

    @Nonnull
    private Token expr_token()
            throws IOException,
            LexerException {
        Token tok = expr_token;

        if (tok != null) {
            // System.out.println("ungetting");
            expr_token = null;
        } else {
            tok = expanded_token_nonwhite();
            // System.out.println("expt is " + tok);

            if (tok.getType() == IDENTIFIER
                    && tok.getText().equals("defined")) {
                Token la = source_token_nonwhite();
                boolean paren = false;
                if (la.getType() == '(') {
                    paren = true;
                    la = source_token_nonwhite();
                }

                // System.out.println("Core token is " + la);
                if (la.getType() != IDENTIFIER) {
                    error(la,
                            "defined() needs identifier, not "
                                    + la.getText());
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "0", new NumericValue(10, "0"));
                } else if (macros.containsKey(la.getText())) {
                    // System.out.println("Found macro");
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "1", new NumericValue(10, "1"));
                } else {
                    // System.out.println("Not found macro");
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "0", new NumericValue(10, "0"));
                }

                if (paren) {
                    la = source_token_nonwhite();
                    if (la.getType() != ')') {
                        expr_untoken(la);
                        error(la, "Missing ) in defined(). Got " + la.getText());
                    }
                }
            }
        }

        // System.out.println("expr_token returns " + tok);
        return tok;
    }

    private void expr_untoken(@Nonnull Token tok)
            throws LexerException {
        if (expr_token != null)
            throw new InternalException(
                    "Cannot unget two expression tokens."
            );
        expr_token = tok;
    }

    private int expr_priority(@Nonnull Token op) {
        switch (op.getType()) {
            case '/':
                return 11;
            case '%':
                return 11;
            case '*':
                return 11;
            case '+':
                return 10;
            case '-':
                return 10;
            case LSH:
                return 9;
            case RSH:
                return 9;
            case '<':
                return 8;
            case '>':
                return 8;
            case LE:
                return 8;
            case GE:
                return 8;
            case EQ:
                return 7;
            case NE:
                return 7;
            case '&':
                return 6;
            case '^':
                return 5;
            case '|':
                return 4;
            case LAND:
                return 3;
            case LOR:
                return 2;
            case '?':
                return 1;
            default:
                // System.out.println("Unrecognised operator " + op);
                return 0;
        }
    }

    private int expr_char(Token token) {
        Object value = token.getValue();
        if (value instanceof Character)
            return ((Character) value).charValue();
        String text = String.valueOf(value);
        if (text.length() == 0)
            return 0;
        return text.charAt(0);
    }

    public long expr(String expr) throws IOException, LexerException {
        push_source(new StringLexerSource(expr), false);
        Token tok = expr_token;
        expr_token = null;
        long result = expr(0);
        expr_token = tok;
        pop_source();
        return result;
    }

    private long expr(int priority)
            throws IOException,
            LexerException {
        /*
         * (new Exception("expr(" + priority + ") called")).printStackTrace();
         */

        Token tok = expr_token();
        long lhs, rhs;

        // System.out.println("Expr lhs token is " + tok);
        switch (tok.getType()) {
            case '(':
                lhs = expr(0);
                tok = expr_token();
                if (tok.getType() != ')') {
                    expr_untoken(tok);
                    error(tok, "Missing ) in expression. Got " + tok.getText());
                    return 0;
                }
                break;

            case '~':
                lhs = ~expr(11);
                break;
            case '!':
                lhs = expr(11) == 0 ? 1 : 0;
                break;
            case '-':
                lhs = -expr(11);
                break;
            case NUMBER:
                NumericValue value = (NumericValue) tok.getValue();
                lhs = value.longValue();
                break;
            case CHARACTER:
                lhs = expr_char(tok);
                break;
            case IDENTIFIER:
                if (warnings.contains(Warning.UNDEF))
                    warning(tok, "Undefined token '" + tok.getText()
                            + "' encountered in conditional.");
                lhs = 0;
                break;

            default:
                expr_untoken(tok);
                //error(tok,"Bad token in expression: " + tok.getText());
                return 0;
        }

        EXPR:
        for (; ; ) {
            // System.out.println("expr: lhs is " + lhs + ", pri = " + priority);
            Token op = expr_token();
            int pri = expr_priority(op);    /* 0 if not a binop. */

            if (pri == 0 || priority >= pri) {
                expr_untoken(op);
                break EXPR;
            }
            rhs = expr(pri);
            // System.out.println("rhs token is " + rhs);
            switch (op.getType()) {
                case '/':
                    if (rhs == 0) {
                        error(op, "Division by zero");
                        lhs = 0;
                    } else {
                        lhs = lhs / rhs;
                    }
                    break;
                case '%':
                    if (rhs == 0) {
                        error(op, "Modulus by zero");
                        lhs = 0;
                    } else {
                        lhs = lhs % rhs;
                    }
                    break;
                case '*':
                    lhs = lhs * rhs;
                    break;
                case '+':
                    lhs = lhs + rhs;
                    break;
                case '-':
                    lhs = lhs - rhs;
                    break;
                case '<':
                    lhs = lhs < rhs ? 1 : 0;
                    break;
                case '>':
                    lhs = lhs > rhs ? 1 : 0;
                    break;
                case '&':
                    lhs = lhs & rhs;
                    break;
                case '^':
                    lhs = lhs ^ rhs;
                    break;
                case '|':
                    lhs = lhs | rhs;
                    break;

                case LSH:
                    lhs = lhs << rhs;
                    break;
                case RSH:
                    lhs = lhs >> rhs;
                    break;
                case LE:
                    lhs = lhs <= rhs ? 1 : 0;
                    break;
                case GE:
                    lhs = lhs >= rhs ? 1 : 0;
                    break;
                case EQ:
                    lhs = lhs == rhs ? 1 : 0;
                    break;
                case NE:
                    lhs = lhs != rhs ? 1 : 0;
                    break;
                case LAND:
                    lhs = (lhs != 0) && (rhs != 0) ? 1 : 0;
                    break;
                case LOR:
                    lhs = (lhs != 0) || (rhs != 0) ? 1 : 0;
                    break;

                case '?': {
                    tok = expr_token();
                    if (tok.getType() != ':') {
                        expr_untoken(tok);
                        error(tok, "Missing : in conditional expression. Got " + tok.getText());
                        return 0;
                    }
                    long falseResult = expr(0);
                    lhs = (lhs != 0) ? rhs : falseResult;
                }
                break;

                default:
                    error(op,
                            "Unexpected operator " + op.getText());
                    return 0;

            }
        }

        /*
         * (new Exception("expr returning " + lhs)).printStackTrace();
         */
        // System.out.println("expr returning " + lhs);
        return lhs;
    }

    @Nonnull
    private Token toWhitespace(@Nonnull Token tok) {
        String text = tok.getText();
        int len = text.length();
        boolean cr = false;
        int nls = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\r':
                    cr = true;
                    nls++;
                    break;
                case '\n':
                    if (cr) {
                        cr = false;
                        break;
                    }
                    /* fallthrough */
                case '\u2028':
                case '\u2029':
                case '\u000B':
                case '\u000C':
                case '\u0085':
                    cr = false;
                    nls++;
                    break;
            }
        }

        char[] cbuf = new char[nls];
        Arrays.fill(cbuf, '\n');
        return new Token(WHITESPACE,
                tok.getLine(), tok.getColumn(),
                new String(cbuf));
    }

    @Nonnull
    private Token _token()
            throws IOException,
            LexerException {

        for (; ; ) {
            Token tok;
            if (!isActive()) {
                Source s = getSource();
                if (s == null) {
                    Token t = next_source();
                    if (t.getType() == P_LINE && !getFeature(Feature.LINEMARKERS))
                        continue;
                    return t;
                }

                try {
                    /* XXX Tell lexer to ignore warnings. */
                    s.setActive(false);
                    tok = source_token();
                } finally {
                    /* XXX Tell lexer to stop ignoring warnings. */
                    s.setActive(true);
                }
                switch (tok.getType()) {
                    case HASH:
                    case NL:
                    case EOF:
                        /* The preprocessor has to take action here. */
                        break;
                    case WHITESPACE:
                        return tok;
                    case CCOMMENT:
                    case CPPCOMMENT:
                        // Patch up to preserve whitespace.
                        if (getFeature(Feature.KEEPALLCOMMENTS))
                            return tok;
                        if (!isActive())
                            return toWhitespace(tok);
                        if (getFeature(Feature.KEEPCOMMENTS))
                            return tok;
                        return toWhitespace(tok);
                    default:
                        // Return NL to preserve whitespace.
                        /* XXX This might lose a comment. */
                        return source_skipline(false);
                }
            } else {
                tok = source_token();

                if (source instanceof UnprocessedFixedTokenSource) {
                    return tok;
                }
            }

            LEX:
            switch (tok.getType()) {
                case EOF:
                    /* Pop the stacks. */
                    return tok;

                case WHITESPACE:
                case NL:
                    return tok;

                case CCOMMENT:
                case CPPCOMMENT:
                    return tok;

                case '!':
                case '%':
                case '&':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case '-':
                case '/':
                case ':':
                case ';':
                case '<':
                case '=':
                case '>':
                case '?':
                case '[':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                case '~':
                case '.':

                    /* From Olivier Chafik for Objective C? */
                case '@':
                    /* The one remaining ASCII, might as well. */
                case '`':

                    // case '#':
                case AND_EQ:
                case ARROW:
                case CHARACTER:
                case DEC:
                case DIV_EQ:
                case ELLIPSIS:
                case EQ:
                case GE:
                case HEADER:    /* Should only arise from include() */

                case INC:
                case LAND:
                case LE:
                case LOR:
                case LSH:
                case LSH_EQ:
                case SUB_EQ:
                case MOD_EQ:
                case MULT_EQ:
                case NE:
                case OR_EQ:
                case PLUS_EQ:
                case RANGE:
                case RSH:
                case RSH_EQ:
                case STRING:
                case SQSTRING:
                case XOR_EQ:
                    return tok;

                case NUMBER:
                    return tok;

                case IDENTIFIER:
                    Macro m = getMacro(tok.getText());
                    if (m == null)
                        return tok;
                    if (source.isExpanding(m))
                        return tok;
                    if ((this.controlListener == null || this.controlListener.expandMacro(m, source, tok.getLine(), tok.getColumn(), false)) && macro(m, tok))
                        break;
                    return tok;

                case P_LINE:
                    if (getFeature(Feature.LINEMARKERS))
                        return tok;
                    break;

                case INVALID:
                    if (getFeature(Feature.CSYNTAX))
                        error(tok, String.valueOf(tok.getValue()));
                    return tok;
                default:
                    if(tok.getType() == PASTE)
                        return tok;
                    else if(tok.getText().equals("#"))
                        return tok;
                    else if (tok.getText().equals("\\"))
                        return tok;
                    else
                        throw new InternalException("Bad token " + tok);
                    // break;

                case HASH:
                    Token hash = tok;
                    tok = source_token_nonwhite();
                    // (new Exception("here")).printStackTrace();
                    switch (tok.getType()) {
                        case NL:
                            break LEX;    /* Some code has #\n */

                        case IDENTIFIER:
                            break;
                        default:
                            error(tok,
                                    "Preprocessor directive not a word "
                                            + tok.getText());
                            return source_skipline(false);
                    }
                    PreprocessorCommand ppcmd = PreprocessorCommand.forText(tok.getText());
                    if (ppcmd == null) {
                        error(tok,
                                "Unknown preprocessor directive "
                                        + tok.getText());
                        return source_skipline(false);
                    }


                    List<Token> ppTokens = new LinkedList<Token>();
                    Token ppTok = tok;
                    if (ppcmd == PP_IF || ppcmd == PP_IFDEF || ppcmd == PP_IFNDEF || ppcmd == PP_ELIF || ppcmd == PP_ELSE || ppcmd == PP_ENDIF) {
                        while (ppTok.getType() != NL && ppTok.getType() != EOF) { //&& ppTok.getType() != EOF
                            if (!ppTok.getText().equals("\r")) {
                                ppTokens.add(ppTok);
                            }
                            ppTok = this.source.peek();
                        }
                    }

                    PP:
                    switch (ppcmd) {

                        case PP_DEFINE:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return define();
                            // break;

                        case PP_UNDEF:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return undef();
                            // break;

                        case PP_INCLUDE:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return include(false);
                            // break;
                        case PP_INCLUDE_NEXT:
                            if (!isActive())
                                return source_skipline(false);
                            if (!getFeature(Feature.INCLUDENEXT)) {
                                error(tok,
                                        "Directive include_next not enabled"
                                );
                                return source_skipline(false);
                            }
                            return include(true);
                        // break;

                        case PP_WARNING:
                        case PP_ERROR:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                error(tok, ppcmd == PP_ERROR);
                            break;

                        case PP_IF:
                            push_state(ppTokens);
                            if (!isActive()) {
                                return source_skipline(false);
                            }
                            expr_token = null;

                        {
                            boolean process = this.controlListener == null || this.controlListener.processIf(ppTokens, this.source, IPreprocessorControlListener.IfType.IF);
                            states.peek().setProcessed(process);

                            if (!process) {
                                states.peek().setActive(true);
                                getSource().removePeeked();

                                //partially process condition
                                List<Token> condition = ppTokens.subList(1, ppTokens.size());
                                String partiallyProcessed = this.controlListener.getPartiallyProcessedCondition(condition, getSource(), IPreprocessorControlListener.IfType.IF, this);

                                if (partiallyProcessed == null) {
                                    ppTokens.add(ppTok);
                                    push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                } else {
                                    List<Token> partiallyProcessedTokens = new LinkedList<Token>();
                                    partiallyProcessedTokens.add(ppTokens.get(0));
                                    partiallyProcessedTokens.add(ppTokens.get(1));
                                    StringLexerSource lex = new StringLexerSource(partiallyProcessed);
                                    Token t = lex.token();
                                    while (t.getType() != EOF) {
                                        partiallyProcessedTokens.add(t);
                                        t = lex.token();
                                    }
                                    if (partiallyProcessedTokens.get(partiallyProcessedTokens.size() - 1).getType() != NL) {
                                        partiallyProcessedTokens.add(ppTok);
                                    }
                                    states.peek().removeLastTokens();
                                    states.peek().setTokens(partiallyProcessedTokens);
                                    push_source(new UnprocessedFixedTokenSource(partiallyProcessedTokens), true);
                                }

                                return hash;

                            } else {
                                states.peek().setActive(expr(0) != 0);
                            }

                            tok = expr_token();    /* unget */

                            if (tok.getType() == NL)
                                return tok;
                            return source_skipline(true);
                            // break;
                        }
                        case PP_ELIF:
                            State state = states.peek();
                            getSource().removePeeked();

                            state.setTokens(ppTokens);
                            if (false) {
                                /* Check for 'if' */
                                ;
                            } else if (state.sawElse()) {
                                error(tok, "#elif after #" + "else");
                                return source_skipline(false);
                            } else if (!state.isParentActive()) {
                                /* Nested in skipped 'if' */
                                return source_skipline(false);
                            } else if (state.isProcessed() && state.isActive()) {
                                /* The 'if' part got executed. */
                                state.setParentActive(false);
                                /* This is like # else # if but with
                                 * only one # end. */
                                state.setActive(false);
                                return source_skipline(false);
                            } else {
                                boolean process = state.isProcessed();
                                if (!process) {
                                    //partially process condition
                                    List<Token> condition = ppTokens.subList(1, ppTokens.size());
                                    String partiallyProcessed = this.controlListener.getPartiallyProcessedCondition(condition, getSource(), IPreprocessorControlListener.IfType.ELSIF, this);

                                    if (partiallyProcessed == null) {
                                        ppTokens.add(ppTok);
                                        push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                    } else {
                                        List<Token> partiallyProcessedTokens = new LinkedList<Token>();
                                        partiallyProcessedTokens.add(ppTokens.get(0));
                                        partiallyProcessedTokens.add(ppTokens.get(1));
                                        StringLexerSource lex = new StringLexerSource(partiallyProcessed);
                                        Token t = lex.token();
                                        while (t.getType() != EOF) {
                                            partiallyProcessedTokens.add(t);
                                            t = lex.token();
                                        }
                                        if (partiallyProcessedTokens.get(partiallyProcessedTokens.size() - 1).getType() != NL) {
                                            partiallyProcessedTokens.add(ppTok);
                                        }
                                        states.peek().removeLastTokens();
                                        states.peek().setTokens(partiallyProcessedTokens);
                                        push_source(new UnprocessedFixedTokenSource(partiallyProcessedTokens), true);
                                    }

                                    return hash;
                                }

                                expr_token = null;
                                state.setActive(expr(0) != 0);
                                tok = expr_token();    /* unget */

                                if (tok.getType() == NL)
                                    return tok;
                                return source_skipline(true);
                            }
                            // break;

                        case PP_ELSE:
                            state = states.peek();
                            if (false)
                                /* Check for 'if' */ ;
                            else if (state.sawElse()) {
                                error(tok,
                                        "#" + "else after #" + "else");
                                return source_skipline(false);
                            } else {
                                state.setSawElse();
                                boolean process = state.isProcessed();
                                state.setActive(!process || !state.isActive());
                                if (!process) {
                                    push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                    return hash;
                                }
                                return source_skipline(warnings.contains(Warning.ENDIF_LABELS));
                            }
                            // break;

                        case PP_IFDEF:
                            push_state(ppTokens);
                            if (!isActive()) {
                                return source_skipline(false);
                            } else {
                                tok = source_token_nonwhite();
                                // System.out.println("ifdef " + tok);
                                if (tok.getType() != IDENTIFIER) {
                                    error(tok, "Expected identifier, not " + tok.getText());
                                    return source_skipline(false);
                                } else {
                                    String text = tok.getText();
                                    boolean exists = macros.containsKey(text);
                                    boolean process = this.controlListener == null || this.controlListener.processIf(ppTokens, this.source, IPreprocessorControlListener.IfType.IFDEF);
                                    states.peek().setActive(!process || exists);
                                    states.peek().setProcessed(process);
                                    if (!process) {

                                        List<Token> condition = new LinkedList<Token>();
                                        condition.add(tok);
                                        String partiallyProcessed = this.controlListener.getPartiallyProcessedCondition(condition, getSource(), IPreprocessorControlListener.IfType.IFDEF, this);

                                        if(partiallyProcessed == null) {
                                            push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                        } else {
                                            List<Token> partiallyProcessedTokens = new LinkedList<Token>();
                                            StringLexerSource lex = new StringLexerSource(partiallyProcessed);
                                            Token t = lex.token();
                                            while (t.getType() != EOF) {
                                                partiallyProcessedTokens.add(t);
                                                t = lex.token();
                                            }
                                            if(partiallyProcessedTokens.get(0).getType() == HASH){
                                                partiallyProcessedTokens.remove(0);
                                            } else {
                                                partiallyProcessedTokens.add(0, ppTokens.get(0));
                                                partiallyProcessedTokens.add(1, ppTokens.get(1));
                                            }
                                            states.peek().removeLastTokens();
                                            states.peek().setTokens(partiallyProcessedTokens);
                                            push_source(new UnprocessedFixedTokenSource(partiallyProcessedTokens), true);
                                        }
                                        return hash;
                                    }

                                    return source_skipline(true);
                                }
                            }
                            // break;

                        case PP_IFNDEF:
                            push_state(ppTokens);
                            if (!isActive()) {
                                return source_skipline(false);
                            } else {
                                tok = source_token_nonwhite();
                                if (tok.getType() != IDENTIFIER) {
                                    error(tok,
                                            "Expected identifier, not "
                                                    + tok.getText());
                                    return source_skipline(false);
                                } else {
                                    String text = tok.getText();
                                    boolean exists = macros.containsKey(text);
                                    boolean process = this.controlListener == null || this.controlListener.processIf(ppTokens, this.source, IPreprocessorControlListener.IfType.IFNDEF);
                                    states.peek().setActive(!process || !exists);
                                    states.peek().setProcessed(process);
                                    if (!process) {

                                        List<Token> condition = new LinkedList<Token>();
                                        condition.add(tok);
                                        String partiallyProcessed = this.controlListener.getPartiallyProcessedCondition(condition, getSource(), IPreprocessorControlListener.IfType.IFNDEF, this);

                                        if(partiallyProcessed == null) {
                                            push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                        } else {
//                                            List<Token> partiallyProcessedTokens = new LinkedList<Token>();
//                                            partiallyProcessedTokens.add(ppTokens.get(0));
//                                            partiallyProcessedTokens.add(ppTokens.get(1));
//                                            StringLexerSource lex = new StringLexerSource(partiallyProcessed);
//                                            Token t = lex.token();
//                                            while (t.getType() != EOF) {
//                                                partiallyProcessedTokens.add(t);
//                                                t = lex.token();
//                                            }
//                                            states.peek().removeLastTokens();
//                                            states.peek().setTokens(partiallyProcessedTokens);
//                                            push_source(new UnprocessedFixedTokenSource(partiallyProcessedTokens), true);

                                            List<Token> partiallyProcessedTokens = new LinkedList<Token>();
                                            StringLexerSource lex = new StringLexerSource(partiallyProcessed);
                                            Token t = lex.token();
                                            while (t.getType() != EOF) {
                                                partiallyProcessedTokens.add(t);
                                                t = lex.token();
                                            }
                                            if(partiallyProcessedTokens.get(0).getType() == HASH){
                                                partiallyProcessedTokens.remove(0);
                                            } else {
                                                partiallyProcessedTokens.add(0, ppTokens.get(0));
                                                partiallyProcessedTokens.add(1, ppTokens.get(1));
                                            }
                                            states.peek().removeLastTokens();
                                            states.peek().setTokens(partiallyProcessedTokens);
                                            push_source(new UnprocessedFixedTokenSource(partiallyProcessedTokens), true);
                                        }
                                        return hash;
                                    }
                                    return source_skipline(true);
                                }
                            }
                            // break;

                        case PP_ENDIF:
                            state = states.peek();
                            pop_state();
                            if (!state.isProcessed()) {
                                push_source(new UnprocessedFixedTokenSource(ppTokens), true);
                                return hash;
                            }
                            return source_skipline(warnings.contains(Warning.ENDIF_LABELS));
                        // break;

                        case PP_LINE:
                            return source_skipline(false);
                        // break;

                        case PP_PRAGMA:
                            if (!isActive())
                                return source_skipline(false);
                            return pragma();
                        // break;

                        default:
                            /* Actual unknown directives are
                             * processed above. If we get here,
                             * we succeeded the map lookup but
                             * failed to handle it. Therefore,
                             * this is (unconditionally?) fatal. */
                            // if (isActive()) /* XXX Could be warning. */
                            throw new InternalException(
                                    "Internal error: Unknown directive "
                                            + tok);
                            // return source_skipline(false);
                    }

            }
        }
    }

    @Nonnull
    private Token token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = _token();
        } while (isWhite(tok));
        return tok;
    }

    /**
     * Returns the next preprocessor token.
     *
     * @return The next fully preprocessed token.
     * @throws IOException       if an I/O error occurs.
     * @throws LexerException    if a preprocessing error occurs.
     * @throws InternalException if an unexpected error condition arises.
     * @see Token
     */
    @Nonnull
    public Token token()
            throws IOException,
            LexerException {
        Token tok = _token();
        if (getFeature(Feature.DEBUG))
            LOG.debug("pp: Returning " + tok);
        return tok;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        Source s = getSource();
        while (s != null) {
            buf.append(" -> ").append(String.valueOf(s)).append("\n");
            s = s.getParent();
        }

        Map<String, Macro> macros = new TreeMap<String, Macro>(getMacros());
        for (Macro macro : macros.values()) {
            buf.append("#").append("macro ").append(macro).append("\n");
        }

        return buf.toString();
    }

    public void close()
            throws IOException {
        {
            Source s = source;
            while (s != null) {
                s.close();
                s = s.getParent();
            }
        }
        for (Source s : inputs) {
            s.close();
        }
    }

}
