/*      This file is part of Juggluco, an Android app to receive and display         */
/*      glucose values from Freestyle Libre 2(+), Libre 3(+), Dexcom G7/ONE+,        */
/*      Sibionics GS1Sb and GS3, Accu-Chek SmartGuide, CareSens Air and              */
/*      Aidex X sensors.                                                             */
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
/*      Thu Oct 15 12:44:39 CEST 2026                                                */

package tk.glucodata;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.view.Gravity;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Arrays;
import tk.glucodata.nums.AllData;
import tk.glucodata.nums.GarminAlarmConfig;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getlabel;
import static tk.glucodata.settings.Settings.editoptions;
import static tk.glucodata.help.help;
import static tk.glucodata.RingTones.EnableControls;

/** Watch preferences never write the phone's native alarm settings. */
public final class GarminAlarms {
    private static final String PREFS="garmin_watch_alarms_v1";
    private static volatile WeakReference<TextView> statusView=new WeakReference<>(null);
    private static volatile long statusPeer=Long.MIN_VALUE;

    public static GarminAlarmConfig load(Context context,long id,String address) {
        SharedPreferences p=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String idkey="id:"+id;
        String key=address==null || address.isEmpty()?idkey:"addr:"+address.toUpperCase(Locale.US);
        GarminAlarmConfig c=GarminAlarmConfig.decode(p.getString(key,null));
        if(c==null && !key.equals(idkey)) c=GarminAlarmConfig.decode(p.getString(idkey,null));
        if(c==null) c=phoneDefaults(1);
        // Mirror both aliases so discovering the BLE address or an SDK id does
        // not replace an edited configuration with the phone defaults.
        p.edit().putString(key,c.encode()).putString(idkey,c.encode()).apply();
        return c;
    }

    public static boolean save(Context context,long id,String address,GarminAlarmConfig c) {
        SharedPreferences.Editor e=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().putString("id:"+id,c.encode());
        if(address!=null && !address.isEmpty()) e.putString("addr:"+address.toUpperCase(Locale.US),c.encode());
        // Commit before reporting success or transmitting this revision.
        return e.commit();
    }

    public static GarminAlarmConfig phoneDefaults(int revision) {
        boolean mmol=Natives.getunit()==1;
        int flags=(Natives.hasalarmlow()?1:0)|(Natives.hasalarmhigh()?2:0)|(Natives.hasalarmloss()?4:0);
        int loss=Natives.readalarmsuspension(4);
        if(loss<1 || loss>4095) loss=20;
        return new GarminAlarmConfig(revision,flags,
                GarminAlarmConfig.threshold(Natives.alarmlow(),mmol),
                GarminAlarmConfig.threshold(Natives.alarmhigh(),mmol),loss,
                repeat(0),repeat(1),loss,duration(0),duration(1),duration(4),
                (Natives.alarmhassound(0)?1:0)|(Natives.alarmhassound(1)?2:0)|(Natives.alarmhassound(4)?4:0),
                GarminAlarmConfig.DEFAULT_LOW_TONE,GarminAlarmConfig.DEFAULT_HIGH_TONE,GarminAlarmConfig.DEFAULT_LOSS_TONE,
                (Natives.alarmhasvibration(0)?1:0)|(Natives.alarmhasvibration(1)?2:0)|(Natives.alarmhasvibration(4)?4:0));
    }

    private static int repeat(int type) { return Math.max(0,Math.min(4095,Natives.readalarmsuspension(type))); }
    private static int duration(int type) {
        int n=Natives.readalarmduration(type);
        return n==65535?0:Math.max(0,Math.min(65535,n));
    }

    private static EditText number(MainActivity context,String text,boolean decimal) {
        EditText e=new EditText(context);
        e.setImeOptions(editoptions);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));
//        alow.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setMinEms(2); 
       // e.setSingleLine(true); 
        e.setText(text);
        return e;
        }

    private static String shown(int threshold,boolean mmol) {
        return String.format(Locale.getDefault(),mmol?"%.1f":"%.0f",threshold/(mmol?180.0:10.0));
    }

    private static int integer(EditText e,int min,int max) {
        int v=Integer.parseInt(e.getText().toString().trim());
        if(v<min || v>max) throw new IllegalArgumentException();
        return v;
    }

    /** Refresh an open settings screen on the main thread, without polling. */
    public static void statusChanged(AllData all,long peerId) {
        TextView view=statusView.get();
        if(view!=null && statusPeer==peerId) view.post(()->{
            if(statusView.get()==view && statusPeer==peerId) view.setText(all.garminAlarmStatus(peerId));
        });
    }

    public static void show(MainActivity context,AllData all,View parent,long peerId) {
        final GarminAlarmConfig original=all.getGarminAlarms(context,peerId);
        if(original==null) return;
        final boolean mmol=Natives.getunit()==1;
        final int[] repeats={original.lowRepeat,original.highRepeat,original.lossRepeat};
        final int[] durations={original.lowDuration,original.highDuration,original.lossDuration};
        final int[] sounds={original.soundFlags};
        final int[] vibrations={original.vibrationFlags};
        final int[] tones={original.lowTone,original.highTone,original.lossTone};
        CheckDirectionBox low=new CheckDirectionBox(context),high=new CheckDirectionBox(context),loss=new CheckDirectionBox(context);
        low.setText(R.string.lowglucosealarm); high.setText(R.string.highglucosealarm); loss.setText(R.string.m3_watch_galarm_loss);
        low.setChecked((original.flags&1)!=0); high.setChecked((original.flags&2)!=0); loss.setChecked((original.flags&4)!=0);
        EditText lowValue=number(context,shown(original.low,mmol),true);
        EditText highValue=number(context,shown(original.high,mmol),true);
        EditText lossWait=number(context,Integer.toString(original.lossMinutes),false);
        Button lowVibe=getbutton(context,R.string.garmin_alarm_alert),highVibe=getbutton(context,R.string.garmin_alarm_alert),lossVibe=getbutton(context,R.string.garmin_alarm_alert);
        Button save=getbutton(context,R.string.save);
        TextView status=getlabel(context,all.garminAlarmStatus(peerId));
        statusPeer=peerId; statusView=new WeakReference<>(status);
    //    TextView note=getlabel(context,R.string.garmin_alarm_explanation);
		Button help = getbutton(context, R.string.helpname);
		help.setOnClickListener(v -> help(R.string.garmin_alarms, context));

       // note.setMaxWidth((int)(GlucoseCurve.metrics.density*420));
        //note.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        var page=new M3.Page(context,R.string.m3_watch_galarm_title,true);
        final View scroll=page.root;
        int unit=mmol?R.string.mmolL:R.string.mgdL;
        var glow=page.group(R.string.m3_watch_galarm_low);
        glow.toggle(low);
        glow.value(R.string.m3_watch_galarm_threshold,lowValue,getlabel(context,unit),lowVibe);
        var ghigh=page.group(R.string.m3_watch_galarm_high);
        ghigh.toggle(high);
        ghigh.value(R.string.m3_watch_galarm_threshold,highValue,getlabel(context,unit),highVibe);
        var gloss=page.group(R.string.lossofsignal);
        gloss.toggle(loss);
        gloss.value(R.string.m3_watch_galarm_after,lossWait,getlabel(context,R.string.minutes),lossVibe);
        var gstatus=page.group(R.string.status);
        gstatus.item(status);
        page.buttons(help,save);
        parent.setVisibility(View.GONE);
        //EnableControls(parent,false);
        page.show(()->{
            if(statusView.get()==status) { 
                statusView.clear(); 
                statusPeer=Long.MIN_VALUE; }
           // EnableControls(parent,true);
            parent.setVisibility(View.VISIBLE); 
            context.hideSystemUI(); 
            hidekeyboard(context);
        });
        low.setOnCheckedChangeListener((b,c)->{lowValue.setEnabled(c);lowVibe.setEnabled(c);});
        high.setOnCheckedChangeListener((b,c)->{highValue.setEnabled(c);highVibe.setEnabled(c);});
        loss.setOnCheckedChangeListener((b,c)->{lossWait.setEnabled(c);lossVibe.setEnabled(c);});
        lowValue.setEnabled(low.isChecked());lowVibe.setEnabled(low.isChecked());
        highValue.setEnabled(high.isChecked());highVibe.setEnabled(high.isChecked());
        lossWait.setEnabled(loss.isChecked());lossVibe.setEnabled(loss.isChecked());
        lowVibe.setOnClickListener(v->alertSettings(context,all,peerId,scroll,0,repeats,durations,sounds,tones,vibrations));
        highVibe.setOnClickListener(v->alertSettings(context,all,peerId,scroll,1,repeats,durations,sounds,tones,vibrations));
        lossVibe.setOnClickListener(v->alertSettings(context,all,peerId,scroll,2,repeats,durations,sounds,tones,vibrations));
        save.setOnClickListener(v->{
            try {
                // Preserve exact stored thresholds if the rounded unit display
                // was not edited (including after a phone unit change).
                int lo=!low.isChecked() || lowValue.getText().toString().equals(shown(original.low,mmol))?original.low:
                        GarminAlarmConfig.threshold(Double.parseDouble(lowValue.getText().toString().trim().replace(',','.')),mmol);
                int hi=!high.isChecked() || highValue.getText().toString().equals(shown(original.high,mmol))?original.high:
                        GarminAlarmConfig.threshold(Double.parseDouble(highValue.getText().toString().trim().replace(',','.')),mmol);
                GarminAlarmConfig c=new GarminAlarmConfig(original.revision,
                        (low.isChecked()?1:0)|(high.isChecked()?2:0)|(loss.isChecked()?4:0),lo,hi,
                        loss.isChecked()?integer(lossWait,1,4095):original.lossMinutes,
                        repeats[0],repeats[1],repeats[2],durations[0],durations[1],durations[2],
                        sounds[0],tones[0],tones[1],tones[2],vibrations[0]);
                if(!all.setGarminAlarms(context,peerId,c)) { status.setText(R.string.garmin_alarm_save_failed); return; }
                status.setText(all.garminAlarmStatus(peerId));
                hidekeyboard(context);
            } catch(IllegalArgumentException e) { status.setText(R.string.garmin_alarm_invalid); }
        });
    }

    private static void alertSettings(MainActivity context,AllData all,long peerId,View parent,
            int kind,int[] repeats,int[] durations,int[] sounds,int[] tones,int[] vibrations) {
        EditText repeat=number(context,Integer.toString(repeats[kind]),false);
        EditText duration=number(context,Integer.toString(durations[kind]),false);
        int support=all.garminAlarmSoundSupport(peerId);
        boolean canEditSound=support==-1 || support==2;
        CheckDirectionBox sound=new CheckDirectionBox(context);
        sound.setText(R.string.soundname);
        sound.setChecked((sounds[0]&(1<<kind))!=0);
        sound.setEnabled(canEditSound);
        int vibrationSupport=all.garminAlarmVibrationSupport(peerId);
        boolean canEditVibration=vibrationSupport==-1 || vibrationSupport==2;
        CheckDirectionBox vibration=new CheckDirectionBox(context);
        vibration.setText(R.string.vibrationname);
        vibration.setChecked((vibrations[0]&(1<<kind))!=0);
        vibration.setEnabled(canEditVibration);
        Spinner tone=tk.glucodata.settings.Settings.getGenSpin(context);
        String[] toneNames=context.getResources().getStringArray(R.array.garmin_alarm_tones);
        tone.setAdapter(new RangeAdapter<String>(Arrays.asList(toneNames),context,name->name));
        tone.setSelection(tones[kind]);
        tone.setEnabled(canEditSound && sound.isChecked());
        sound.setOnCheckedChangeListener((button,checked)->tone.setEnabled(canEditSound && checked));
        /*
        TextView soundNote=getlabel(context,support==0?R.string.garmin_alarm_sound_update:
                support==1?R.string.garmin_alarm_sound_unavailable:
                support==2?R.string.garmin_alarm_sound_supported:R.string.garmin_alarm_sound_unknown);
        soundNote.setMaxWidth((int)(GlucoseCurve.metrics.density*320));
        TextView vibrationNote=getlabel(context,vibrationSupport==0?R.string.garmin_alarm_outputs_update:
                vibrationSupport==1?R.string.garmin_alarm_vibration_unavailable:
                vibrationSupport==2?R.string.garmin_alarm_vibration_supported:R.string.garmin_alarm_vibration_unknown);
        vibrationNote.setMaxWidth((int)(GlucoseCurve.metrics.density*320));
        TextView repeatNote=getlabel(context,kind==2?R.string.garmin_alarm_zero_loss:R.string.garmin_alarm_zero_glucose);
        repeatNote.setMaxWidth((int)(GlucoseCurve.metrics.density*320));
        */
		Button help = getbutton(context, R.string.helpname);
		help.setOnClickListener(v -> help(R.string.garmin_alert, context));
        Button save=getbutton(context,R.string.save);
        var page=new M3.Page(context,context.getString(new int[]{R.string.lowglucosealarm,R.string.highglucosealarm,R.string.lossofsignal}[kind]),true);
        var galert=page.group(R.string.garmin_alarm_alert);
        galert.value(context.getString(R.string.duraction).replace(":","").trim(),duration,getlabel(context,R.string.sec));
        galert.value(context.getString(R.string.minuteddeactivated).replace(":","").trim(),repeat);
        galert.toggle(sound);
        galert.value(R.string.garmin_alarm_tone,tone);
        galert.toggle(vibration);
        TextView error=page.note("");
        page.buttons(help,save);

        EnableControls(parent,false);
        page.show(()->{
            EnableControls(parent,true);
           // parent.setVisibility(View.VISIBLE);
            hidekeyboard(context);
            context.hideSystemUI();
            });
        save.setOnClickListener(v->{try {
            int r=integer(repeat,0,4095),d=integer(duration,0,65535);
            repeats[kind]=r;durations[kind]=d;
            if(canEditSound) {
                sounds[0]=sound.isChecked()?sounds[0]|(1<<kind):sounds[0]&~(1<<kind);
                tones[kind]=tone.getSelectedItemPosition();
            }
            if(canEditVibration) {
                vibrations[0]=vibration.isChecked()?vibrations[0]|(1<<kind):vibrations[0]&~(1<<kind);
            }
            context.doonback();
        }catch(IllegalArgumentException e){error.setText(R.string.garmin_alarm_invalid);}});
    }

    private static void hidekeyboard(MainActivity context) { tk.glucodata.help.hidekeyboard(context); }
}
