package com.example.fergusonfinalproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.fergusonfinalproject.ml.ModelFood10items;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class scanPage extends AppCompatActivity{

    public static final int CameraPermission = 1;
    public static final int cam_request = 2;
    //tensorFlow models uses 224 x 224 image
    int imageSize = 224;
    ImageView imageView;
    Button photo,list, home;
    TextView result;

    ArrayList<String> ingredients = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_page);

        //setting up interactive tools
        imageView = findViewById(R.id.displayPicture);
        photo = findViewById(R.id.camButton);
        result = findViewById(R.id.result);
        home = findViewById(R.id.homepage);


        //button listener for taking photo
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //asks for camera permissions
                cameraPermission();
            }
        });


        //button listener for going to view page
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                returnHome();

            }
        });
    }

    private void viewListPage() {
        //sends the list of ingredients to other activity
        Intent saveList = new Intent(this,listPage.class);
        saveList.putExtra("ingList", ingredients);
        startActivity(saveList);

        Intent intent = new Intent(this,listPage.class);
        startActivity(intent);


    }

    private void returnHome() {
        //sends the list of ingredients to other activity
        Intent saveList = new Intent(this,listPage.class);
        saveList.putExtra("ingList", ingredients);
        startActivity(saveList);

        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);

    }

    //checks if the user has already give camera permission and if not requests it
    private void cameraPermission() {
        //checks and asks for camera permission, if it already has it opens camera
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},CameraPermission);
        } else{
            openCamera();
        }
    }




    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //checks users answer to camera access, if given opens camera, if not sends message to user
        if (requestCode == CameraPermission) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //use intent to open the camera
    private void openCamera() {
        //uses intent to take the picture
        //originally was going to use CameraX but i had issues converting the taken image to a bitmap
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, cam_request);
    }

    //handles the photo that is taken
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == cam_request){
            //converts the image taken into a bitmap to be read by tensorFlow
            Bitmap pic = (Bitmap) data.getExtras().get("data");



            //centers the image to allow easier use for tensorFlow
            int dimension = Math.min(pic.getWidth(), pic.getHeight());
            pic = ThumbnailUtils.extractThumbnail(pic, dimension, dimension);
            pic = Bitmap.createScaledBitmap(pic, imageSize, imageSize, false);

            //displays the image taken back to the user
            imageView.setImageBitmap(pic);

            imageClassify(pic);

        }
    }

    public void imageClassify(Bitmap image) {
        try {
            ModelFood10items model = ModelFood10items.newInstance(getApplicationContext());

            //sets the inputs for tensorFlow
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            //create an array of the proper size for tensorFlow and get each pixel from the image
            int[] intVal = new int[imageSize * imageSize];
            image.getPixels(intVal, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            //changes the pixels to their RGB values
            //image classification models are required to have all of the RGB values to work
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intVal[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            //loads the modified bytebuffer
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model based on image that was captured
            ModelFood10items.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


            float[] confidences = outputFeature0.getFloatArray();

            //find the model with the highest confidence and notes its location
            int pos = 0;
            float maxConf = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConf) {
                    maxConf = confidences[i];
                    pos = i;
                }
            }

            //returns the name of model with the highest confidence
            String[] highConf = {"Apple", "Avocado", "Banana", "Orange", "Pineapple","Carrot", "Leek", "Potato", "Tomato"};
            result.setText(highConf[pos]);

            //saves the results of each image captured
            ingredients.add(highConf[pos]);
            Toast.makeText(this, highConf[pos] + " was added to your list", Toast.LENGTH_SHORT).show();



            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            //should not error but print if does
            e.printStackTrace();
        }
    }
}