package net.codebot.pdfviewer;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class CPath extends Path implements Serializable {

    public ArrayList<CPoint> points;
    public ArrayList<CPoint> erasePoints;
    public String inputType;

    public CPath(String type) {
        super();
        points = new ArrayList<CPoint>();
        erasePoints  = new ArrayList<CPoint>();
        inputType = type;

    }

    public CPath(CPath path) {
        super(path);

        erasePoints  = new ArrayList<CPoint>();


        this.points = new ArrayList<>();
        for(CPoint p: path.points){
            points.add(new CPoint(p));
        }
        this.populate();

        this.inputType = path.inputType;
    }

    public void addPoint(float x, float y) {
        CPoint p = new CPoint((int) x, (int) y);
        points.add(p);
    }

    public boolean contains(float x, float y) {
        for(CPoint point: points) {
            Rect bound = new Rect(point.x - 40, point.y - 40, point.x + 40, point.y + 40);
            Log.d("contains", "Checking " + x + " " + y);

            if(bound.contains((int) x, (int) y)) {
                return true;
            }
        }
        for(CPoint point: erasePoints) {
            Rect bound = new Rect(point.x - 45, point.y - 45, point.x + 45, point.y + 45);
            Log.d("contains", "Checking " + x + " " + y);

            if(bound.contains((int) x, (int) y)) {
                return true;
            }
        }
        return false;

    }

    public void loadPath() {
        boolean flag = false;
        for(CPoint point : points) {
            if(!flag) {
                this.moveTo(point.x, point.y);
                flag = true;
            } else {
                this.lineTo(point.x, point.y);
            }
        }
    }

    public void populate() {
        for(int i = 0; i < points.size() - 1; i++) {
            CPoint p1 = points.get(i);
            CPoint p2 = points.get(i+1);
            if(p1.distance2(p2) > 3600) {
                int x = (int) (p1.x+p2.x)/2;
                int y = (int) (p1.y+p2.y)/2;
                erasePoints.add(new CPoint(x, y));
            }
            if(p1.distance2(p2) > 14400) {
                int x = (int) (p1.x+p2.x)/3;
                int y = (int) (p1.y+p2.y)/3;
                int x2 = (int) (p1.x+p2.x)*2/3;
                int y2 = (int) (p1.y+p2.y)*2/3;
                erasePoints.add(new CPoint(x, y));
                erasePoints.add(new CPoint(x2, y2));
            }
        }
    }
}
