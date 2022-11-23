package com.zsl2007.model;

public class ConvSignModel {

    private int start;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    private int end;


    public ConvSignModel(int start, int end){
        this.start = start;
        this.end = end;
    }
}
