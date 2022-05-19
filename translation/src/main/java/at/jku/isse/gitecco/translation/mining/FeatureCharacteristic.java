package at.jku.isse.gitecco.translation.mining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FeatureCharacteristic {
    private Integer linesOfCode;
    private Integer scatteringDegreeIFs;
    private Integer scatteringDegreeNIFs;
    private ArrayList<String> scatteringDegreeFiles;
    //number of features with the analyzed #IFDEF of a feature revision and number of lines inside its block
    private Map<Integer,Integer> tanglingDegreeIFs;
    private Integer nestingDegree;
    private Integer numberOfTopLevelBranches;
    private Integer numberOfNonTopLevelBranches;

    public FeatureCharacteristic() {
        this.linesOfCode = 0;
        this.scatteringDegreeIFs = 0;
        this.scatteringDegreeNIFs = 0;
        this.scatteringDegreeFiles = new ArrayList<>();
        this.tanglingDegreeIFs = new HashMap<>();
        this.nestingDegree = 0;
        this.numberOfTopLevelBranches = 0;
        this.numberOfNonTopLevelBranches = 0;
    }

    public Integer getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(Integer linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public Integer getScatteringDegreeIFs() {
        return scatteringDegreeIFs;
    }

    public void setScatteringDegreeIFs(Integer scatteringDegreeIFs) {
        this.scatteringDegreeIFs = scatteringDegreeIFs;
    }

    public ArrayList<String> getScatteringDegreeFiles() {
        return scatteringDegreeFiles;
    }

    public void addScatteringDegreeFiles(String scatteringDegreeFiles) {
        this.scatteringDegreeFiles.add(scatteringDegreeFiles);
    }

    public Map<Integer, Integer> getTanglingDegreeIFs() {
        return tanglingDegreeIFs;
    }

    public Integer getScatteringDegreeNIFs() {
        return scatteringDegreeNIFs;
    }

    public void setScatteringDegreeNIFs(Integer scatteringDegreeNIFs) {
        this.scatteringDegreeNIFs = scatteringDegreeNIFs;
    }

    public void addTanglingDegreeIFs(Integer numberFeatures, Integer numberlines) {
        this.tanglingDegreeIFs.put(numberFeatures,numberlines);
    }

    public Integer getNestingDegree() {
        return nestingDegree;
    }

    public void setNestingDegree(Integer nestingDegree) {
        this.nestingDegree = nestingDegree;
    }

    public Integer getNumberOfTopLevelBranches() {
        return numberOfTopLevelBranches;
    }

    public void setNumberOfTopLevelBranches(Integer numberOfTopLevelBranches) {
        this.numberOfTopLevelBranches = numberOfTopLevelBranches;
    }

    public Integer getNumberOfNonTopLevelBranches() {
        return numberOfNonTopLevelBranches;
    }

    public void setNumberOfNonTopLevelBranches(Integer numberOfNonTopLevelBranches) {
        this.numberOfNonTopLevelBranches = numberOfNonTopLevelBranches;
    }
}
