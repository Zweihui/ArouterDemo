package com.zwh.myapplication;

import android.os.Bundle;
import android.view.View;

import com.zwh.annotation.ARouter;

import androidx.appcompat.app.AppCompatActivity;

@ARouter(path = "/app/LoginActivity")
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void goToPersonal(View view) {
        RouterManager.getInstance()
                .build("/app/UserActivity")
                .navigation(this);
    }
}
