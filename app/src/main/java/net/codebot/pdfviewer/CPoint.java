package net.codebot.pdfviewer;

import java.io.Serializable;

public class CPoint implements Serializable {

    public int x;
    public int y;

    public CPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public CPoint(CPoint p) {
        this.x = p.x;
        this.y = p.y;
    }

    public int distance2(CPoint p2) {
        int dx = p2.x - x;
        int dy = p2.y - y;
        int distance2 = dx*dx + dy*dy;
        return distance2;
    }
}
