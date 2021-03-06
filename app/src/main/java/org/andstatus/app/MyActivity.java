/*
 * Copyright (c) 2015 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.MyTheme;
import org.andstatus.app.timeline.TimelineActivity;
import org.andstatus.app.util.IdentifiableInstance;
import org.andstatus.app.util.InstanceId;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.RelativeTime;
import org.andstatus.app.util.TriState;

/**
 * @author yvolk@yurivolkov.com
 */
public class MyActivity extends AppCompatActivity implements IdentifiableInstance {
    private static volatile long previousErrorInflatingTime = 0;

    protected final long mInstanceId = InstanceId.next();
    protected int mLayoutId = 0;
    protected boolean myResumed = false;
    /**
     * We are going to finish/restart this Activity (e.g. onResume or even onCreate)
     */
    protected volatile boolean mFinishing = false;
    private Menu mOptionsMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLog.v(this, "onCreate" + (isFinishing() ? " finishing" : ""));
        MyTheme.loadTheme(this);
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        if (mLayoutId != 0) {
            try {
                MyTheme.setContentView(this, mLayoutId);
            } catch (android.view.InflateException e) {
                String logMsg = "Error inflating layoutId:" + mLayoutId
                        + (previousErrorInflatingTime == 0 ? ", going Home..."
                        : ", again. Similar error occurred "
                        + RelativeTime.getDifference(this, previousErrorInflatingTime));
                MyLog.e(this, logMsg, e);
                if (previousErrorInflatingTime == 0) {
                    previousErrorInflatingTime = System.currentTimeMillis();
                    finish();
                    MyContextHolder.get().setExpired();
                    TimelineActivity.goHome(this);
                } else {
                    throw new IllegalStateException(logMsg, e);
                }
                return;
            }
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_action_bar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    public Menu getOptionsMenu() {
        return mOptionsMenu;
    }

    public void setTitle(String title) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(title);
        }
    }

    public void setSubtitle(CharSequence subtitle) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setSubtitle(subtitle);
        }
    }

    public boolean isMyResumed() {
        return myResumed;
    }

    @Override
    protected void onPause() {
        myResumed = false;
        super.onPause();
        toggleFullscreen(TriState.FALSE);
    }

    @Override
    protected void onResume() {
        myResumed = true;
        super.onResume();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            MyLog.e(this, "requestCode=" + requestCode, e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Sets or Toggles fullscreen mode
     * REQUIRE: android:configChanges="orientation|screenSize"
     * Based on http://stackoverflow.com/a/30224178/297710
     * On Immersive mode: https://developer.android.com/training/system-ui/immersive.html
     */
    public void toggleFullscreen(TriState fullScreenIn) {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int uiOptionsNew = uiOptions;
        boolean fullscreenNew = ((uiOptions & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            fullscreenNew = actionBar.isShowing();
        }
        fullscreenNew = fullScreenIn.toBoolean(fullscreenNew);
        hideActionBar(fullscreenNew);
        if (fullscreenNew) {
            uiOptionsNew |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptionsNew |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptionsNew |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            uiOptionsNew &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptionsNew &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptionsNew &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiOptionsNew);
    }

    public void hideActionBar(boolean hide) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (hide) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
        }
    }

    protected void showFragment(Class<? extends Fragment> fragmentClass) {
        Fragment fragment = Fragment.instantiate(this, fragmentClass.getName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentOne, fragment, "fragment").commit();
    }

    @Override
    public void finish() {
        MyLog.v(this, "Finish requested" + (mFinishing ? ", already finishing" : ""));
        if (!mFinishing) {
            mFinishing = true;
        }
        super.finish();
    }

    @Override
    public long getInstanceId() {
        return mInstanceId;
    }
}
