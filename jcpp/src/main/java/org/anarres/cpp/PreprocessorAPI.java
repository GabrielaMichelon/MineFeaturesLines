package org.anarres.cpp;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PreprocessorAPI {

    private Preprocessor pp;

    private PreprocessorControlListener controlListener;

    private boolean inlineIncludes = true;

    private boolean keepIncludes = false;

    private boolean keepDefines = false;

    private List<String> fileTypes = new LinkedList<String>();

    private File currentFile = null;

    private File fileCurrentlyProcessed = null;

    private PrintStream out = null;

    public PreprocessorAPI() {
        this(null);
    }

    public PreprocessorAPI(PreprocessorControlListener controlListener) {
        this.pp = new Preprocessor();
        this.controlListener = controlListener;
        initDefault();
    }

    private void initDefault() {
        pp.addFeature(Feature.DIGRAPHS);
        pp.addFeature(Feature.TRIGRAPHS);
        pp.addFeature(Feature.LINEMARKERS);
        pp.addFeature(Feature.INCLUDENEXT);
        pp.addWarning(Warning.IMPORT);
        // pp.addMacro("__JCPP__");
        pp.getSystemIncludePath().add("/usr/local/include");
        pp.getSystemIncludePath().add("/usr/include");
        pp.getFrameworksPath().add("/System/Library/Frameworks");
        pp.getFrameworksPath().add("/Library/Frameworks");
        pp.getFrameworksPath().add("/Local/Library/Frameworks");

        fileTypes.add("c");
        fileTypes.add("cpp");
        fileTypes.add("h");
        fileTypes.add("hpp");

        pp.setListener(new PreprocessorListener() {
            public void handleWarning(@Nonnull Source source, int line, int column, @Nonnull String msg) throws LexerException {
                //System.out.println(source.getName() + ":" + line + ":" + column + ": warning: " + msg);
            }

            public void handleError(@Nonnull Source source, int line, int column, @Nonnull String msg) throws LexerException {
                // System.out.println(source.getName() + ":" + line + ":" + column + ": error: " + msg);
            }

            public void handleSourceChange(@Nonnull Source source, @Nonnull SourceChangeEvent event) {
                if (source instanceof FileLexerSource) {
                    currentFile = ((FileLexerSource) source).getFile();
                }
            }

            public void handleInclude(@Nonnull String text, boolean next, Source source, Source toInclude) {
                if (keepIncludes) {
                    if (source instanceof FileLexerSource) {
                        if (!inlineIncludes && ((FileLexerSource) source).getFile().equals(fileCurrentlyProcessed)) {
                            //out.println("#include " + text);
                            if(next){
                                out.println("#include_next " + text);
                            } else {
                                out.println("#include " + text);
                            }
                        }
                    }
                }
            }

            public void handleDefine(Macro m, Source source) {
                if (keepDefines) {
                    if (source instanceof FileLexerSource) {
                        if (((FileLexerSource) source).getFile().equals(fileCurrentlyProcessed)) {
                            out.print("#define " + m.getName());
                            if(m.isFunctionLike()){
                                out.print("(");
                                boolean first = true;
                                for(String arg : m.getArgs()){
                                    if(!first){
                                        out.print(", ");
                                    }
                                    first = false;
                                    out.print(arg);
                                }
                                out.print(")");
                            }
                            out.print(" ");
                            for (Token tok : m.getTokens()) {
                                out.print(tok.getText());
                            }
                        }
                    }
                }
            }

            public void handleUndefine(Macro m, Source source) {
                if (keepDefines) {
                    if (source instanceof FileLexerSource) {
                        if (((FileLexerSource) source).getFile().equals(fileCurrentlyProcessed)) {
                            out.print("#undef " + m.getName());
                        }
                    }
                }
            }
        });

        if(controlListener != null){
            this.pp.setControlListener(this.controlListener);
        }
    }

    public void debug() {
        pp.addFeature(Feature.DEBUG);
    }

    public void addSystemIncludePath(String path) {
        pp.getSystemIncludePath().add(path);
    }

    /**
     * @param ext - file extension without "."
     */
    public void addFileExtensionToHandle(String ext) {
        this.fileTypes.add(ext);
    }

    /**
     * @param ext - file extension without "."
     */
    public void removeFileExtensionToHandle(String ext) {
        this.fileTypes.remove(ext);
    }

    /**
     * Set if you want to process the content of the header files into your output
     *
     * @param inlineIncludes - include the headers from #include in the output
     */
    public void setInlineIncludes(boolean inlineIncludes) {
        this.inlineIncludes = inlineIncludes;
    }

    /**
     * Set if you want to keep include directives in output. Note: They still will be processed to get macros from them.
     *
     * @param keepIncludes - keep includes unprocessed in output
     */
    public void setKeepIncludes(boolean keepIncludes) {
        this.keepIncludes = keepIncludes;
    }

    /**
     * Set if you want to keep define and undef directives in output. Note: They still will be processed to expand macros if configured to.
     *
     * @param keepDefines - keep defines unprocessed in output
     */
    public void setKeepDefines(boolean keepDefines) {
        this.keepDefines = keepDefines;
    }

    public void addMacro(String name) {
        try {
            pp.addMacro(name);
        } catch (LexerException e) {
            e.printStackTrace();
        }
    }

    public void addMacro(String name, String value) {
        try {
            pp.addMacro(name, value);
        } catch (LexerException e) {
            e.printStackTrace();
        }
    }

    public void removeMacro(String name) {
        pp.getMacros().remove(name);
    }

    /**
     * process files
     *
     * @param src       - source file or directory
     * @param targetDir - target directory for output
     */
    public void preprocess(File src, File targetDir, List<String> dirFiles, String commitInformation) {
        //add directories that we need to include manually to get all the files to create a clean version because the
        // pp.getSystemIncludePath().add("/usr/local/include"); pp.getSystemIncludePath().add("/usr/include"); does not includes
        //files outside the root path
        if(dirFiles != null && !dirFiles.isEmpty()){
            for (String dir: dirFiles) {
                pp.getSystemIncludePath().add(dir);
            }
        }

        if (this.inlineIncludes && this.keepIncludes) {
            throw new IllegalStateException("includeHeaders and keepIncludes should not be set at the same time");
        }

        List<File> files = new LinkedList<File>();
        Set<File> binaryFiles = new HashSet<>();
        getFilesToProcess(src, files);
        getFilesToCopy(src, binaryFiles);
        File target = new File(targetDir, commitInformation);
        target.getParentFile().mkdir();
        File file = new File(targetDir, commitInformation);
        targetDir= file;

        for (File f : binaryFiles) {
            try {
                String sourcePath = f.getCanonicalPath();
                String relativePath = sourcePath.substring(src.getCanonicalPath().length());

                if (relativePath.length() == 0) {
                    target = new File(targetDir, f.getName());
                } else {
                    target = new File(targetDir, relativePath);
                }

                target.getParentFile().mkdirs();

                if(target.exists()){
                    target.delete();
                }

                Files.copy(f.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Error copying non source files to target dir");
                e.printStackTrace();
            }
        }

        for (File f : files) {

            this.fileCurrentlyProcessed = f;
            if(this.controlListener != null){
                this.controlListener.setFileCurrentlyProcessed(f);
            }

            try {
                pp.addInput(f);

                String sourcePath = f.getCanonicalPath();
                String relativePath = sourcePath.substring(src.getCanonicalPath().length());

                if (relativePath.length() == 0) {
                    target = new File(targetDir, f.getName());
                } else {
                    target = new File(targetDir, relativePath);
                }

                target.getParentFile().mkdirs();

                out = new PrintStream(target);

                try {
                    for (; ; ) {
                        Token tok = pp.token();
                        if (tok == null)
                            break;
                        if (tok.getType() == Token.EOF)
                            break;
                        if (tok.getType() != Token.P_LINE) {
                            if (this.inlineIncludes || f.equals(this.currentFile)) {
                                out.print(tok.getText());
                            }
                        }
                    }
                } catch (Exception e) {
                    StringBuilder buf = new StringBuilder("Preprocessor failed:\n");
                    Source s = pp.getSource();
                    while (s != null) {
                        buf.append(" -> ").append(s).append("\n");
                        s = s.getParent();
                    }
                    System.err.println(buf.toString());
                    e.printStackTrace();
                }

                out.flush();
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void getFilesToProcess(File f, List<File> files) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                getFilesToProcess(file, files);
            }
        } else if (f.isFile()) {
            for (String ext : this.fileTypes) {
                if (f.getName().endsWith("." + ext)) {
                    files.add(f);
                }
            }
        }
    }

    private void getFilesToCopy(File f, Set<File> files) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                //application specific condition: f.getName().endsWith(".git") is for ignoring the .git folder
                if(!file.getName().endsWith(".git")) getFilesToCopy(file, files);
            }
        } else if (f.isFile()) {
            boolean add = true;
            for (String ext : this.fileTypes) {
                if(f.getName().endsWith("." + ext)) add = false;
            }
            if(add) files.add(f);
        }
    }
}
