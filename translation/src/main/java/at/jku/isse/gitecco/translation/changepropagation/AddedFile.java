package at.jku.isse.gitecco.translation.changepropagation;

import at.jku.isse.gitecco.core.type.Feature;

import java.util.ArrayList;

public class AddedFile {
    private ArrayList<Integer> linesInsert;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;
    private Feature feature;

    public AddedFile(ArrayList<Integer> linesInsert, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected, Feature feature) {
        this.linesInsert = linesInsert;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
        this.feature = feature;
    }

    public ArrayList<Integer> getLinesInsert() {
        return linesInsert;
    }

    public void setLinesInsert(ArrayList<Integer> linesInsert) {
        this.linesInsert = linesInsert;
    }

    public ArrayList<String> getFeatureInteractions() {
        return featureInteractions;
    }

    public void setFeatureInteractions(ArrayList<String> featureInteractions) {
        this.featureInteractions = featureInteractions;
    }

    public ArrayList<String> getFeatureMightAffected() {
        return featureMightAffected;
    }

    public void setFeatureMightAffected(ArrayList<String> featureMightAffected) {
        this.featureMightAffected = featureMightAffected;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }
}
