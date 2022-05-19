package at.jku.isse.gitecco.translation.changepropagation;

import java.util.ArrayList;

public class Changes {
    private ArrayList<ChangedFile> changedFiles;
    private ArrayList<AddedFile> addedFiles;
    private ArrayList<DeletedFile> deletedFiles;

    public Changes(ArrayList<ChangedFile> changedFiles, ArrayList<AddedFile> addedFiles, ArrayList<DeletedFile> deletedFiles) {
        this.changedFiles = changedFiles;
        this.addedFiles = addedFiles;
        this.deletedFiles = deletedFiles;
    }

    public Changes() {
        this.changedFiles = new ArrayList<>();
        this.addedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
    }

    public ArrayList<ChangedFile> getChangedFiles() {
        return changedFiles;
    }

    public void addChangedFiles(ChangedFile changedFiles) {
        this.changedFiles.add(changedFiles);
    }

    public ArrayList<AddedFile> getAddedFiles() {
        return addedFiles;
    }

    public void addAddedFiles(AddedFile addedFiles) {
        this.addedFiles.add(addedFiles);
    }

    public ArrayList<DeletedFile> getDeletedFiles() {
        return deletedFiles;
    }

    public void addDeletedFiles(DeletedFile deletedFiles) {
        this.deletedFiles.add(deletedFiles);
    }
}
