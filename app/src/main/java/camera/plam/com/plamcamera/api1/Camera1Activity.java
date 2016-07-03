package camera.plam.com.plamcamera.api1;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.FrameLayout;

import camera.plam.com.plamcamera.R;
import camera.plam.com.plamcamera.listener.ICameraIO;

public class Camera1Activity extends Activity implements ICameraIO {

    public static final String LOG_TAG = Camera1Activity.class.getSimpleName();
    private static final String CAMERA_FRAGMENT_TAG = "camera_fragment";

    private FrameLayout mFragmentContainer;

    private Camera1Fragment mCamera1Fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mCamera1Fragment = Camera1Fragment.getCamera1FragmentInstance();
        fragmentTransaction.add(mFragmentContainer.getId(), mCamera1Fragment, CAMERA_FRAGMENT_TAG);
        fragmentTransaction.commit();
        mCamera1Fragment.setCameraIOListener(this);
    }

    @Override
    public void onBackPressed() {
        if (mCamera1Fragment.isImagePreviewVisible()) {
            mCamera1Fragment.resumeCamera();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCameraOpenFailure() {
        //TODO unable to open camera
    }
}
