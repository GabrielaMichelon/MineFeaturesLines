package at.jku.isse.gitecco.translation.changepropagation;

import at.jku.isse.gitecco.core.type.Feature;

import java.util.ArrayList;
import java.util.List;

public class ChangedFile {
    private ArrayList<Integer> linesInsert;
    private ArrayList<Integer> linesRemoved;
    private int oldFilePosition;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;
    private List<String> currentLines;
    private List<String> previousLines;
    private Feature feature;

    public ChangedFile(ArrayList<Integer> linesInsert, ArrayList<Integer> linesRemoved, int  oldFilePosition, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected, List<String> currentLines, List<String> previousLines, Feature feature) {
        this.linesInsert = linesInsert;
        this.linesRemoved = linesRemoved;
        this.oldFilePosition = oldFilePosition;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
        this.currentLines = currentLines;
        this.previousLines = previousLines;
        this.feature = feature;
    }

    public ArrayList<Integer> getLinesInsert() {
        return this.linesInsert;
    }

    public void setLinesInsert(ArrayList<Integer> linesInsert) {
        this.linesInsert = linesInsert;
    }

    public ArrayList<Integer> getLinesRemoved() {
        return this.linesRemoved;
    }

    public void setLinesRemoved(ArrayList<Integer> linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public int getOldFilePosition() {
        return this.oldFilePosition;
    }

    public void setOldFilePosition(int oldFilePosition) {
        this.oldFilePosition = oldFilePosition;
    }

    public ArrayList<String> getFeatureInteractions() {
        return this.featureInteractions;
    }

    public void setFeatureInteractions(ArrayList<String> featureInteractions) {
        this.featureInteractions = featureInteractions;
    }

    public ArrayList<String> getFeatureMightAffected() {
        return this.featureMightAffected;
    }

    public void setFeatureMightAffected(ArrayList<String> featureMightAffected) {
        this.featureMightAffected = featureMightAffected;
    }

    public List<String> getPreviousLines() {
        return this.previousLines;
    }

    public void setPreviousLines(List<String> previousLines) {
        this.previousLines = previousLines;
    }

    public List<String> getCurrentLines() {
        return this.currentLines;
    }

    public void setCurrentLines(List<String> currentLines) {
        this.currentLines = currentLines;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }
}
