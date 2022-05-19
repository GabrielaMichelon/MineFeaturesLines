package at.jku.isse.gitecco.core.git;

import java.util.ArrayList;
import java.util.List;

public class FileChange {
    private String lines;
    private String position;
    private List<String> fileLines;
    private List<String> previousContent;

    public FileChange(String lines, String position,List<String> fileLines, List<String> previousContent) {
        this.lines = lines;
        this.position = position;
        this.fileLines = fileLines;
        this.previousContent = previousContent;
    }

    public String getLines() {
        return this.lines;
    }

    public void setLines(String lines) {
        this.lines = lines;
    }

    public String getPosition() {
        return this.position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public List<String> getFileLines() {
        return this.fileLines;
    }

    public void setFileLines(List<String> fileLines) {
        this.fileLines = fileLines;
    }

    public List<String> getPreviousContent() {
        return this.previousContent;
    }

    public void setPreviousContent(List<String> previousContent) {
        this.previousContent = previousContent;
    }
}
