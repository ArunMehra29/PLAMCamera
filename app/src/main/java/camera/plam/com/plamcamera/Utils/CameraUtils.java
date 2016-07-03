package camera.plam.com.plamcamera.Utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by PunK _|_ RuLz on 13/10/15.
 */
public class CameraUtils {

    private static final String LOG_TAG = CameraUtils.class.getSimpleName();
    private static final String DIRECTORY_NAME = "PLAM";
    public static final int IMAGE_QUALITY = 100;

    public static final int DEGREE_ZERO = 0;
    public static final int DEGREE_NINETY = 90;
    public static final int DEGREE_ONE_EIGHTY = 180;
    public static final int DEGREE_TWO_SEVENTY = 270;
    public static final int DEGREE_THREE_SIXTY = 360;

    public static int mCurrentCameraId;

    private static Camera mCamera;

    public static Camera getCameraInstance(Activity activity, int cameraId) {
        mCamera = null;
        try {
            mCamera = Camera.open(cameraId);
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);
            Camera.CameraInfo info =
                    new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
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
            mCamera.setDisplayOrientation(result);
            mCurrentCameraId = cameraId;
        } catch (Exception e) {
            Log.d(LOG_TAG, "unable to open camera");
            e.printStackTrace();
        }
        return mCamera; // returns null if camera is unavailable
    }

    public static boolean isCameraFlashAvailable() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes == null) {
            return false;
        }

        for (String flashMode : flashModes) {
            if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)
                    || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)
                    || Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create a File for saving an image
     */
    public static File getOutputMediaFile() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), DIRECTORY_NAME);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("LOL", "failed to create directory");
                return null;
            }
        }
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                timeStamp + ".jpg");
        return mediaFile;
    }

    /**
     * Check if this device has a camera
     */
    public static boolean isCameraPresent(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * Determine the space between the first two fingers
     */
    public static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @TargetApi(23)
    public static boolean isCameraHardwareSupported(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(String.valueOf(CameraCharacteristics.LENS_FACING_FRONT));
            return CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ==
                    characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * round off float value to specified digits
     * @param number
     * @param digits
     * @return
     */
    public static float roundOff(float number, int digits) {
        BigDecimal bd = new BigDecimal(Float.valueOf(number));
        bd = bd.setScale(digits, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public enum CameraFlashState {
        ON,
        OFF,
        AUTO;

        private static CameraFlashState[] val = values();

        public CameraFlashState getNext() {
            return val[(this.ordinal() + 1) % val.length];
        }
    }

    public enum ScreenOrientation {
        PORTRAIT,
        LEFT_LANDSCAPE,
        PORTRAIT_UPSIDE_DOWN,
        RIGHT_LANDSCAPE;
    }

    public static ScreenOrientation getOrientation(int orientationValue) {
        if (DEGREE_ZERO <= orientationValue && DEGREE_NINETY > orientationValue) {
            return ScreenOrientation.PORTRAIT;
        } else if (DEGREE_NINETY <= orientationValue && DEGREE_ONE_EIGHTY > orientationValue) {
            return ScreenOrientation.RIGHT_LANDSCAPE;
        } else if (DEGREE_ONE_EIGHTY <= orientationValue && DEGREE_TWO_SEVENTY > orientationValue) {
            return ScreenOrientation.PORTRAIT_UPSIDE_DOWN;
        } else if (DEGREE_TWO_SEVENTY <= orientationValue && DEGREE_THREE_SIXTY > orientationValue) {
            return ScreenOrientation.LEFT_LANDSCAPE;
        }
        return null;
    }
}
