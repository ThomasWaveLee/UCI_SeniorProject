package se.bitcraze.crazyfliecontrol2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Vector;

/**
 * Created by Paolo on 2/2/2018.
 */

public class MappingView extends SurfaceView implements SurfaceHolder.Callback {

    private MappingThread mappingThread;
    private Canvas canvas;
    private Graph graph;
    private Vector<Graph.Node> nodeVector;
    private float height;
    private float width;

    private float x_origin;
    private float y_origin;

    private Paint nodePaint;
    private Paint edgePaint;

    MappingView(Context context){
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        canvas = new Canvas();
    }

    public void setGraph(Graph g){
        graph = g;
        nodeVector = graph.getAllNodes();
    }

    public void draw(Canvas canvas){
        canvas.drawColor(Color.GRAY);
        //drawMap(nodeVector.elementAt(0));

        for(Graph.Node n: nodeVector){
            for(Graph.Edge e: n.edgeList){
                    canvas.drawLine(x_origin + 40*n.coordinate.x, y_origin - 40*n.coordinate.y,
                            x_origin + 40*e.target.coordinate.x, y_origin - 40*e.target.coordinate.y,
                            edgePaint);
            }
        }
        for(Graph.Node n: nodeVector)
            canvas.drawCircle(x_origin + 40*n.coordinate.x, y_origin - 40*n.coordinate.y, 35, nodePaint);
        //update user state on map
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height){
        // Respond to surface changes
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        mappingThread.interrupt();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mappingThread = new MappingThread(this);
        mappingThread.start();

        height = getHeight();
        width = getWidth();
        x_origin = width / 2;
        y_origin = height / 2 - 40;

        nodePaint = new Paint();
        nodePaint.setColor(Color.BLUE);
        edgePaint = new Paint();
        edgePaint.setColor(Color.WHITE);
        edgePaint.setStrokeWidth(10f);
    }
}
