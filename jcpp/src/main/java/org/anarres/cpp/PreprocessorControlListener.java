package org.anarres.cpp;

import java.io.File;

public abstract class PreprocessorControlListener implements IPreprocessorControlListener {

    private File fileCurrentlyProcessed = null;

    public File getFileCurrentlyProcessed() {
        return fileCurrentlyProcessed;
    }

    protected void setFileCurrentlyProcessed(File fileCurrentlyProcessed) {
        this.fileCurrentlyProcessed = fileCurrentlyProcessed;
    }
}
