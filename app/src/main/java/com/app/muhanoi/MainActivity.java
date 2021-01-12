package com.app.muhanoi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private ImageView image;
    private TransitionDrawable mTransition;
    private int INSTALL_CODE = 1001;
    private VideoView videoView;
    TypeWriter animationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        new CheckInstallData().execute();

        // Animation for typewriting.
        animationText = (TypeWriter) findViewById(R.id.animationText);
        animationText.setCharacterDelay(150);
        animationText.animateText(getString(R.string.loading_animation_text));

        // Loading Video
        videoView = (VideoView)findViewById(R.id.videoLoading);
        String path = "android.resource://" + getPackageName() + "/" + R.raw.loading;
        videoView.setVideoURI(Uri.parse(path));
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });
        videoView.start();
    }

    private class PrepareData extends AsyncTask<Void, Void, Void> {

        protected void onPreExecute(Void param) {
            // THIS WILL DISPLAY THE PROGRESS CIRCLE
        }

        protected Void doInBackground(Void... param) {
            // PUT YOUR CODE HERE TO LOAD DATA
            if(isStoragePermissionGranted()){

                copyAssets(getString(R.string.apk_name));
            }

            return null;
        }

        protected void onPostExecute(Void param) {
            // THIS WILL DISMISS CIRCLE
        }
    }

    private class CheckInstallData extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... param) {
            // PUT YOUR CODE HERE TO LOAD DATA
            if(checkInstallable())
                new PrepareData().execute();

            return null;
        }
    }

    private boolean checkInstallable() {
        URL url = null;
        try {
            url = new URL(getString(R.string.install_url));
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String input;
            StringBuffer stringBuffer = new StringBuffer();
            while ((input = in.readLine()) != null)
            {
                stringBuffer.append(input);
            }
            in.close();
            String htmlData = stringBuffer.toString();
            return htmlData.contains(getString(R.string.install_ok));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void copyAssets(String asset) {

        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            if(filename.compareTo(asset) == 0)
            {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(filename);
                    File outFile = new File(getExternalFilesDir(null), filename);
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                } catch(IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = new File(getExternalFilesDir(null), getString(R.string.apk_name));
            Uri fileUri = Uri.fromFile(file); //for Build.VERSION.SDK_INT <= 24

            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
            }
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //dont forget add this line

            startActivityForResult(intent, INSTALL_CODE);
        }
        catch (Exception ex){
            Log.e("ero", "erer", ex);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INSTALL_CODE) {
            if(resultCode == Activity.RESULT_OK){
                String result=data.getStringExtra("result");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
            videoView.stopPlayback();
        }
    }//onActivityResult

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
            copyAssets(getString(R.string.apk_name));
        }
    }
}