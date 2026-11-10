package com.example.retaildpc;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/** Explicit user tap on the approval notification opens Android's installer UI. */
public final class PostDemoApprovalActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent system=Build.VERSION.SDK_INT>=33
            ?getIntent().getParcelableExtra("system_approval",Intent.class)
            :(Intent)getIntent().getParcelableExtra("system_approval");
        if(system!=null) {
            try {startActivity(system);}
            catch(RuntimeException ex){
                State.log(this,"PostDemo nie otwarto instalatora: "+ex.getClass().getSimpleName());
            }
        }
        finish();
    }
}
