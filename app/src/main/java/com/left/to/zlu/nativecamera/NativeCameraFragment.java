package com.left.to.zlu.nativecamera;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by user on 8/6/14.
 */
public class NativeCameraFragment extends Fragment {

    Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {

            String fileName = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString()
                    + File.separator
                    + "PicTest_" + System.currentTimeMillis() + ".jpg";
            File file = new File(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }

            try {
                BufferedOutputStream bufferedStream = new BufferedOutputStream(
                        new FileOutputStream(file));
                bufferedStream.write(arg0);
                bufferedStream.flush();
                bufferedStream.close();
                scanFileToPhotoAlbum(file.getAbsolutePath());

                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                exif.setAttribute("UserComment", "Jpeg Caption as comment ...");
                exif.saveAttributes();
                ShowExif(exif);

                Toast.makeText(getActivity(),
                        "[Test] Photo take and store in" + file.toString(),
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Picture Failed" + e.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    private void ShowExif(ExifInterface exif) {
        String myAttribute = "Exif information ---\n";
        myAttribute += getTagString(ExifInterface.TAG_DATETIME, exif);
        myAttribute += getTagString(ExifInterface.TAG_FLASH, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
        myAttribute += getTagString(ExifInterface.TAG_IMAGE_LENGTH, exif);
        myAttribute += getTagString(ExifInterface.TAG_IMAGE_WIDTH, exif);
        myAttribute += getTagString(ExifInterface.TAG_MAKE, exif);
        myAttribute += getTagString(ExifInterface.TAG_MODEL, exif);
        myAttribute += getTagString(ExifInterface.TAG_ORIENTATION, exif);
        myAttribute += getTagString(ExifInterface.TAG_WHITE_BALANCE, exif);
        myAttribute += getTagString("UserComment", exif);
        metaDataText.setText(myAttribute);
    }

    private String getTagString(String tag, ExifInterface exif) {
        return (tag + " : " + exif.getAttribute(tag) + "\n");
    }

    public void scanFileToPhotoAlbum(String path) {

        MediaScannerConnection.scanFile(getActivity(),
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                }
        );
    }

    // Native camera.
    private Camera mCamera;
    // View to display the camera output.
    private CameraPreview mPreview;
    private TextView metaDataText;

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

        metaDataText = (TextView) rootView.findViewById(R.id.meta_data);

        Button captureButton = (Button) rootView.findViewById(R.id.camera_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, null, jpegPictureCallback);
                    }
                }
        );

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
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
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
