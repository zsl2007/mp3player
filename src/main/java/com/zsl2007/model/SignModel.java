package com.zsl2007.model;

public class SignModel {
    private int sign;

    public int getSign() {
        return sign;
    }

    public void setSign(int sign) {
        this.sign = sign;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    private boolean used;

    public SignModel(int sign, boolean used){
        this.sign = sign;
        this.used = used;
    }
}
