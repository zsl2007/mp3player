package com.zsl2007.model;

import java.util.ArrayList;
import java.util.List;

public class SignSaveModel {
    String filename;
    List<SignModel> siglist;

    public String getFilename() {
        return filename;
    }

    public List<SignModel> getSiglist() {
        return siglist;
    }

    public void setSiglist(List<SignModel> siglist) {
        this.siglist = siglist;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


}
