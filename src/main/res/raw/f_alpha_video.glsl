#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTexCoord;
uniform samplerExternalOES sTexture;
void main() {
    //  gl_FragColor=texture2D(sTexture, vTexCoord);
    if (vTexCoord.x>=0.5){
        vec4 color = texture2D(sTexture, vTexCoord);
        vec4 alpha = texture2D(sTexture, vTexCoord+ vec2(-0.5, 0));
        gl_FragColor = vec4(color.rgb, alpha.r);
    }
}