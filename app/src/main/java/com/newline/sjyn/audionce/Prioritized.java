package com.newline.sjyn.audionce;

public abstract class Prioritized {
    private float priority;

    public void setPriority(float prior){
        priority = prior;
    }

    public float getPriority(){
        return priority;
    }
}
