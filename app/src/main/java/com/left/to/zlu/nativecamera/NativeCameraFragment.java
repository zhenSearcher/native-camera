package com.left.to.zlu.nativecamera;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by user on 8/6/14.
 */
public class NativeCameraFragment extends Fragment {

    // Native camera.
    private Camera mCamera;
    // View to display the camera output.
    private CameraPreview mPreview;

    public NativeCameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_native_camera, container, false);

        // Create our Preview view and set it as the content of our activity.
        boolean opened = safeCameraOpenInView(rootView);

        if (opened == false) {
            Log.d("CameraGuide", "Error, Camera failed to open");
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
    }

    /**
     * Recommended "safe" way to open the camera.
     *
     * @param view
     * @return
     */
    private boolean safeCameraOpenInView(View view) {
        boolean qOpened = false;
        releaseCameraAndPreview();
        mCamera = getCameraInstance();
        setCameraDisplayOrientation(getActivity(), Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        qOpened = (mCamera != null);

        if (qOpened == true) {
            mPreview = new CameraPreview(getActivity().getBaseContext(), mCamera);
            FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
            preview.addView(mPreview);
            mPreview.startCameraPreview();
        }
        return qOpened;
    }

    /**
     * Safe method for getting a camera instance.
     *
     * @return
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mPreview != null) {
            mPreview.destroyDrawingCache();
            mPreview.setCamera(null);
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}