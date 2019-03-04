package com.example.qunjia.mdpapp.OpenGL;
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Seker on 7/2/2015.
 *
 *
 * Some code is uses from the OpenGL ES 3.0 programming guide second edition book.  used under the MIT license.
 *
 */
public class myRenderer implements GLSurfaceView.Renderer {

    private int mWidth;
    private int mHeight;
    private static String TAG = "myRenderer";
    private static GridMap3D gridMap3D;
    private static float mAngle =0;
    private static float mTransY=0f;
    private static float mTransX=0f;
    private static float mTransZ=0f;
    private static final float Z_NEAR = 1f;
    private static final float Z_FAR = 40f;

    private static float mDirection = 0;
    private static Boolean turnRight = false;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];

    private static int[][] gridMap;
    private Context context;
    //
    myRenderer(Context c, int[][] gridMap) {
        myRenderer.gridMap = gridMap;
        context = c;
    }
    ///
    // Create a shader object, load the shader source, and
    // compile the shader.
    //
    public static int LoadShader(int type, String shaderSrc) {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES30.glCreateShader(type);

        if (shader == 0) {
            return 0;
        }

        // Load the shader source
        GLES30.glShaderSource(shader, shaderSrc);

        // Compile the shader
        GLES30.glCompileShader(shader);

        // Check the compile status
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e(TAG, "Error!!!!");
            Log.e(TAG, GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    ///
    // Initialize the shader and program object
    //
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        //set the clear buffer color to light gray.
        GLES30.glClearColor(0.9f, .9f, 0.9f, 0.9f);
        //initialize the gridMap code for drawing.

        gridMap3D = new GridMap3D(context, gridMap);
        //if we had other objects setup them up here as well.
    }

    // /
    // Draw a triangle using the shader pair created in onSurfaceCreated()
    //
    public void onDrawFrame(GL10 glUnused) {

        // Clear the color buffer  set above by glClearColor.
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        //need this otherwise, it will over right stuff and the grid map will look wrong!
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // Set the camera position (View matrix)  note Matrix is an include, not a declared method.
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -1,
                0f, 0f, 0f,
                0f, 1.0f, 0.0f);

        // Create a rotation and translation for the grid map
        Matrix.setIdentityM(mRotationMatrix, 0);

        //translate everything to 0 before rotating
        Matrix.translateM(mRotationMatrix, 0, 0, 0, 0);
        Matrix.rotateM(mRotationMatrix, 0, mAngle, 0, 1, 0);
        int magnitude = 2;
        if (Math.abs(mDirection - mAngle) >= 180) {
            magnitude = magnitude * 2;
        }
        if(turnRight && mDirection > mAngle){
            mAngle += magnitude;
        }
        else if(!turnRight && mDirection < mAngle){
            mAngle -= magnitude;
        }

        //move the grid map up/down and left/right
        Matrix.translateM(mRotationMatrix, 0, mTransX, mTransY, mTransZ);


        // combine the model with the view matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mRotationMatrix, 0);

        // combine the model-view with the projection matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        gridMap3D.draw(mMVPMatrix);

    }

    // /
    // Handle surface changes
    //
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        mWidth = width;
        mHeight = height;
        // Set the viewport
        GLES30.glViewport(0, 0, mWidth, mHeight);
        float aspect = (float) width / height;

        // this projection matrix is applied to object coordinates
        //no idea why 53.13f, it was used in another example and it worked.
        Matrix.perspectiveM(mProjectionMatrix, 0, 53.13f, aspect, Z_NEAR, Z_FAR);
    }

    //used the touch listener to move the grid map up/down (y) and left/right (x)
    public static float getY() {
        return mTransY;
    }

    public static void setY(float mY) {
        mTransY = mY;
    }

    public static float getX() {
        return mTransX;
    }

    public static void setX(float mX) {
        mTransX = mX;
    }

    public static float getZ() {
        return mTransZ;
    }

    public static void setZ(float mZ) {
        mTransZ = mZ;
    }

    public static void rotateRight() {
        mDirection += 90;
        turnRight = true;
    }

    public static void rotateLeft() {
        mDirection -= 90;
        turnRight = false;
    }

    public static float  getRotation(){
        return  mDirection;
    }
}