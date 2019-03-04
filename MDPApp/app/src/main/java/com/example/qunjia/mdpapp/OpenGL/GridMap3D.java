package com.example.qunjia.mdpapp.OpenGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES30;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.qunjia.mdpapp.R;

public class GridMap3D {
    private int mProgramObject;
    private FloatBuffer mVertices;
    private final float size = 0.5f;//initial size
    private  final float aSize = 0.33f;
    private int[][] gridMap;
    private Context context;
    private int exploredNo, unexploredNo, obstaclesNo;
    //vertex shader code
    private String vShaderStr =
            "#version 300 es 			  \n"
                    + "uniform mat4 uMVPMatrix;     \n"
                    + "in vec4 vPosition;           \n"
                    + "void main()                  \n"
                    + "{                            \n"
                    + "   gl_Position = uMVPMatrix * vPosition;  \n"
                    + "}                            \n";
    //fragment shader code.
    private String fShaderStr =
            "#version 300 es		 			          	\n"
                    + "precision mediump float;					  	\n"
                    + "uniform vec4 vColor;	 			 		  	\n"
                    + "out vec4 fragColor;	 			 		  	\n"
                    + "void main()                                  \n"
                    + "{                                            \n"
                    + "  fragColor = vColor;                    	\n"
                    + "}                                            \n";

    String TAG = "3D grid map";


    //finally some methods
    //constructor
    GridMap3D(Context c, int[][] gridMap) {
        context = c;
        exploredNo = Integer.parseInt(context.getResources().getString(R.string.exploredNo));
        unexploredNo = Integer.parseInt(context.getResources().getString(R.string.unexploredNo));
        obstaclesNo = Integer.parseInt(context.getResources().getString(R.string.obstaclesNo));
        float[] mVerticesData = CreateVerticesData(gridMap);
        //first setup the mVertices correctly.
        mVertices = ByteBuffer
                .allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVerticesData);
        mVertices.position(0);

        //setup the shaders
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = myRenderer.LoadShader(GLES30.GL_VERTEX_SHADER, vShaderStr);
        fragmentShader = myRenderer.LoadShader(GLES30.GL_FRAGMENT_SHADER, fShaderStr);

        // Create the program object
        programObject = GLES30.glCreateProgram();

        if (programObject == 0) {
            Log.e(TAG, "So some kind of error, but what?");
            return;
        }

        GLES30.glAttachShader(programObject, vertexShader);
        GLES30.glAttachShader(programObject, fragmentShader);

        // Bind vPosition to attribute 0
        GLES30.glBindAttribLocation(programObject, 0, "vPosition");

        // Link the program
        GLES30.glLinkProgram(programObject);

        // Check the link status
        GLES30.glGetProgramiv(programObject, GLES30.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e(TAG, "Error linking program:");
            Log.e(TAG, GLES30.glGetProgramInfoLog(programObject));
            GLES30.glDeleteProgram(programObject);
            return;
        }

        // Store the program object
        mProgramObject = programObject;

        //now everything is setup and ready to draw.
    }

    public void draw(float[] mvpMatrix) {

        // Use the program object
        GLES30.glUseProgram(mProgramObject);

        // get handle to shape's transformation matrix
        int mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramObject, "uMVPMatrix");
        myRenderer.checkGlError("glGetUniformLocation");

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES30.glGetUniformLocation(mProgramObject, "vColor");


        // Apply the projection and view transformation
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        myRenderer.checkGlError("glUniformMatrix4fv");

        int VERTEX_POS_INDX = 0;
        mVertices.position(VERTEX_POS_INDX);  //just in case.  We did it already though.

        //add all the points to the space, so they can be correct by the transformations.
        //would need to do this even if there were no transformations actually.
        GLES30.glVertexAttribPointer(VERTEX_POS_INDX, 3, GLES30.GL_FLOAT,
                false, 0, mVertices);
        GLES30.glEnableVertexAttribArray(VERTEX_POS_INDX);

        //grid map color
        int i = 0;
        for(int r = 0; r < gridMap.length; r++) {
            for (int c = 0; c < gridMap[r].length; c++){
                if(gridMap[r][c] == unexploredNo) {
                    float[] unexploredColor =  getFloatArrayFromARGB(ContextCompat.getColor(context, R.color.unexplored));
                    GLES30.glUniform4fv(mColorHandle, 1,unexploredColor, 0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 6);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#000000")), 0);//black
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 6);
                    GLES30.glLineWidth(2);
                    i+=6;
                } else if(gridMap[r][c] == exploredNo) {
                    float[] exploredColor =  getFloatArrayFromARGB(ContextCompat.getColor(context, R.color.explored));
                    GLES30.glUniform4fv(mColorHandle, 1,exploredColor, 0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 6);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#000000")), 0);//black
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 6);
                    GLES30.glLineWidth(2);
                    i+=6;
                } else if(gridMap[r][c] == obstaclesNo){
                    float[] obstaclesColor =  getFloatArrayFromARGB(ContextCompat.getColor(context, R.color.obstacle));
                    //white part of arrow (1st face)
                    GLES30.glUniform4fv(mColorHandle, 1,getFloatArrayFromARGB(Color.parseColor("#FFFFFF")) ,0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 18);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#FFFFFF")), 0);//white
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 18);
                    GLES30.glLineWidth(2);
                    i+=18;
                    //black part of arrow (1st face)
                    GLES30.glUniform4fv(mColorHandle, 1,obstaclesColor ,0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 9);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#FFFFFF")), 0);//white
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 9);
                    GLES30.glLineWidth(2);
                    i+=9;
                    //2nd,3rd,4th,5th,6th face
                    GLES30.glUniform4fv(mColorHandle, 1,obstaclesColor ,0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 30);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#FFFFFF")), 0);//white
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 30);
                    GLES30.glLineWidth(2);
                    i+= 30;
                }
                /*else if(gridMap[r][c] == obstaclesNo){
                    float[] obstaclesColor =  getFloatArrayFromARGB(ContextCompat.getColor(context, R.color.obstacle));
                    GLES30.glUniform4fv(mColorHandle, 1,obstaclesColor ,0);
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i, 36);
                    GLES30.glUniform4fv(mColorHandle, 1, getFloatArrayFromARGB(Color.parseColor("#FFFFFF")), 0);//white
                    GLES30.glDrawArrays(GLES30.GL_LINES, i, 36);
                    GLES30.glLineWidth(2);
                    i+=36;
                }*/
            }
        }
    }

    private float[] CreateVerticesData(int[][] gridMap) {
        this.gridMap = gridMap;
        ArrayList<Float> arrayList = new ArrayList();
        for (int r = 0; r < gridMap.length; r++) {
            for (int c = 0; c< gridMap[r].length; c++){
                if (gridMap[r][c] == exploredNo) {
                    arrayList.addAll(Arrays.asList(getFlatGroundVertices(c, r)));
                }
                else if (gridMap[r][c] == unexploredNo) {
                    arrayList.addAll(Arrays.asList(getFlatGroundVertices(c, r)));
                }
                else if (gridMap[r][c] == obstaclesNo) {
                    //arrayList.addAll(Arrays.asList(getObstacleVertices(c, r)));
                    arrayList.addAll(Arrays.asList(getRightArrowVertices(c, r)));
                }
            }
        }

        float[] result = new float[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            result[i] = arrayList.get(i);
        }

        return result;
    }

    private float[] getFloatArrayFromARGB(int color_base){
        //int color_base = Color.parseColor(color_str);
        int red = Color.red(color_base);
        int green = Color.green(color_base);
        int blue = Color.blue(color_base);
        int alpha = Color.alpha(color_base);

        return new float[]{
                (red / 255f),
                (green / 255f),
                (blue / 255f),
                (alpha / 255f)
        };
    }
    private Float[] getFlatGroundVertices(int colNo, int rowNo){
        return new Float[]{
                (2 * colNo)     * size  , -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size  , -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size  , -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size  , -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size  , -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size  , -size, size * (2 * rowNo), // top-left
        };
    }

    private Float[] getObstacleVertices(int colNo, int rowNo){
        return new Float[]{
                ////////////////////////////////////////////////////////////////////
                // right
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // FRONT
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                ////////////////////////////////////////////////////////////////////
                // BACK
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left

                ////////////////////////////////////////////////////////////////////
                // TOP
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BOTTOM
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // left
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
        };
    }

    private Float[] getFrontArrowVertices(int colNo, int rowNo){
        return new Float[]{
                ////////////////////////////////////////////////////////////////////
                // FRONT
                ////////////////////////////////////////////////////////////////////
                //White
                (2 * colNo)     * size,  size         , size * (2 * rowNo + 2), // top-left
                (2 * colNo)     * size,  aSize * size , size * (2 * rowNo + 2), // arrow-left
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo + 2), // arrow-top

                (2 * colNo + 2) * size,  size         , size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo + 2), // arrow-top

                (2 * colNo)     * size, -size         , size * (2 * rowNo + 2), // bottom-left
                (2 * colNo)     * size, aSize * size  , size * (2 * rowNo + 2), // arrow-left
                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-left

                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-left
                (2 * colNo+0.5f)* size, aSize * size  , size * (2 * rowNo + 2), // arrow-middle-left
                (2 * colNo)     * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-left

                (2 * colNo + 2) * size, -size         , size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-right

                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-right
                (2 * colNo+1.5f)* size,  aSize *size  , size * (2 * rowNo + 2), // arrow-middle-right
                (2 * colNo+2)   * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right

                // black
                (2 * colNo)     * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-left
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo + 2), // arrow-top

                (2 * colNo+0.5f)* size,  aSize *size  , size * (2 * rowNo + 2), // arrow-middle-left
                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-left
                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-right

                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo + 2), // arrow-bottom-right
                (2 * colNo+1.5f)* size,  aSize *size  , size * (2 * rowNo + 2), // arrow-middle-right
                (2 * colNo+0.5f)* size,  aSize *size  , size * (2 * rowNo + 2), // arrow-middle-left
                ////////////////////////////////////////////////////////////////////
                // right
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BACK
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left

                ////////////////////////////////////////////////////////////////////
                // TOP
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BOTTOM
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // left
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
        };
    }

    private Float[] getBackArrowVertices(int colNo, int rowNo){
        return new Float[]{
                ////////////////////////////////////////////////////////////////////
                // BACK
                ////////////////////////////////////////////////////////////////////
                //White
                (2 * colNo)     * size,  size         , size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  aSize * size , size * (2 * rowNo), // arrow-left
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo), // arrow-top

                (2 * colNo + 2) * size,  size         , size * (2 * rowNo), // top-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo), // arrow-right
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo), // arrow-top

                (2 * colNo)     * size, -size         , size * (2 * rowNo), // bottom-left
                (2 * colNo)     * size, aSize * size  , size * (2 * rowNo), // arrow-left
                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-left

                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-left
                (2 * colNo+0.5f)* size, aSize * size  , size * (2 * rowNo), // arrow-middle-left
                (2 * colNo)     * size,  aSize *size  , size * (2 * rowNo), // arrow-left

                (2 * colNo + 2) * size, -size         , size * (2 * rowNo), // bottom-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo), // arrow-right
                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-right

                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-right
                (2 * colNo+1.5f)* size,  aSize *size  , size * (2 * rowNo), // arrow-middle-right
                (2 * colNo+2)   * size,  aSize *size  , size * (2 * rowNo), // arrow-right

                // black
                (2 * colNo)     * size,  aSize *size  , size * (2 * rowNo), // arrow-left
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo), // arrow-right
                (2 * colNo + 1) * size,  size         , size * (2 * rowNo), // arrow-top

                (2 * colNo+0.5f)* size,  aSize *size  , size * (2 * rowNo), // arrow-middle-left
                (2 * colNo+0.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-left
                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-right

                (2 * colNo+1.5f)* size, -size         , size * (2 * rowNo), // arrow-bottom-right
                (2 * colNo+1.5f)* size,  aSize *size  , size * (2 * rowNo), // arrow-middle-right
                (2 * colNo+0.5f)* size,  aSize *size  , size * (2 * rowNo), // arrow-middle-left
                ////////////////////////////////////////////////////////////////////
                // right
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // FRONT
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                ////////////////////////////////////////////////////////////////////
                // TOP
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BOTTOM
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // left
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
        };
    }

    private Float[] getRightArrowVertices(int colNo, int rowNo){
        return new Float[]{
                ////////////////////////////////////////////////////////////////////
                // right
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                //White
                (2 * colNo) * size,  size         , size * (2 * rowNo), // top-left
                (2 * colNo) * size,  aSize * size , size * (2 * rowNo), // arrow-left
                (2 * colNo) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo) * size,  size         , size * (2 * rowNo + 2), // top-right
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo) * size, -size         , size * (2 * rowNo), // bottom-left
                (2 * colNo) * size, aSize * size  , size * (2 * rowNo), // arrow-left
                (2 * colNo) * size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left

                (2 * colNo) * size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left
                (2 * colNo) * size, aSize * size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo), // arrow-left

                (2 * colNo) * size, -size         , size * (2 * rowNo + 2), // bottom-right
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo) * size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right

                (2 * colNo) * size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo+1.5f), // arrow-middle-right
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo + 2 ), // arrow-right

                // black
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo), // arrow-left
                (2 * colNo) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo)* size,  aSize *size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                (2 * colNo)* size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left
                (2 * colNo)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right

                (2 * colNo)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right
                (2 * colNo)* size,  aSize *size  , size * (2 * rowNo+1.5f), // arrow-middle-right
                (2 * colNo)* size,  aSize *size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                ////////////////////////////////////////////////////////////////////
                // FRONT
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                ////////////////////////////////////////////////////////////////////
                // BACK
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left

                ////////////////////////////////////////////////////////////////////
                // TOP
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BOTTOM
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // left
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-left
        };
    }
    
    private Float[] getLeftArrowVertices(int colNo, int rowNo){
        return new Float[]{
                ////////////////////////////////////////////////////////////////////
                // left
                ////////////////////////////////////////////////////////////////////
                //White
                (2 * colNo + 2)     * size,  size         , size * (2 * rowNo), // top-left
                (2 * colNo + 2)     * size,  aSize * size , size * (2 * rowNo), // arrow-left
                (2 * colNo + 2) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo + 2) * size,  size         , size * (2 * rowNo + 2), // top-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo + 2) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo + 2)     * size, -size         , size * (2 * rowNo), // bottom-left
                (2 * colNo + 2)     * size, aSize * size  , size * (2 * rowNo), // arrow-left
                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left

                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left
                (2 * colNo + 2)* size, aSize * size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                (2 * colNo + 2)     * size,  aSize *size  , size * (2 * rowNo), // arrow-left

                (2 * colNo + 2) * size, -size         , size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right

                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right
                (2 * colNo + 2)* size,  aSize *size  , size * (2 * rowNo+1.5f), // arrow-middle-right
                (2 * colNo + 2)   * size,  aSize *size  , size * (2 * rowNo + 2 ), // arrow-right

                // black
                (2 * colNo + 2)     * size,  aSize *size  , size * (2 * rowNo), // arrow-left
                (2 * colNo + 2) * size,  aSize *size  , size * (2 * rowNo + 2), // arrow-right
                (2 * colNo + 2) * size,  size         , size * (2 * rowNo + 1), // arrow-top

                (2 * colNo + 2)* size,  aSize *size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+0.5f), // arrow-bottom-left
                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right

                (2 * colNo + 2)* size, -size         , size * (2 * rowNo+1.5f), // arrow-bottom-right
                (2 * colNo + 2)* size,  aSize *size  , size * (2 * rowNo+1.5f), // arrow-middle-right
                (2 * colNo + 2)* size,  aSize *size  , size * (2 * rowNo+0.5f), // arrow-middle-left
                ////////////////////////////////////////////////////////////////////
                // right
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // FRONT
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // top-left
                ////////////////////////////////////////////////////////////////////
                // BACK
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left

                ////////////////////////////////////////////////////////////////////
                // TOP
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size,  size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size,  size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size,  size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size,  size, size * (2 * rowNo), // top-left
                ////////////////////////////////////////////////////////////////////
                // BOTTOM
                ////////////////////////////////////////////////////////////////////
                // Triangle 1
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
                (2 * colNo)     * size, -size, size * (2 * rowNo + 2), // bottom-left
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                // Triangle 2
                (2 * colNo + 2) * size, -size, size * (2 * rowNo + 2), // bottom-right
                (2 * colNo + 2) * size, -size, size * (2 * rowNo), // top-right
                (2 * colNo)     * size, -size, size * (2 * rowNo), // top-left
        };
    }
}
