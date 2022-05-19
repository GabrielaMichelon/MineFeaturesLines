package at.jku.isse.gitecco.translation.changepropagation;

import at.jku.isse.gitecco.core.type.Feature;

import java.util.ArrayList;

public class DeletedFile {
    private ArrayList<Integer> linesRemoved;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;
    private Feature feature;

    public DeletedFile(ArrayList<Integer> linesRemoved, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected, Feature feature) {
        this.linesRemoved = linesRemoved;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
        this.feature = feature;
    }

    public ArrayList<Integer> getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(ArrayList<Integer> linesRemoved) {
        this.linesRemoved = linesRemoved;
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
