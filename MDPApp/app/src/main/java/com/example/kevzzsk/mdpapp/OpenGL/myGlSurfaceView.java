package com.example.kevzzsk.mdpapp.OpenGL;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/*
 * simple extention of the GLsurfaceview.  basically setup to use opengl 3.0
 * and set some configs.  This would be where the touch listener is setup to do something.
 *
 * It also declares and sets the render.
 */

public class myGlSurfaceView extends GLSurfaceView {

    myRenderer myRender;

    public myGlSurfaceView(Context context, int[][] gridMap) {
        super(context);
        // Create an OpenGL ES 3.0 context.
        setEGLContextClientVersion(3);

        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // Set the Renderer for drawing on the GLSurfaceView
        myRender = new myRenderer(context, gridMap);
        setRenderer(myRender);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }


    //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private static final float TOUCH_SCALE_FACTOR = 0.015f;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                //subtract, so the grid map moves the same direction as your finger.
                //with plus it moves the opposite direction.
                myRender.setX(myRender.getX() - (dx * TOUCH_SCALE_FACTOR));

                float dy = y - mPreviousY;
                myRender.setY(myRender.getY() - (dy * TOUCH_SCALE_FACTOR));
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

}