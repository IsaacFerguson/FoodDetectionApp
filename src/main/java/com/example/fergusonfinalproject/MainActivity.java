package com.example.fergusonfinalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//image classification happens on scanPage

public class MainActivity extends AppCompatActivity{

    private Button scan, viewList;
    List<String> ingredients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //connect buttons
        scan = findViewById(R.id.picture);

        //opens page to the scanning page
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openScanPage();
            }
        });



    }

    private void openListPage() {
        Intent intent = new Intent(this,listPage.class);
        startActivity(intent);
    }


    private void openScanPage() {
        Intent intent = new Intent(this,scanPage.class);
        startActivity(intent);
    }
}