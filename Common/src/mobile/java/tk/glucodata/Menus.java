/*      This file is part of Juggluco, an Android app to receive and display         */
/*      glucose values from Freestyle Libre 2 and 3 sensors.                         */
/*                                                                                   */
/*      Copyright (C) 2021 Jaap Korthals Altes <jaapkorthalsaltes@gmail.com>         */
/*                                                                                   */
/*      Juggluco is free software: you can redistribute it and/or modify             */
/*      it under the terms of the GNU General Public License as published            */
/*      by the Free Software Foundation, either version 3 of the License, or         */
/*      (at your option) any later version.                                          */
/*                                                                                   */
/*      Juggluco is distributed in the hope that it will be useful, but              */
/*      WITHOUT ANY WARRANTY; without even the implied warranty of                   */
/*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         */
/*      See the GNU General Public License for more details.                         */
/*                                                                                   */
/*      You should have received a copy of the GNU General Public License            */
/*      along with Juggluco. If not, see <https://www.gnu.org/licenses/>.            */
/*                                                                                   */
/*      Sun Oct 08 20:48:20 CEST 2023                                                */





package tk.glucodata;

import android.content.res.Configuration;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;


import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MainActivity.REQUEST_BARCODE;
import static tk.glucodata.Natives.getInvertColors;
import static tk.glucodata.NumberView.smallScreen;
import static tk.glucodata.settings.Settings.removeContentView;


public class Menus {
static public boolean on=false;
static private boolean waitingForLandscape=false;
static private View currentView=null;
static private MainActivity owner=null;
static private View waitingDecor=null;
static private ViewTreeObserver.OnPreDrawListener waitingPreDraw=null;
static private int stableWidth=-1,stableHeight=-1,stableRotation=-1;
static private int stableLeft=-1,stableTop=-1,stableRight=-1,stableBottom=-1;
static private int stableFrames=0;
static private final String LOG_ID="Menus";

private static void resetStableWindow() {
    stableWidth=stableHeight=stableRotation=-1;
    stableLeft=stableTop=stableRight=stableBottom=-1;
    stableFrames=0;
    }

private static void cancelWindowWait() {
    if(waitingDecor!=null && waitingPreDraw!=null) {
        ViewTreeObserver obs=waitingDecor.getViewTreeObserver();
        if(obs.isAlive())
            obs.removeOnPreDrawListener(waitingPreDraw);
        }
    waitingDecor=null;
    waitingPreDraw=null;
    resetStableWindow();
    }

static void newTask() {
    cancelWindowWait();
    on=false;
    waitingForLandscape=false;
    currentView=null;
    owner=null;
    }

private static boolean stableMenuWindow(MainActivity act,View decor) {
    if(owner!=act || !on || act.isHandlingConfigurationChange()) {
        resetStableWindow();
        return false;
        }
    Configuration config=act.getResources().getConfiguration();
    int width=decor.getWidth();
    int height=decor.getHeight();
    final boolean requireLandscape=act.canForceMenuLandscape(config);
    if(width<=0 || height<=0 ||
       (requireLandscape &&
        (config.orientation!=Configuration.ORIENTATION_LANDSCAPE || width<=height))) {
        resetStableWindow();
        return false;
        }

    WindowInsetsCompat wi=ViewCompat.getRootWindowInsets(decor);
    if(wi==null) {
        resetStableWindow();
        return false;
        }
    Insets bars=wi.getInsets(WindowInsetsCompat.Type.systemBars());
    int rotation=act.getWindowManager().getDefaultDisplay().getRotation();

    if(width==stableWidth && height==stableHeight && rotation==stableRotation &&
       bars.left==stableLeft && bars.top==stableTop &&
       bars.right==stableRight && bars.bottom==stableBottom) {
        ++stableFrames;
        }
    else {
        stableWidth=width;
        stableHeight=height;
        stableRotation=rotation;
        stableLeft=bars.left;
        stableTop=bars.top;
        stableRight=bars.right;
        stableBottom=bars.bottom;
        stableFrames=1;
        }
    return stableFrames>=2;
    }

private static void waitForLandscapeWindow(MainActivity act) {
    waitingForLandscape=true;
    View decor=act.getWindow().getDecorView();
    if(waitingDecor==decor && waitingPreDraw!=null)
        return;

    cancelWindowWait();
    waitingDecor=decor;
    waitingPreDraw=new ViewTreeObserver.OnPreDrawListener() {
        @Override public boolean onPreDraw() {
            if(owner!=act || !on || currentView!=null && currentView.isAttachedToWindow()) {
                cancelWindowWait();
                return true;
                }
            Configuration config=act.getResources().getConfiguration();
            if(act.canForceMenuLandscape(config) &&
               config.orientation!=Configuration.ORIENTATION_LANDSCAPE)
                act.setMenuForcesLandscape(true);

            if(stableMenuWindow(act,decor)) {
                /* Remove the observer before adding the overlay.  Two equal
                 * pre-draws prove that the real window geometry and insets
                 * have reached a stable landscape state. */
                ViewTreeObserver obs=decor.getViewTreeObserver();
                if(obs.isAlive())
                    obs.removeOnPreDrawListener(this);
                waitingDecor=null;
                waitingPreDraw=null;
                waitingForLandscape=false;
                decor.post(() -> {
                    if(owner==act && on && !act.isHandlingConfigurationChange() &&
                       (currentView==null || !currentView.isAttachedToWindow()))
                        showNow(act);
                    });
                }
            else if(stableFrames>0) {
                /* A static screen is not guaranteed to get another frame.
                 * Explicitly schedule the second pre-draw needed by the
                 * stability test.  If geometry changes on that frame,
                 * stableLandscapeWindow() resets the candidate and another
                 * verification frame is scheduled. */
                decor.postOnAnimation(decor::invalidate);
                }
            return true;
            }
        };
    decor.getViewTreeObserver().addOnPreDrawListener(waitingPreDraw);
    ViewCompat.requestApplyInsets(decor);
    decor.invalidate();
    }

static void configurationChanged(MainActivity act, Configuration config) {
    /* Menus.on also remains true while a child screen is on top. Only a menu
     * explicitly waiting for its own reconstruction may take orientation
     * ownership here. */
    if(isWearable || owner!=act || !on || !waitingForLandscape)
        return;
    if(act.canForceMenuLandscape(config) &&
       config.orientation!=Configuration.ORIENTATION_LANDSCAPE)
        act.setMenuForcesLandscape(true);
    waitForLandscapeWindow(act);
    }

static public void show(MainActivity act) {
    on=true;

    if(owner!=act) {
        cancelWindowWait();
        owner=act;
        currentView=null;
        }

    if(!isWearable) {
        act.setMenuForcesLandscape(true);
        if(currentView!=null && currentView.isAttachedToWindow()) {
            waitingForLandscape=false;
            return;
            }
        waitForLandscapeWindow(act);
        return;
        }
    showNow(act);
    }

static private void showNow(MainActivity act) {
    waitingForLandscape=false;
    resetStableWindow();
    var page=new M3.Page(act,R.string.connections,false);
    View view = page.root;
    currentView=view;
    var devices=page.group(R.string.m3_conn_devices);
    var sensorrow=devices.nav(R.string.sensor,act.getString(R.string.m3_conn_sensor_sum),null);
    var mirrorrow=devices.nav(R.string.mirror,act.getString(R.string.m3_conn_mirror_sum),null);
    var watchrow=devices.nav(R.string.watches,act.getString(R.string.m3_conn_watch_sum),null);

    if(!isWearable) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) { }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if(currentView==v)
                    currentView=null;

                /* Ignore callbacks belonging to an Activity that has already
                 * been replaced by another MainActivity instance. */
                if(owner!=act)
                    return;

                /*
                 * removeconfig() deliberately removes overlays. That is not
                 * the same as leaving Menus: keep ownership and rebuild after
                 * the new geometry has settled.
                 */
                if(act.isHandlingConfigurationChange()) {
                    if(on)
                        waitingForLandscape=true;
                    return;
                    }

                /*
                 * Ordinary detach means either Menus was closed or a child
                 * screen was opened. The child screen should follow the normal
                 * orientation policy until Menus.show() is called on return.
                 */
                act.setMenuForcesLandscape(false);
                }
            });
        }
      act.themeLightBars();
	act.setonback(() -> {
        if(owner!=act) {
            removeContentView(view);
            return;
            }
        if(act.isHandlingConfigurationChange()) {
            /*
             * doonback() is also used by removeconfig().  Preserve the fact
             * that Menus is logically open and recreate it after geometry has
             * settled instead of treating this as a real Back press.
             */
            waitingForLandscape=true;
            removeContentView(view);
            return;
            }
   		act.lightBars(!getInvertColors( ));
			   {if(doLog) {Log.i(LOG_ID,"onback");};};
			   on=false;
            waitingForLandscape=false;
            cancelWindowWait();
			removeContentView(view);
				act.requestRender();
			});

        watchrow.setOnClickListener(v ->{
				if(!isWearable) {
				act.poponback();
   		   act.lightBars(!getInvertColors( ));
					removeContentView(view);
					tk.glucodata.Watch.show(act);
					}
	}); 
        sensorrow.setOnClickListener(v ->{
				act.poponback();
   		   act.lightBars(!getInvertColors( ));
				removeContentView(view);
			       bluediag.start(act);
		}); 
        mirrorrow.setOnClickListener(v ->{
				act.poponback();
		     removeContentView(view);
			(new Backup()).mkbackupview(act);
	}); 
   
	act.addMyContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    if(!isWearable)
        view.requestApplyInsets();

    }


};



