package PouchVCS;

import java.io.Serializable;
import java.util.ArrayList;

public class CloneUnit implements Serializable {
    private ArrayList<FileMeta> fileList;
    private String cloneHashcode;
    private String title;
    private String description;

    // Existing constructors
    public CloneUnit() {
        this.fileList = new ArrayList<>();
    }

    public CloneUnit(ArrayList<FileMeta> fileList, String cloneHashcode) {
        this.fileList = fileList;
        this.cloneHashcode = cloneHashcode;
    }

    // ✅ New constructor to include title and description
    public CloneUnit(ArrayList<FileMeta> fileList, String cloneHashcode, String title, String description) {
        this.fileList = fileList;
        this.cloneHashcode = cloneHashcode;
        this.title = title;
        this.description = description;
    }

    // Getters
    public ArrayList<FileMeta> getFileList() {
        return fileList;
    }

    public String getCloneHashcode() {
        return cloneHashcode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    // Setters if needed
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
