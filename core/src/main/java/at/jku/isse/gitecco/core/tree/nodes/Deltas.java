package at.jku.isse.gitecco.core.tree.nodes;

public class Deltas {
    private int lineFromAdd = -1;
    private int lineToAdd = -1;
    private int totalLinesAdd = -1;
    private int posLinesAdd = -1;
    private int lineFromRemov = -1;
    private int lineToRemov = -1;
    private int totalLinesRemov = -1;
    private int posLinesRemov = -1;


    public Deltas(int lineFromAdd, int lineToAdd, int totalLinesAdd, int posLinesAdd, int lineFromRemov, int lineToRemov, int totalLinesRemov, int posLinesRemov) {
        this.lineFromAdd = lineFromAdd;
        this.lineToAdd = lineToAdd;
        this.totalLinesAdd = totalLinesAdd;
        this.posLinesAdd = posLinesAdd;
        this.lineFromRemov = lineFromRemov;
        this.lineToRemov = lineToRemov;
        this.totalLinesRemov = totalLinesRemov;
        this.posLinesRemov = posLinesRemov;
    }

    public int getLineFromAdd() {
        return lineFromAdd;
    }

    public void setLineFromAdd(int lineFromAdd) {
        this.lineFromAdd = lineFromAdd;
    }

    public int getLineToAdd() {
        return lineToAdd;
    }

    public void setLineToAdd(int lineToAdd) {
        this.lineToAdd = lineToAdd;
    }

    public int getTotalLinesAdd() {
        return totalLinesAdd;
    }

    public void setTotalLinesAdd(int totalLinesAdd) {
        this.totalLinesAdd = totalLinesAdd;
    }

    public int getPosLinesAdd() {
        return posLinesAdd;
    }

    public void setPosLinesAdd(int posLinesAdd) {
        this.posLinesAdd = posLinesAdd;
    }

    public int getLineFromRemov() {
        return lineFromRemov;
    }

    public void setLineFromRemov(int lineFromRemov) {
        this.lineFromRemov = lineFromRemov;
    }

    public int getLineToRemov() {
        return lineToRemov;
    }

    public void setLineToRemov(int lineToRemov) {
        this.lineToRemov = lineToRemov;
    }

    public int getTotalLinesRemov() {
        return totalLinesRemov;
    }

    public void setTotalLinesRemov(int totalLinesRemov) {
        this.totalLinesRemov = totalLinesRemov;
    }

    public int getPosLinesRemov() {
        return posLinesRemov;
    }

    public void setPosLinesRemov(int posLinesRemov) {
        this.posLinesRemov = posLinesRemov;
    }
}
