package com.zwh.myapplication;

import android.os.Bundle;
import android.view.View;

import com.zwh.annotation.ARouter;

import androidx.appcompat.app.AppCompatActivity;

@ARouter(path = "/app/MainActivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goToLogin(View view) {
        RouterManager.getInstance()
                .build("/app/LoginActivity")
                .navigation(this);
    }
}
