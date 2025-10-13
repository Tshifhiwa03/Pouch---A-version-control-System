package PouchVCS;

import java.io.Serializable;
import java.util.ArrayList;

public class CloneUnit implements Serializable {
    private ArrayList<FileMeta> fileList;
    private String cloneHashcode;
    private String title;          // Add this
    private String description;    // Add this

    // New constructor with title and description
    public CloneUnit(ArrayList<FileMeta> fileList, String cloneHashcode, String title, String description) {
        this.fileList = fileList;
        this.cloneHashcode = cloneHashcode;
        this.title = title;
        this.description = description;
    }

    // Existing constructor
    public CloneUnit(ArrayList<FileMeta> fileList, String cloneHashcode) {
        this.fileList = fileList;
        this.cloneHashcode = cloneHashcode;
        this.title = "";
        this.description = "";
    }

    // Getter methods
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
}
