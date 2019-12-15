package com.example.smarttrashbin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.james.mime4j.field.ContentTypeField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button cameraBtn;
    private ImageView imageView;
    //edit for uploading
    private Button uploadBtn;
    private TextView textView;

    private Button requestBtn;

    private EditText positionText;

    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_PERMISSION = 200;
    private String imageFilePath = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBtn = findViewById(R.id.cameraBtn);
        imageView = findViewById(R.id.imagePreview);
        //edit for uploading
        uploadBtn = findViewById(R.id.uploadBtn);
        textView = findViewById(R.id.trashType);
        requestBtn = findViewById(R.id.requestBtn);
        positionText = findViewById(R.id.positionText);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraIntent();
            }
        });

        //edit for uploading
        uploadBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                uploadImageIntent();
            }
        });

        requestBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                callTrashBinIntent();
            }
        });

    }
    //click camera -> create a photo file, then store snapshot into it
    private void openCameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() +".provider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(pictureIntent, REQUEST_IMAGE);
        }
    }

    private void uploadImageIntent(){
        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
//        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                    HttpPost httppost = new HttpPost("http://6155cfaa.ngrok.io/recommend");
//                    HttpPost httppost = new HttpPost("http://2fa10552.ngrok.io/recommend");
                    File file = new File(imageFilePath);
                    MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//        ContentBody cbFile = new FileBody(file, "image/jpeg");
                    Log.e("print","below are compressing");
                    Bitmap original = BitmapFactory.decodeFile(file.getPath());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    original.compress(Bitmap.CompressFormat.JPEG, 50, out);
//                    Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
                    File f = null;
                    f = createCompressedImageFile();
                    f.createNewFile();
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(out.toByteArray());
                    fos.flush();
                    fos.close();

                    ContentBody cbFile = new FileBody(f);
                    mpEntity.addPart("Filename", new StringBody(f.getName()));
                    mpEntity.addPart("Filedata", cbFile);


                    httppost.setEntity(mpEntity);
                    Log.e("step", "executing request " + httppost.getRequestLine());
                    HttpResponse response = httpclient.execute(httppost);
                    HttpEntity resEntity = response.getEntity();

                    Log.e("step", response.getStatusLine().toString());
                    if (resEntity != null) {
                        final String result_str = EntityUtils.toString(resEntity);
                        Log.e("step-result", result_str);
//
//                        runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//
//                                // Stuff that updates the UI
//                                textView.setText(result_str);
//                                final String trash_type = result_str.split(" ")[0];
//                                Thread thread = new Thread(new Runnable(){
//                                    public void run() {
//                                        try {
//                                            HttpClient httpclient = new DefaultHttpClient();
//                                            URI uri = new URI("http://255f8c04.ngrok.io?COMMAND=type:" + trash_type);
//                                            HttpGet httpget = new HttpGet(uri);
//                                            HttpResponse response = httpclient.execute(httpget);
//                                        }
//                                        catch (Exception e){
//                                            e.printStackTrace();
//                                        }
//                                    }});
//                                thread.start();
//                            }
//                        });
                    }
                    if (resEntity != null) {
                        resEntity.consumeContent();
                    }

                    httpclient.getConnectionManager().shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("error",imageFilePath);
                }
            }
        });

        thread.start();
    }

    private void callTrashBinIntent(){
        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {
                    String position_str = positionText.getText().toString();
                    if (position_str != null) {
                        HttpClient httpclient = new DefaultHttpClient();
                        URI uri = new URI("http://255f8c04.ngrok.io/acceptCommand?Command=action:" + position_str);
                        HttpGet httpget = new HttpGet(uri);
                        HttpResponse response = httpclient.execute(httpget);
                    }
                    else{

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }});
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting Permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                imageView.setImageURI(Uri.parse(imageFilePath));
            }
            else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "You cancelled the operation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFilePath = image.getAbsolutePath();
        Log.e("print",imageFilePath);
        return image;
    }

    private File createCompressedImageFile() throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFilePath = image.getAbsolutePath();
        Log.e("print",imageFilePath);
        return image;
    }

}
