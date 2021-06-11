package net.codebot.pdfviewer;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provied them with this code.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final String DATAFILE = "datafile";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;
    ImageButton moveButton;
    ImageButton drawButton;
    ImageButton highlightButton;
    ImageButton eraseButton;
    ImageButton undoButton;
    ImageButton redoButton;
    ImageButton nextPageButton;
    ImageButton previousPageButton;

    TextView pageNum;

    int pageNumber = 0;

    public String activeTool = "move"; //Draw, Highlight, Erase, Move
    HashMap<Integer, ArrayList<CPath>> allPaths = new HashMap<>();



    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setPressed(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        moveButton = findViewById(R.id.moveButton);
        drawButton = findViewById(R.id.drawButton);
        highlightButton = findViewById(R.id.highlightButton);
        eraseButton = findViewById(R.id.eraseButton);
        undoButton = findViewById(R.id.undoButton);
        redoButton = findViewById(R.id.redoButton);
        nextPageButton = findViewById(R.id.nextPage);
        previousPageButton = findViewById(R.id.previousPage);

        moveButton.setBackgroundColor(Color.RED);

        pageNum = findViewById(R.id.currentPage);
        pageNum.setText(Integer.toString(pageNumber+1));

        init();
        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
//        try {
//            openRenderer(this);
//            showPage(0);
//            //closeRenderer();
//        } catch (IOException exception) {
//            Log.d(LOGNAME, "Error opening PDF");
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onStart() {
        super.onStart();
        readData();
        try {
            openRenderer(this);
            currentPage = null;
            showPage(0);
            //closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onStop() {
        super.onStop();
        savePaths();
        saveData();

        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }

    private void init() {
        moveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d("Buttons","move");
                toggleButton(1);
                activeTool = "move";
            }
        });

        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButton(2);
                activeTool = "draw";
            }
        });

        highlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButton(3);
                activeTool = "highlight";
            }
        });

        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButton(4);
                activeTool = "erase";
            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.undo();
            }
        });

        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.redo();
            }
        });

        previousPageButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                if(pageNumber == 0) {
                    return;
                }
                savePaths();
                pageNumber--;
                pageNum.setText(Integer.toString(pageNumber+1));
                pageImage.reset();
                pageImage.paths.clear();

                showPage(pageNumber);

                loadPaths(pageNumber);

                Log.d(LOGNAME, "Paths " + pageImage.paths.size());

            }
        });

        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pageNumber == pdfRenderer.getPageCount()-1) {
                    return;
                }
                savePaths();
                pageNumber++;
                pageNum.setText(Integer.toString(pageNumber+1));
                pageImage.reset();
                pageImage.paths.clear();


                showPage(pageNumber);

                loadPaths(pageNumber);
                Log.d(LOGNAME, "Paths " + pageImage.paths.size());


            }
        });
    }

    private void toggleButton(int i) {
        moveButton.setBackgroundColor(Color.TRANSPARENT);
        drawButton.setBackgroundColor(Color.TRANSPARENT);
        eraseButton.setBackgroundColor(Color.TRANSPARENT);
        highlightButton.setBackgroundColor(Color.TRANSPARENT);

        switch(i) {
            case 1:
                moveButton.setBackgroundColor(Color.RED);
                break;
            case 2:
                drawButton.setBackgroundColor(Color.RED);
                break;
            case 3:
                highlightButton.setBackgroundColor(Color.RED);
                break;
            case 4:
                eraseButton.setBackgroundColor(Color.RED);
                break;
        }
    }

    private void savePaths() {
        ArrayList<CPath> hash = new ArrayList<CPath>();//pageImage.paths);
        for(CPath path : pageImage.paths) {
            hash.add(new CPath(path));
        }
        allPaths.put(pageNumber, hash);
        Log.d(LOGNAME, "Stored " + hash.size());


    }

    private void loadPaths(int index) {
        pageImage.paths = allPaths.getOrDefault(index, new ArrayList<CPath>());
        //Log.d(LOGNAME, "Loaded " + pageImage.paths.size());

        allPaths.remove(index);
    }

    private void saveData() {
        Log.d(LOGNAME, "Saving Data");
        try {
            FileOutputStream fos = this.openFileOutput(DATAFILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(allPaths);
            os.close();
            fos.close();
        } catch (IOException ex) {
        Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    private void readData(){
        Log.d(LOGNAME, "Reading stored data: ");
        try {
            FileInputStream fis = this.openFileInput(DATAFILE);
            ObjectInputStream is = new ObjectInputStream(fis);
            allPaths = (HashMap<Integer, ArrayList<CPath>>) is.readObject();

            for (HashMap.Entry<Integer, ArrayList<CPath>> page : allPaths.entrySet()) {
                for (CPath path: page.getValue()){
                    path.loadPath();
                }
            }
            is.close();
            fis.close();
            loadPaths(0);
        } catch (Exception ex) {
            Log.d(LOGNAME, "Exception");

        }
    }

}
