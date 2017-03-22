package com.demo.mediahelper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.master.mediahelper.FileUtils;
import com.master.mediahelper.MediaHelper;

import java.io.File;

/**
 * Created by hb on 14/2/17.
 */

public class MediaActivity extends Activity {
    private static final String TAG = "MediaActivity";

    ImageView image;
    MediaHelper mediaHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        image = (ImageView) findViewById(R.id.image);
        findViewById(R.id.btn_take).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickerDialog();
            }
        });
        mediaHelper = new MediaHelper(this, "com.example.test.provider");
    }

    private void showImagePickerDialog() {
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MediaActivity.this);
        builder.setTitle("Add Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    if (ActivityCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MediaActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                        return;
                    }
                    mediaHelper.image(new MediaHelper.Callback() {
                        @Override
                        public void onResult(File file, String mimeType) {
                            Log.d(TAG, "onResult() called with: file = [" + file + "], mimeType = [" + mimeType + "]");
                            Glide.with(MediaActivity.this)
                                    .load(file.getPath())
                                    .into(image);
                        }
                    });
                } else if (items[item].equals("Choose from Library")) {
                    if (ActivityCompat.checkSelfPermission(MediaActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MediaActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                        return;
                    }
                    mediaHelper.file(FileUtils.MIME_TYPE_IMAGE, new MediaHelper.Callback() {
                        @Override
                        public void onResult(File file, String mimeType) {
                            Log.d(TAG, "onResult() called with: file = [" + file + "], mimeType = [" + mimeType + "]");
                            Glide.with(MediaActivity.this)
                                    .load(file.getPath())
                                    .into(image);
                        }
                    });
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mediaHelper != null)
            mediaHelper.onActivityResult(requestCode, resultCode, data);
    }
}
