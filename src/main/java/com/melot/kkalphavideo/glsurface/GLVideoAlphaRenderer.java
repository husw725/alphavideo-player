package com.melot.kkalphavideo.glsurface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.melot.kkalphavideo.OpenGlUtils;
import com.melot.kkalphavideo.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by siwen.hu on 2019/6/11.
 */
public class GLVideoAlphaRenderer implements GLSurfaceView.Renderer
        , SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnVideoSizeChangedListener {
    private static final String TAG = "GLVideoAlphaRenderer";
    private Context context;
    private int aPositionHandle;
    private int programId;
    private FloatBuffer vertexBuffer;
    //
    private final float[] vertexData = {
            //x,y,s,t  x,y为顶点坐标，s,t为纹理坐标
            -1f, 1f, 0.5f, 1f,
            1f, 1f, 1f, 1f,
            -1f, -1f, 0.5f, 0f,
            1f, -1f, 1f, 0f
    };

    private final float[] projectionMatrix = new float[16];
    private int uMatrixHandle;

    //    private final float[] textureVertexData = {
//            0.5f, 1f,
//            1.0f, 1f,
//            0.5f, 0f,
//            1.0f, 0f,
//    };
//    private FloatBuffer textureVertexBuffer;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int textureId;

    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;
    private float[] mSTMatrix = new float[16];
    private int uSTMMatrixHandle;

    private boolean updateSurface;
    private boolean playerPrepared;
    private int screenWidth, screenHeight;

    public GLVideoAlphaRenderer(Context context) {
        this.context = context;
        playerPrepared = false;
        synchronized (this) {
            updateSurface = false;
        }
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

//        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
//                .order(ByteOrder.nativeOrder())
//                .asFloatBuffer()
//                .put(textureVertexData);
//        textureVertexBuffer.position(0);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(true);
    }

    public void playVideo(String videoPath) {
        try {
            mediaPlayer.setDataSource(context, Uri.parse(videoPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexShader = OpenGlUtils.readShaderFromRawResource(context, R.raw.v_alpha_video);
        String fragmentShader = OpenGlUtils.readShaderFromRawResource(context, R.raw.f_alpha_video);
        programId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");

        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        //  mTriangle = new Triangle(-1);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        surface.release();

        if (!playerPrepared) {
            try {
                mediaPlayer.prepare();
                playerPrepared = true;
            } catch (IOException t) {
                Log.e(TAG, "media player prepare failed");
            }
            mediaPlayer.start();
            playerPrepared = true;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        float ratio = width > height ?
                (float) width / height :
                (float) height / width;
        //*2 与/2 是较正只显示一半视频的变形
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio * 2, ratio * 2, -1f, 1f, -1f, 1f);
        } else Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -ratio / 2, ratio / 2, -1f, 1f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        //  mTriangle.draw();
        synchronized (this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
        }
        GLES20.glUseProgram(programId);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                16, vertexBuffer);

        vertexBuffer.position(2);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glUniform1i(uTextureSamplerHandle, 0);
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        updateProjection(width, height);
    }

    private void updateProjection(int videoWidth, int videoHeight) {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
    }


    boolean pause = false;

    public void onPause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            pause = true;
        }
    }

    public void onResume() {
        if (mediaPlayer != null && pause) {
            mediaPlayer.start();
            pause = false;
        }
    }

    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
