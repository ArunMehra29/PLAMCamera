package camera.plam.com.plamcamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import camera.plam.com.plamcamera.api1.Camera1Activity;

public class CaptureImageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, Camera1Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }
}
