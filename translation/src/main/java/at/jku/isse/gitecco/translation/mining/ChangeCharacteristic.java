package at.jku.isse.gitecco.translation.mining;

import java.util.ArrayList;

public class ChangeCharacteristic {

    private Integer linesOfCodeAdded;
    private Integer linesOfCodeRemoved;
    private ArrayList<String> scatteringDegreeFiles;
    private Integer scatteringDegreeIfs;
    private ArrayList<Integer> tanglingDegreeIFs;

    public ChangeCharacteristic() {
        this.linesOfCodeAdded = 0;
        this.linesOfCodeRemoved = 0;
        this.scatteringDegreeFiles = new ArrayList<>();
        this.scatteringDegreeIfs = 0;
        this.tanglingDegreeIFs = new ArrayList<>();
    }

    public Integer getLinesOfCodeAdded() {
        return linesOfCodeAdded;
    }

    public void setLinesOfCodeAdded(Integer linesOfCodeAdded) {
        this.linesOfCodeAdded = linesOfCodeAdded;
    }

    public Integer getLinesOfCodeRemoved() {
        return linesOfCodeRemoved;
    }

    public void setLinesOfCodeRemoved(Integer linesOfCodeRemoved) {
        this.linesOfCodeRemoved = linesOfCodeRemoved;
    }

    public ArrayList<String> getScatteringDegreeFiles() {
        return scatteringDegreeFiles;
    }

    public void addScatteringDegreeFiles(String scatteringDegreeFiles) {
        this.scatteringDegreeFiles.add(scatteringDegreeFiles);
    }

    public Integer getScatteringDegreeIfs() {
        return this.scatteringDegreeIfs;
    }

    public void setScatteringDegreeIfs(Integer scatteringDegreeIfs) {
        this.scatteringDegreeIfs = scatteringDegreeIfs;
    }

    public ArrayList<Integer> getTanglingDegree() {
        return tanglingDegreeIFs;
    }

    public void addTanglingDegree(Integer tanglingDegreeIFs)
    {
        this.tanglingDegreeIFs.add(tanglingDegreeIFs);
    }

}
