package chire.mod;

import arc.files.Fi;

public class PyModImport {
    public Fi file;

    public String code;

    public PyModImport(Fi pyFile){
        this.file = pyFile;
        this.code = pyFile.readString();
    }


}
