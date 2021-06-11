package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Stack;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    private MainActivity ctx;

    // drawing path
    CPath path = null;
    ArrayList<CPath> paths;
    ArrayList<CPath> removedPaths;

    Stack<CEvent> redoStack;
    Stack<CEvent> undoStack;

    //Scale
    ScaleGestureDetector sgd;
    GestureDetector gd;
    float scaleFactor;
    float scaleFactor1;
    float scaleFactor2;
    float scaleFactor3;
    float scaleFactor4;

    // image to display
    Bitmap bitmap;
    Paint draw = new Paint();
    Paint highlight = new Paint();

    float lastFocusX;
    float lastFocusY;
    Matrix drawMatrix = new Matrix();


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            lastFocusX = detector.getFocusX();
            lastFocusY = detector.getFocusY();
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            scaleFactor4 = scaleFactor3;
//            scaleFactor3 = scaleFactor2;
//            scaleFactor2 = scaleFactor1;
//            scaleFactor1 = scaleFactor;
//            scaleFactor *= detector.getScaleFactor();
//            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f));
//            float average = (3*scaleFactor+3*scaleFactor1+2*scaleFactor2+scaleFactor3+scaleFactor4)/10;
//            PDFimage.this.setScaleX(average);
//            PDFimage.this.setScaleY(average);
//            return true;
            Matrix transformationMatrix = new Matrix();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            transformationMatrix.postTranslate(-focusX, -focusY);
            transformationMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor());

            float focusShiftX = focusX - lastFocusX;
            float focusShiftY = focusY - lastFocusY;
            transformationMatrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY);
            drawMatrix.postConcat(transformationMatrix);
            lastFocusX = focusX;
            lastFocusY = focusY;
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onScroll(MotionEvent downEvent, MotionEvent currentEvent, float distanceX, float distanceY) {
            drawMatrix.postTranslate(-distanceX, -distanceY);
            invalidate();
            return true;
        }

    }

    // constructor
    public PDFimage(MainActivity context) {
        super(context);
        this.ctx = context;
        paths = new ArrayList<CPath>();
        removedPaths = new ArrayList<CPath>();
        redoStack = new Stack<CEvent>();
        undoStack = new Stack<CEvent>();
        sgd = new ScaleGestureDetector(this.getContext(), new ScaleListener());
        gd = new GestureDetector(this.getContext(), new GestureListener());
        scaleFactor = 1.0f;
        scaleFactor1 = 1.0f;
        scaleFactor2 = 1.0f;
        scaleFactor3 = 1.0f;
        setBrush();
    }

    public void reset() {
        drawMatrix = new Matrix();
        redoStack.clear();
        undoStack.clear();
        removedPaths.clear();
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Matrix inverse = new Matrix();
        drawMatrix.invert(inverse);
        float ex = event.getX(); // - mx;
        float ey = event.getY();// - my;
        float[] pts = new float[2];
        pts[0] = ex;
        pts[1] = ey;
        inverse.mapPoints(pts);
        ex = pts[0];
        ey = pts[1];

        //MOVE
        if(ctx.activeTool.equals("move")) {
            sgd.onTouchEvent(event);
            gd.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(LOGNAME, "Action down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(LOGNAME, "Action move");

                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(LOGNAME, "Action up");

                    break;
            }
            return true;
        }

        //ERASE
        if(ctx.activeTool.equals("erase")) {
            for(CPath path: paths) {
                if(path.contains(ex, ey)) {
                    removedPaths.add(path);
                    Log.d("remove", "Path removed");

                }
            }

            for(CPath path: removedPaths) {
                paths.remove(path);
                undoStack.push(new CEvent(path,true));
            }
            redoStack.clear();
            removedPaths.clear();

            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Use if we want to implement one erase as one event
            }
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(LOGNAME, "Action down");
                path = new CPath(ctx.activeTool);
                path.moveTo(ex, ey);
                path.addPoint(ex, ey);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(LOGNAME, "Action move");
//                Log.d("addpoint", "Adding " + ex + " " + ey);
                // TODO Add more points to a line if two points are far apart
                path.lineTo(ex, ey);
                path.addPoint(ex, ey);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(LOGNAME, "Action up");
                paths.add(path);
                path.populate();
                undoStack.push(new CEvent(path,false));
                redoStack.clear();
                break;
        }
        return true;
    }

    public void undo() {
        if(undoStack.isEmpty()) {
            return;
        }
        CEvent event = undoStack.pop();

        if(event.isRemove) {
            paths.add(event.path);
        } else {
            paths.remove(event.path);
        }

        redoStack.push(event);
    }
    // TODO clear redo undo stack
    public void redo() {
        if(redoStack.isEmpty()) {
            return;
        }
        CEvent event = redoStack.pop();

        if(event.isRemove) {
            paths.remove(event.path);
        } else {
            paths.add(event.path);
        }

        undoStack.push(event);
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush() {
        draw.setColor(Color.BLUE);
        draw.setStyle(Paint.Style.STROKE);
        draw.setStrokeWidth(4);
        highlight.setColor(Color.YELLOW);
        highlight.setStyle(Paint.Style.STROKE);
        highlight.setStrokeWidth(23);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        Matrix inverse = new Matrix();
        drawMatrix.invert(inverse);
        // draw lines over it
        for (CPath path : paths) {
            path.transform(drawMatrix);
            canvas.drawPath(path, path.inputType.equals("draw") ? draw : highlight);
            path.transform(inverse);
        }
        canvas.setMatrix(drawMatrix);
        super.onDraw(canvas);
    }
}
