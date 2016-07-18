package com.plam.camera;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import plam.com.camera.api1.Camera1Activity;

public class CaptureImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);
        Intent intent = new Intent(this, Camera1Activity.class);
        startActivity(intent);
    }
}
