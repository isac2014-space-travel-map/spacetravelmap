package info.izumin.android.isac2014.moverioapp.model;

import android.hardware.Camera;

/**
 * Created by izumin on 1/20/14.
 */
public class CameraProvider {
    private static final String TAG = CameraProvider.class.getSimpleName();
    private final CameraProvider self = this;

    private static Camera mCamera;

    /**
     * A safe way to get an instance of the Camera object.
     * (from: http://developer.android.com/guide/topics/media/camera.html#access-camera)
     * @return new instance of the Camera object
     */
    public static Camera getInstance() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
            }
        }
        return mCamera;
    }

    public static void release() {
        if (mCamera !=  null) {
            mCamera.release();
            mCamera = null;
        }
    }
}
