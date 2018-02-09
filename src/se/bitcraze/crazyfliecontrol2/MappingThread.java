package se.bitcraze.crazyfliecontrol2;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Created by Paolo on 2/2/2018.
 */

public class MappingThread extends Thread{
    MappingView mv;
    public MappingThread(MappingView mv){
        this.mv = mv;
    }

    public void run(){
        SurfaceHolder sHolder = mv.getHolder();
        while( !Thread.interrupted()){
            Canvas c = sHolder.lockCanvas(null);
            try {
                synchronized (sHolder) {
                    mv.draw(c);
                }
            }catch(Exception e){
            }finally{
                if(c != null){
                    sHolder.unlockCanvasAndPost(c);
                }
            }
            try{
                Thread.sleep(1);
            }catch(InterruptedException e){
                return;
            }
        }
    }
}
