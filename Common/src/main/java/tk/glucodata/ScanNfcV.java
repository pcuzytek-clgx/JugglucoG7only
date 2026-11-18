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
/*      Fri Jan 27 15:31:05 CET 2023                                                 */


package tk.glucodata;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.nfc.Tag;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.CalendarContract;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import tk.glucodata.settings.Settings;

import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Applic.mgdLmult;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.Log.showbytes;
import static tk.glucodata.MainActivity.systembarBottom;
import static tk.glucodata.MainActivity.systembarLeft;
import static tk.glucodata.MainActivity.systembarRight;
import static tk.glucodata.MainActivity.systembarTop;
import static tk.glucodata.settings.Settings.removeContentView;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getlabel;

public class ScanNfcV   {
    private static final String LOG_ID = "ScanNfcV";



    @SuppressWarnings("deprecation")
public static void failure(Vibrator vibrator) {
    final long[] vibrationPatternFailure = {0, 500}; // [ms]
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternFailure, -1));
    }




static     AudioAttributes audioattributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM) .build();
static    VibrationAttributes vibrationattributes=new VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM) .build();
static void vibrates(Vibrator vibrator,final long[] vibrationPatternstart ,final int[]  amplitude) {
    vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternstart,amplitude, 1),vibrationattributes);
    }
    @SuppressWarnings("deprecation")
public static Vibrator getvibrator(Context context) {
    return ((VibratorManager)context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();

    }
public static void startvibration(Vibrator vibrator) {
        final long[] vibrationPatternstart = {0, 70, 50,50,50,50,50};
        final int[] amplitude={0,  255,150,0,255,50,0};
        vibrates(vibrator,vibrationPatternstart,amplitude); 
    }
static private void newsensor(MainActivity act,String text,String name) {

  /*  var metrics= act.getResources().getDisplayMetrics();
    int width= metrics.widthPixels;
    int height=metrics.heightPixels; */
    int widthin=GlucoseCurve.getwidth();
    int heightin=GlucoseCurve.getheight();
    if(heightin>widthin) {
        Log.i(LOG_ID,"newsensor exchange height-width");
        int tmp=widthin;
        widthin=heightin;
        heightin=tmp;
        }
    final int height=heightin;
    final int width=widthin;
    int pad=width/30;
    {if(doLog) {Log.i(LOG_ID,"newsensor "+name);};};
    act.runOnUiThread(() -> {
       TextView nameview=getlabel(act,name);
       nameview.setTextSize(TypedValue.COMPLEX_UNIT_PX, Applic.largefontsize);
       TextView tv=getlabel(act,text);
       if(!isWearable)
               tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,Applic.largefontsize);

        CheckDirectionBox calBox = new CheckDirectionBox(act);
        calBox.setPadding(0,pad,0,pad);
        long timedata=Natives.getSensorEndData(name);
        final var endtime=   (timedata&0xFFFFFFFFL)*1000L;
        calBox.setChecked((timedata>>32)!=0L);
        calBox.setText(R.string.addsensorenddate);
        final boolean  stillused=endtime>System.currentTimeMillis();
        if(!stillused)
            calBox.setVisibility(GONE);

        Button ok=getbutton(act,R.string.ok);

        Layout lay=new Layout(act, (l, w, h) -> {
        /*
           int wid=GlucoseCurve.getwidth()- systembarRight-systembarLeft;
            if(wid>w)
                l.setX((wid-w)*.5f+systembarLeft);
            var hei=height-systembarBottom;
            if(hei>h) {
                l.setY((hei-h)*.5f);
                }
*/
                return new int[] {w,h};
            },new View[]{nameview},new View[]{tv},new View[]{calBox},new View[]{ok});
            ok.setOnClickListener(v->{
               removeContentView(lay); 
               if(stillused&&calBox.isChecked()) {
                    insertcalendar(act,name,endtime) ;
                    }
               else
                   askcalendar=false;
                });
        lay.setPadding(pad,pad,pad,pad);
        var bg=new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(M3.dp(act,28));
        bg.setColor(M3.color(act,com.google.android.material.R.attr.colorSurfaceContainerHigh,0xFF2B2B2B));
        lay.setBackground(bg);
        M3.style(ok);
       var params = new FrameLayout.LayoutParams( WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL| Gravity.CENTER);
        act.addMyContentView(lay, params);
    });
    }
static boolean askcalendar=true;
static int calendar(MainActivity act,int ret,String name) {
    if(askcalendar)  {
        int waitmin=(ret&0xff)==5?ret>>8:0;
        String mess=(waitmin>0) ?
(act.getString(R.string.sensor)+" "+name+act.getString(R.string.ready_in)+waitmin+" "+act.getString(R.string.minutes)) :act.getString(R.string.ready_for_use);
        newsensor(act,mess,name);
        return 0;
        }
    else
        return ret;
    }
private static void insertcalendar(MainActivity act,String name,long endtime) {
/*
    long endtime=Natives.sensorends()*1000L;
    if(endtime<= System.currentTimeMillis())
        return; */

    try {
        Intent intent = new Intent(Intent.ACTION_INSERT)
        .putExtra(CalendarContract.Events.TITLE, act.getString(R.string.enddatesensor)+name)
        .putExtra(CalendarContract.Events.DESCRIPTION, act.getString(R.string.sensor)+" "+name+act.getString(R.string.endstime) )
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, endtime)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endtime+1000L);
        {if(doLog) {Log.i(LOG_ID,"start calendar app");};};
        act.startActivity(intent);
        askcalendar=false;
        } 
    catch(Throwable error) {
            String mess=error.getMessage();
            if(mess==null) mess="Exception";
            Log.stack(LOG_ID,mess,error);
            Toast.makeText(act,mess, Toast.LENGTH_LONG).show();
            }
    }
}
