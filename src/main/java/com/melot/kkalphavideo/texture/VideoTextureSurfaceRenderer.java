package com.melot.kkalphavideo.texture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.provider.Settings;
import android.util.Log;

import com.melot.kkalphavideo.OpenGlUtils;
import com.melot.kkalphavideo.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by siwen.hu on 2019/6/14.
 */
public class VideoTextureSurfaceRenderer extends TextureSurfaceRenderer implements
        SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnVideoSizeChangedListener {

    private FitType mFitType = FitType.FIT_IN;

    float transX, transY, transZ;

    public void setTranslateM(float x, float y, float z) {
        transX = x;
        transY = y;
        transZ = z;
    }

    public enum FitType {
        FIT_IN,
        FIT_OUT//撑满View，不变形
    }

    public static final String TAG = VideoTextureSurfaceRenderer.class.getSimpleName();

    private static float vertexData[] = {
            -1, 1, 0.5f, 1.0f,
            1, 1, 1.0f, 1.0f,
            -1, -1, 0.5f, 0.0f,
            1, -1, 1.0f, 0.0f
    };


    private Context context;
    private int[] textures = new int[1];

    private int shaderProgram;
    private FloatBuffer vertexBuffer;

    private SurfaceTexture videoTexture;
    private float[] projectionMatrix;
    private boolean frameAvailable = false;

    int uTextureSamplerHandle;
    int aTextureCoordHandle;
    int aPositionHandle;
    int uMatrixHandle;
    private int uSTMMatrixHandle;
    private float[] mSTMatrix = new float[16];
    private int screenWidth;
    private int screenHeight;


    public VideoTextureSurfaceRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super(texture, width, height);
        this.context = context;
        projectionMatrix = new float[16];
    }

    private void setupGraphics() {
        String vertexShader = OpenGlUtils.readShaderFromRawResource(context, R.raw.v_alpha_video);
        String fragmentShader = OpenGlUtils.readShaderFromRawResource(context, R.raw.f_alpha_video);
        shaderProgram = OpenGlUtils.loadProgram(vertexShader, fragmentShader);

        GLES20.glUseProgram(shaderProgram);
        uTextureSamplerHandle = GLES20.glGetUniformLocation(shaderProgram, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMatrix");
        //修改变形
        uSTMMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uSTMatrix");

        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
    }

    private void setupVertexBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertexData.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertexData);
        vertexBuffer.position(0);
    }

    private void setupTexture() {
        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
        videoTexture.setOnFrameAvailableListener(this);
    }

    int playcount = 0;
    long lastTime = 0;

    @Override
    protected boolean draw() {
        if (lastTime == 0) lastTime = System.currentTimeMillis();
        playcount++;
        long current = System.currentTimeMillis();
        if (current - lastTime > 30000) {
            lastTime = current;
            Log.d("hsw", "texture alpha video fps=" + playcount / 30);
            playcount = 0;
        }

        synchronized (this) {
            if (frameAvailable) {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(mSTMatrix);
                frameAvailable = false;
            } else {
                return false;
            }

        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, width, height);
        this.drawTexture();

        return true;
    }

    private void drawTexture() {
        // Draw texture

        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(uTextureSamplerHandle, 0);

        vertexBuffer.position(2);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer);

        //  GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glDisableVertexAttribArray(aPositionHandle);
//        GLES20.glDisableVertexAttribArray(aTextureCoordHandle);
    }


    @Override
    protected void initGLComponents() {
        setupVertexBuffer();
        setupTexture();
        setupGraphics();
    }

    @Override
    protected void deinitGLComponents() {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.release();
        videoTexture.setOnFrameAvailableListener(null);
    }


    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    @Override
    public SurfaceTexture getVideoTexture() {
        return videoTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }

    public void onSurfaceTextureSizeChange(int width, int height) {
        Log.d("hsw", "onSurfaceTextureSizeChange width=" + width + ",height=" + height);
        screenWidth = width;
        screenHeight = height;
//        initScreenSize(width, height);
    }

    public void setFitTyp(FitType type) {
        mFitType = type;
        if (videoHeight > 0) updateProjection();
    }

    int videoWidth, videoHeight;

    private void updateProjection() {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (FitType.FIT_OUT.equals(mFitType)) {
            if (videoRatio < screenRatio) {
                Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
            } else
                Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        } else {
            if (videoRatio > screenRatio) {
                Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
            } else {
                Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
            }
        }
        if (FitType.FIT_IN.equals(mFitType)) {
            Matrix.translateM(projectionMatrix, 0, transX, transY, transZ);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d("hsw", "onVideoSizeChanged width=" + width + ",height=" + height);
        this.videoWidth = width / 2;
        this.videoHeight = height;
        updateProjection();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("hsw", "onSurfaceTextureAvailable width=" + width + ",height=" + height);
        initScreenSize(width, height);
    }

    private void initScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        float ratio = width > height ?
                (float) width / height :
                (float) height / width;
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f);
        } else Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -ratio, ratio, -1f, 1f);
    }


    public void onDestroy() {
        super.onDestroy();
    }
}