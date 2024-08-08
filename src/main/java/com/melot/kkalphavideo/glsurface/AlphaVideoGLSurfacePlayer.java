package com.melot.kkalphavideo.glsurface;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by siwen.hu on 2019/6/11.
 */
public class AlphaVideoGLSurfacePlayer extends GLSurfaceView {

    private GLVideoAlphaRenderer glVideoRenderer;

    public AlphaVideoGLSurfacePlayer(Context context) {
        super(context);
        init(context);
    }

    public AlphaVideoGLSurfacePlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        setVisibility(GONE);
        setZOrderOnTop(true);
        setEGLContextClientVersion(2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            getHolder().setFormat(PixelFormat.TRANSLUCENT);
        } else {
            setEGLConfigChooser(5, 6, 5, 8, 0, 0);
        }
        glVideoRenderer = new GLVideoAlphaRenderer(context);//创建renderer
        setRenderer(glVideoRenderer);//设置renderer
    }

    public void playVideo(String path) {
        setVisibility(View.VISIBLE);
        glVideoRenderer.playVideo(path);
    }

    @Override
    public void onPause() {
        super.onPause();
        glVideoRenderer.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        glVideoRenderer.onResume();
    }

    public void onDestroy() {
        setVisibility(View.GONE);
        glVideoRenderer.onDestroy();
    }

}
