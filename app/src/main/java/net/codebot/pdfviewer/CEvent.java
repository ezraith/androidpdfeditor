package net.codebot.pdfviewer;

import java.util.ArrayList;

public class CEvent {

    CPath path;
    boolean isRemove;


    public CEvent (CPath path, boolean isRemove) {
        this.path = path;
        this.isRemove = isRemove;
    }

    public void reverse() {
        if(isRemove) {
            isRemove = false;
        } else {
            isRemove = true;
        }
    }


}
