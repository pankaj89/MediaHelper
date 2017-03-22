package com.master.mediahelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.app.Activity.RESULT_OK;
import static com.master.mediahelper.FileUtils.MIME_TYPE_IMAGE;

/**
 * Created by Pankaj on 14/2/17.
 * For Nougat support, Copy and past in res/xml/provider_paths.xml
 * <paths xmlns:android="http://schemas.android.com/apk/res/android">
 * <external-path
 * name="external_files"
 * path="." />
 * </paths>
 * <p>
 * Manifest entry
 * ---------------
 * <application ...
 * <p>
 * <provider
 * android:name="android.support.v4.content.FileProvider"
 * android:authorities="com.example.test.provider"
 * android:exported="false"
 * android:grantUriPermissions="true">
 * <meta-data
 * android:name="android.support.FILE_PROVIDER_PATHS"
 * android:resource="@xml/provider_paths" />
 * </provider>
 * <p>
 * </application>
 * <p>
 * <p>
 * Use
 * ------
 * MediaHelper mediaHelper = new MediaHelper(this, "com.example.test.provider");
 * mediaHelper.image();
 * mediaHelper.file();
 */

public class MediaHelper {

    private Activity activity;
    private Fragment fragment;
    private String provider;
    private String DIRECTORY_NAME = "MediaHelper";

    public class DefaultExtensions {
        public static final String IMAGE = ".jpeg";
        public static final String AUDIO = ".mp3";
        public static final String VIDEO = ".mp4";
        public static final String PDF = ".mp3";
        public static final String OTHER = ".other";
    }

    public MediaHelper(Activity activity) {
        this.activity = activity;
    }

    public MediaHelper(Activity activity, String provider) {
        this.activity = activity;
        this.provider = provider;
        FileUtils.AUTHORITY = provider;
    }

    public MediaHelper(Fragment fragment) {
        this.fragment = fragment;
    }

    public MediaHelper(Fragment fragment, String provider) {
        this.fragment = fragment;
        this.provider = provider;
    }

    File photoFile;

    public static abstract class Callback {
        public abstract void onResult(File file, String mimeType);

        public void onError(Exception e) {
        }
    }

    private Callback callback;

    private Context getContext() {
        return activity != null ? activity : fragment.getContext();
    }

    //=============
    //Image - start
    //=============
    private int REQUEST_FILE = 1000, REQUEST_IMAGE_CAMERA = 1001;

    public void image(Callback callback) {
        this.callback = callback;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile = null;
        try {
            requestedMimeType = MIME_TYPE_IMAGE;
            photoFile = createFile(MIME_TYPE_IMAGE, DefaultExtensions.IMAGE);
            //Old
            //Uri photoURI = Uri.fromFile(photoFile);
            //New
            Uri photoURI = FileProvider.getUriForFile(getContext(), provider, photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            if (activity != null)
                activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA);
            else if (fragment != null)
                fragment.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA);

        } catch (IOException ex) {
            if (callback != null)
                callback.onError(ex);
            ex.printStackTrace();
            return;
        }
    }

    private String requestedMimeType;

    public void file(String mimeType, Callback callback) {
        this.requestedMimeType = mimeType;
        this.callback = callback;

        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) { //(requestedMimeType != null && requestedMimeType.toLowerCase().contains("image")) || for opening gallery
            intent = new Intent(Intent.ACTION_PICK);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType(mimeType);
        if (activity != null)
            activity.startActivityForResult(Intent.createChooser(intent, "Select App"), REQUEST_FILE);
        else if (fragment != null) {
            fragment.startActivityForResult(Intent.createChooser(intent, "Select App"), REQUEST_FILE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAMERA && resultCode == RESULT_OK && photoFile != null) {
            if (callback != null)
                callback.onResult(photoFile, requestedMimeType);
        } else if (requestCode == REQUEST_FILE && resultCode == RESULT_OK) {
            if (callback != null) {
                try {
                    File file = FileUtils.getFile(getContext(), data.getData());
                    if (file != null && file.exists()) {
                        callback.onResult(file, requestedMimeType);
                    } else {
                        file = guessFile(getContext(), requestedMimeType, data.getData());
                        saveFile(data.getData(), file, new Callback() {
                            @Override
                            public void onResult(File file, String mimeType) {
                                callback.onResult(file, mimeType);
                            }

                            @Override
                            public void onError(Exception e) {
                                super.onError(e);
                                callback.onError(e);
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //=============
    private File guessFile(Context context, String defaultMimeType, Uri uri) throws IOException {
        String mimeType = FileUtils.getMimeType(context, uri);
        String extension = "";
        if (!TextUtils.isEmpty(mimeType) && FileUtils.MIME_TYPE_STREAM.equalsIgnoreCase(mimeType)) {
            requestedMimeType = mimeType;
            extension = FileUtils.getExtension(uri + "");
            if (TextUtils.isEmpty(extension) && !TextUtils.isEmpty(defaultMimeType)) {
                extension = getExtensionFromMimeType(defaultMimeType);
            }
        } else {
            requestedMimeType = defaultMimeType;
            extension = getExtensionFromMimeType(defaultMimeType);
        }

        return createFile(mimeType, extension);
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null)
            return DefaultExtensions.OTHER;

        if (requestedMimeType.toLowerCase().contains("image")) {
            return DefaultExtensions.IMAGE;
        } else if (requestedMimeType.toLowerCase().contains("audio")) {
            return DefaultExtensions.AUDIO;
        } else if (requestedMimeType.toLowerCase().contains("video")) {
            return DefaultExtensions.VIDEO;
        } else if (requestedMimeType.toLowerCase().contains("pdf")) {
            return DefaultExtensions.PDF;
        } else {
            return DefaultExtensions.OTHER;
        }
    }


    private File createFile(String mimeType, String extension) throws IOException {
        File parentStorageDir = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
        if (parentStorageDir.exists() == false) {
            parentStorageDir.mkdirs();
        }
        String folderName;

        if (mimeType != null && mimeType.toLowerCase().contains("image")) {
            folderName = "Images";
        } else if (mimeType != null && mimeType.toLowerCase().contains("audio")) {
            folderName = "Audio";
        } else if (mimeType != null && mimeType.toLowerCase().contains("video")) {
            folderName = "Video";
        } else if (mimeType != null && mimeType.toLowerCase().contains("pdf")) {
            folderName = "Pdf";
        } else {
            folderName = "Others";
        }

        File mediaStorageDir = new File(parentStorageDir.getAbsolutePath(), folderName);
        if (mediaStorageDir.exists() == false) {
            mediaStorageDir.mkdirs();
        }

        File file = File.createTempFile("File_" + System.currentTimeMillis(), extension, mediaStorageDir);
        return file;
    }

    private void saveFile(final Uri source, final File destination, final Callback callback) {

        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;

            @Override
            protected Boolean doInBackground(Void... voids) {

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = getContext().getContentResolver().openInputStream(source);
                    outputStream = new FileOutputStream(destination);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                    outputStream.close();
                    outputStream.close();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    exception = e;
                } finally {
                    if (inputStream != null) try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        exception = e;
                    }
                    if (outputStream != null) try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        exception = e;
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (callback != null) {
                    if (result)
                        callback.onResult(destination, requestedMimeType);
                    else
                        callback.onError(exception);
                }
            }
        }.execute();
    }
}
