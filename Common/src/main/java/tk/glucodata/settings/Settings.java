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
/*      Fri Jan 27 15:31:32 CET 2023                                                 */


package tk.glucodata.settings;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.graphics.Color.BLACK;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.Spinner.MODE_DIALOG;
import static android.widget.Spinner.MODE_DROPDOWN;
import static tk.glucodata.Applic.DynamicTheme;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Applic.usedlocale;
import static tk.glucodata.Backup.getnumedit;
import static tk.glucodata.Layout.getMargins;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MainActivity.canRotate;
import static tk.glucodata.Natives.getInvertColors;
import static tk.glucodata.Natives.getRTL;
import static tk.glucodata.Natives.getScheduleProfile;
import static tk.glucodata.Natives.getalarmSoundType;
import static tk.glucodata.Natives.getshowcalibratedstream;
import static tk.glucodata.Natives.getshowhistories;
import static tk.glucodata.Natives.getshownumbers;
import static tk.glucodata.Natives.getshowscans;
import static tk.glucodata.Natives.getshowstream;
import static tk.glucodata.Natives.removeScheduleProfile;
import static tk.glucodata.Natives.setthreshold;
import static tk.glucodata.NumberView.avoidSpinnerDropdownFocus;
import static tk.glucodata.RingTones.EnableControls;
import static tk.glucodata.Specific.useclose;
import static tk.glucodata.help.help;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getcheckbox;
import static tk.glucodata.util.getlabel;
import static tk.glucodata.util.getradiobuttonId;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Html;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.google.android.material.slider.LabelFormatter;
//import com.google.android.material.slider.RangeSlider;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;

import tk.glucodata.Applic;
import tk.glucodata.Backup;
import tk.glucodata.BooleanSupplier;
import tk.glucodata.BuildConfig;
import tk.glucodata.CheckDirectionBox;
import tk.glucodata.CheckDirectionRadio;
import tk.glucodata.GlucoseCurve;
import tk.glucodata.HealthConnection;
import tk.glucodata.LabelAdapter;
import tk.glucodata.Layout;
import tk.glucodata.Log;
import tk.glucodata.M3;
import tk.glucodata.MainActivity;
import tk.glucodata.Menus;
import tk.glucodata.Natives;
import tk.glucodata.Notify;
import tk.glucodata.NumAlarm;
import tk.glucodata.R;
import tk.glucodata.Specific;
import tk.glucodata.SuperGattCallback;

import java.text.DecimalFormat;
import java.util.Locale;

public class Settings  {
private final static String LOG_ID="Settings";
MainActivity activity;

/*
public static String    oldfloat2string(Float get) {
    return get.toString();
} */
private static final DecimalFormat df1 = new DecimalFormat("#.#",new DecimalFormatSymbols(Locale.US));
public static String    float2string(Float get) {
    return df1.format(get);
} 
/*
public static String    float2string(Float get) {
      return String.format("%.1f",get);
    }  */
boolean IntentscanEnabled() {
    try{
    Application app= activity.getApplication();
      PackageManager manage = app.getPackageManager();
    ComponentName  scan= new ComponentName(app, "tk.glucodata.glucodata");
    return manage.getComponentEnabledSetting(scan)!=COMPONENT_ENABLED_STATE_DISABLED;
    }
    catch (Throwable e) {

        Log.stack(LOG_ID,e);
    }
    return false;
    }
void EnableIntentScanning(boolean val) {
    try{
    Application app= activity.getApplication();
      PackageManager manage = app.getPackageManager();
    ComponentName  scan= new ComponentName(app, "tk.glucodata.glucodata");
    int com=val?COMPONENT_ENABLED_STATE_ENABLED:COMPONENT_ENABLED_STATE_DISABLED;
       manage.setComponentEnabledSetting(scan,com , DONT_KILL_APP);
    }
    catch (Throwable e) {

        Log.stack(LOG_ID,e);
    }
    }
static private Settings thisone=null;
public static void set(MainActivity act) {
    act.themeLightBars();
    thisone=new Settings();

    thisone.makesettingsin(act);

    }
private class Closerun implements Runnable {
 public  void run() {
        hidekeyboard();
        finish();
         activity.lightBars(!getInvertColors( ));
        if(tk.glucodata.Menus.on)
            tk.glucodata.Menus.show(activity);
        }
    };
private void makesettingsin(MainActivity act) {
        activity=act;

        colorwindowbackground=Applic.backgroundcolor;
       mksettings(activity);
       final var  closerun=new Closerun(); 
       /*
    final Runnable[] closerun=new Runnable[1]; //to get rid of may not be initialize nonsense
    closerun[0]=() -> {
        hidekeyboard();
        finish();
           act.lightBars(!getInvertColors( ));
        if(tk.glucodata.Menus.on)
            tk.glucodata.Menus.show(activity);

        };
    act.setonback(closerun[0]);
        */
    act.setonback(closerun);
}


void recreate() {
// removeContentView(settinglayout);
    layoutweg();
   mksettings(activity);

   }
void layoutweg() {
/*     removeContentView(settinglayout);
    settinglayout=null;*/
    }
public static void closeview() {
    if(thisone!=null)
        thisone.finish();
    }
static void hideSystemUI() {
    }
void finish() {
    layoutweg();
    settinglayout.setVisibility(GONE);
    activity.applyScreenOrientation(activity.getResources().getConfiguration());
//    if(editlabel!=null) removeContentView(editlabel) ;
    removeContentView(settinglayout);
    thisone=null;


    if(!isWearable) {
        activity.showui=false;
        }
    activity.requestRender();
    }

//    Button deletelabel;
public static    int editoptions=(isWearable?0:(EditorInfo.IME_FLAG_NO_EXTRACT_UI| EditorInfo.IME_FLAG_NO_FULLSCREEN))| EditorInfo.IME_ACTION_DONE;

 int colorwindowbackground;

static int getbackgroundcolor(Context context) {
    TypedValue typedValue = new TypedValue();
    if (context.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true) && typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
        return typedValue.data;
    } else
        return Color.RED;
}

//HorizontalScrollView settinglayout=null;
FrameLayout settinglayout=null;

static View[] mkalarm(MainActivity context,String label1,boolean show,Float value,int kind) {

       
    CheckDirectionBox yeslow = new CheckDirectionBox(context);
    yeslow.setText(label1);
    EditText alow = new EditText(context);

    final int minheight= GlucoseCurve.dpToPx(48);
    alow.setMinimumHeight(minheight);
    alow.setImeOptions(editoptions);
    alow.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//    alow.setImeOptions(editoptions);
    alow.setMinEms(2);
    Button ring=getbutton(context,R.string.ringtonename);


    if(show)  {
         yeslow.setChecked(true);
        }
   else {
        ring.setVisibility(INVISIBLE);
        alow.setVisibility(INVISIBLE);
        }

    alow.setText( float2string(value));
yeslow.setOnCheckedChangeListener(
                 (buttonView,  isChecked) -> {
         if(isChecked) {
                    alow.setVisibility(VISIBLE);
                    ring.setVisibility(VISIBLE);
            }
        else {
            ring.setVisibility(INVISIBLE);
            alow.setVisibility(INVISIBLE);
            }
    });

    return new View[]{yeslow, alow,ring};
    }
/** Rows only shown while sw is checked (replaces the INVISIBLE toggling of the old layouts). */
static void m3dependent(CompoundButton sw,View... rows) {
    for(var r:rows)
        r.setVisibility(sw.isChecked()?VISIBLE:GONE);
    sw.setOnCheckedChangeListener((buttonView,isChecked)-> {
        for(var r:rows)
            r.setVisibility(isChecked?VISIBLE:GONE);
        });
    }
/** Alarm made by mkalarm as M3 rows: switch, threshold and ringtone. */
static void m3alarm(M3.Group g,View[] alarm) {
    var sw=(CompoundButton)alarm[0];
    g.toggle(sw);
    alarm[1].setVisibility(VISIBLE);
    alarm[2].setVisibility(VISIBLE);
    var unit=getlabel(g.getContext(),R.string.m3_mgdl);
    m3dependent(sw,g.value(R.string.m3_alarm_level,alarm[1],unit),g.nav((Button)alarm[2]));
    }
public static float str2float(String str) {
    if(str!=null) {
         try {
        return Float.parseFloat(str);
                } catch(Exception e) {};
        }
    return 0.0f;
  }
public static float edit2float(EditText ed) {
    return str2float(ed.getText().toString());
    }

void hidekeyboard() {
 tk.glucodata.help.hidekeyboard(activity) ;
 }
boolean scanenabled=true;
//int addindex=-1;

//EditText glow, ghigh ,tlow,thigh;
EditText tlow,thigh;
static float round(float value,float size)  {
   return Math.round(value*size)/size;
   }
void setvalues() {

//        alow.setText( float2string(value));
    }




static public Spinner getProfileSpinner(MainActivity context) {
   var spin=  getGenSpin(context);
   final String profile=context.getString(R.string.profile);
   final List<String> strprofiles= Arrays.asList(context.getString(R.string.defaultname),profile+"1",profile+"2",profile+"3",profile+"4",profile+"5");
   final var adapt=new LabelAdapter<String>(context,strprofiles,0);
    spin.setAdapter(adapt);
    return spin;
    }

static private void advancedalarm(MainActivity context,View parview) {
    View[] verylowalarm= mkalarm(context,context.getString(R.string.verylowglucosealarm),Natives.hasalarmverylow(),Natives.alarmverylow(),5);
    View[] prelowalarm= mkalarm(context,context.getString(R.string.prelowglucosealarm),Natives.hasalarmprelow(),Natives.alarmprelow(),7);
    View[] veryhighalarm=mkalarm(context,context.getString(R.string.veryhighglucosealarm),Natives.hasalarmveryhigh(),Natives.alarmveryhigh(),6);
    View[] prehighalarm=mkalarm(context,context.getString(R.string.prehighglucosealarm),Natives.hasalarmprehigh(),Natives.alarmprehigh(),8);

    var close=getbutton(context,R.string.closename);
    var schedules=getbutton(context,R.string.schedules);
   ViewGroup layout;
    if(isWearable) {
       final var height= GlucoseCurve.getheight();
       getMargins(schedules).topMargin=getMargins(close).bottomMargin=(int)(height*.05);
       final var width= GlucoseCurve.getwidth();
       var hormarg= (int)(height*.1f);
       getMargins(veryhighalarm[1]).setMarginStart(hormarg);getMargins(veryhighalarm[2]).setMarginEnd(hormarg);
       getMargins(verylowalarm[1]).setMarginStart(hormarg);getMargins(verylowalarm[2]).setMarginEnd(hormarg);
       getMargins(prehighalarm[1]).setMarginStart(hormarg);getMargins(prehighalarm[2]).setMarginEnd(hormarg);
       getMargins(prelowalarm[1]).setMarginStart(hormarg);getMargins(prelowalarm[2]).setMarginEnd(hormarg);

        Layout lay = new Layout(context, (l, w, h) -> {
            int[] ret={w,h};
            return ret;
            },
            new View[]{schedules},
            new View[]{verylowalarm[0]},new View[]{verylowalarm[1],verylowalarm[2]},
            new View[]{veryhighalarm[0]},new View[]{veryhighalarm[1],veryhighalarm[2]},
            new View[]{prelowalarm[0]},new View[]{prelowalarm[1],prelowalarm[2]},
            new View[]{prehighalarm[0]},new View[]{prehighalarm[1],prehighalarm[2]},
            new View[]{close});
        var scroll=new ScrollView(context);    
        scroll.addView(lay);
        scroll.setFillViewport(true);
        scroll.setSmoothScrollingEnabled(false);
       scroll.setScrollbarFadingEnabled(true);
       scroll.setVerticalScrollBarEnabled(Applic.scrollbar);
       layout=scroll;
    lay.setPaddingRelative((int)(width*0.02f),0,(int)(width*0.05f),0);
    layout.setBackgroundColor(Applic.backgroundcolor);
    context.addMyContentView(layout, new ViewGroup.LayoutParams( MATCH_PARENT ,MATCH_PARENT));
       }
    else {
     var help=getbutton(context,R.string.helpname);
     help.setOnClickListener(v-> help(R.string.advancedAlarmshelp,context));
     var page=new M3.Page(context,R.string.m3_advanced_alarms,true);
     var low=page.group(R.string.m3_low);
     m3alarm(low,verylowalarm);
     m3alarm(low,prelowalarm);
     var high=page.group(R.string.m3_high);
     m3alarm(high,veryhighalarm);
     m3alarm(high,prehighalarm);
     var sched=page.group(R.string.m3_alarm_profile);
     sched.nav(R.string.schedules,null,v->schedules.performClick());
     page.buttons(help);
     page.showOnly();
     layout=page.root;
        }






    schedules.setOnClickListener(v->scheduleProfiles(context,layout));

    verylowalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(5).mkviews(context,context.getString(R.string.verylowglucosealarm),layout);
        });
    veryhighalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(6).mkviews(context,context.getString(R.string.veryhighglucosealarm),layout);
        });
    prelowalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(7).mkviews(context,context.getString(R.string.prelowglucosealarm),layout);
        });
    prehighalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(8).mkviews(context,context.getString(R.string.prehighglucosealarm),layout);
        });
Runnable saver=() -> {
         boolean hasverylow=((CheckDirectionBox) verylowalarm[0]).isChecked();
         boolean hasveryhigh=((CheckDirectionBox) veryhighalarm[0]).isChecked();
         boolean hasprelow=((CheckDirectionBox) prelowalarm[0]).isChecked();
         boolean hasprehigh=((CheckDirectionBox) prehighalarm[0]).isChecked();

         Natives.setAdvancedAlarms(str2float(((EditText)verylowalarm[1]).getText().toString()),
                    str2float(((EditText)veryhighalarm[1]).getText().toString()),hasverylow,hasveryhigh,hasprelow,hasprehigh,
str2float(((EditText)prelowalarm[1]).getText().toString()), str2float(((EditText)prehighalarm[1]).getText().toString()));
        };

    MainActivity.setonback(()-> {
        saver.run();
        removeContentView(layout);
        alarmsettings(context,parview);
        } );
    close.setOnClickListener(v->{MainActivity.doonback(); });
}

static private class ProfileScheduleHolder extends RecyclerView.ViewHolder {
    public ProfileScheduleHolder(View view,ProfileScheduleAdapter adapt,View parent) {
       super(view);
       view.setOnClickListener(v -> {
            int pos=getAbsoluteAdapterPosition();
            changeProfile((MainActivity)view.getContext(),pos,adapt,parent);
            });

    }

}
static private String profilename(int pos) {
    if(pos==0)
        return Applic.getContext().getString(R.string.defaultname);
    return Applic.getContext().getString(R.string.profile)+pos;
    }
static private class ProfileScheduleAdapter extends RecyclerView.Adapter<ProfileScheduleHolder> {
    View layout;

    ProfileScheduleAdapter(View layout) {
        this.layout=layout;
        }
    @NonNull
    @Override
    public ProfileScheduleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
         var view=new TextView( parent.getContext());
          view.setTransformationMethod(null);
          view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Applic.largefontsize);
          view.setLayoutParams(new ViewGroup.LayoutParams(  MATCH_PARENT, WRAP_CONTENT));
          if(isWearable)
              view.setGravity(Gravity.CENTER);
          else {
              view.setGravity(MainActivity.rtl?Gravity.RIGHT:Gravity.LEFT);
              var ctx=parent.getContext();
              view.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
              view.setTextColor(M3.onSurface(ctx));
              int p=M3.dp(ctx,14);
              view.setPadding(0,p,0,p);
              var tv=new TypedValue();
              if(ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,tv,true))
                  view.setBackgroundResource(tv.resourceId);
              }
           return new ProfileScheduleHolder(view,this,layout);
          }

    @Override
    public void onBindViewHolder(final ProfileScheduleHolder holder, int pos) {
    	TextView text=(TextView)holder.itemView;
        short[] minprofile=getScheduleProfile(pos);
        short min=minprofile[0];
        short profile=minprofile[1];
//        final String arrow = MainActivity.rtl ? "\u2190" : "\u27A1";
        final String arrow = MainActivity.rtl ? "\u2B05" : "\u27A1";

//     final String arrow = MainActivity.rtl ? 	"\u2B05":"\u27A1";
//        final String arrow = MainActivity.rtl ? "\u2190" : "\u2192";  // ← →
//        final String arrow = MainActivity.rtl ? "\u2B05" : "\u2B95";
//        final String arrow=MainActivity.rtl?"🡄 ":"🡆";
        text.setText(String.format(usedlocale,"%02d:%02d\t%s\t%s", min/60,min%60,arrow,profilename(profile)));
    	}

        @Override
        public int getItemCount() {
            return Natives.nrScheduledProfiles();
           }
    }
static private void changeProfile(MainActivity act,int wasindex, ProfileScheduleAdapter adapt,View parview) {
    if(!isWearable)
        EnableControls(parview,false);
    int index;
    if(wasindex<0) {
        index=Natives.nrScheduledProfiles( );
        if(index>=10) {
                Applic.argToaster(act,"Za dużo harmonogramów",Toast.LENGTH_LONG);
                return;
                }
          }
    else
        index=wasindex;

   short[] minpro=Natives.getScheduleProfile(index);
   var spin=getProfileSpinner(act) ;
   spin.setSelection(minpro[1]);
   spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public  void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
             minpro[1]= (short) position;
            }
        @Override
        public  void onNothingSelected (AdapterView<?> parent) {

        } });
    Button timebut=new Button(act);
    timebut.setText(String.format(Locale.US,"%02d:%02d",minpro[0]/60, minpro[0]%60));

    timebut.setOnClickListener(
            v->  {
                act.getnumberview().gettimepicker(act,minpro[0]/60, minpro[0]%60,
                (hour,min) -> {
                        minpro[0]= (short) (hour*60+min);
                        timebut.setText(String.format(Locale.US,"%02d:%02d",hour,min));
                   },()->{;});
         });
    var save=getbutton(act,R.string.save);
    var cancel=getbutton(act,R.string.cancel);
    var delete=getbutton(act,R.string.delete);
    if(wasindex<0)
        delete.setVisibility(INVISIBLE);
     else {
        delete.setOnClickListener(v->{
            removeScheduleProfile(index);
            adapt.notifyItemRemoved(index);
    	    NumAlarm.handlealarm(Applic.app);
            MainActivity.doonback();
            });
         }
    cancel.setOnClickListener(v->{
    	MainActivity.doonback();
    	});
    save.setOnClickListener(v->{
        int induit=Natives.setScheduleProfile(wasindex,minpro[0], minpro[1]);
        if(induit>=0) {
    	    MainActivity.doonback();
            if(induit==index) {
                if(wasindex<0)
                    adapt.notifyItemInserted(induit);
                else
                    adapt.notifyItemChanged(induit);
                }
            else
                adapt.notifyDataSetChanged();
    	    NumAlarm.handlealarm(Applic.app);
            return;
            }
        else {
            Applic.argToaster(act,"Za dużo harmonogramów",Toast.LENGTH_SHORT);
            }
    	});
//    Button help=getbutton(act,R.string.helpname);
    var height=GlucoseCurve.getheight();
   if(isWearable) {
       var width=GlucoseCurve.getheight();
        getMargins(cancel).bottomMargin=(int)(height*.1);
        getMargins(timebut).setMarginStart((int)(width*.01));
        var layout= new Layout(act, (l, w, h) -> {
    	int[] ret={w,h};
    	return ret;
    	},new View[]{cancel},new View[]{timebut,spin},new View[]{delete},new View[]{save});
        layout.setBackgroundColor(Applic.backgroundcolor);
        act.addMyContentView(layout,new ViewGroup.LayoutParams( MATCH_PARENT, MATCH_PARENT));
        MainActivity.setonback( () -> {
                removeContentView(layout);
                });
        }
     else {
        delete.setVisibility(wasindex<0?GONE:VISIBLE);
        var page=new M3.Page(act,R.string.m3_schedule,true);
        var g=page.group(null);
        g.value(R.string.m3_hour,timebut);
        g.value(R.string.m3_alarm_profile,spin);
        page.buttons(delete,cancel,save);
        page.show(()->EnableControls(parview,true));
         }
    }
static public void scheduleProfiles(MainActivity act,View parview) {
    if(!isWearable)
        EnableControls(parview,false);
    if(doLog) {Log.i(LOG_ID,"scheduleProfiles");};
    Button ok=getbutton(act,R.string.closename);
    Button newone=getbutton(act,R.string.newname);
    RecyclerView recycle = new RecyclerView(act);
    recycle.setLayoutParams(new ViewGroup.LayoutParams( WRAP_CONTENT , WRAP_CONTENT));
    LinearLayoutManager lin = new LinearLayoutManager(act);
    recycle.setLayoutManager(lin);
    if(isWearable) {
    View[][] views=new View[][]{new View[]{ok},new View[]{recycle},new View[]{newone}};
    var layout= new Layout(act, (l, w, h) -> {
    	int[] ret={w,h};
    	return ret;
    	},views);
    var numadapt = new ProfileScheduleAdapter(layout); 
    recycle.setAdapter(numadapt);
    ok.setOnClickListener(v->{
    	MainActivity.doonback();
    	});
    newone.setOnClickListener(v->{
         changeProfile(act, -1, numadapt,layout);
    	});
       final var type=MATCH_PARENT;
     act.addMyContentView(layout,new ViewGroup.LayoutParams( type,type));
        layout.setBackgroundColor(Applic.backgroundcolor);
    MainActivity.setonback( () -> {
                removeContentView(layout);
            });
        }
    else {
        Button help=getbutton(act,R.string.helpname);
        help.setOnClickListener(v->{
            help(R.string.schedulehelp,act);
            });
        var page=new M3.Page(act,R.string.schedules,true);
        var g=page.group(null);
        recycle.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
        recycle.setNestedScrollingEnabled(false);
        var listrow=g.item(recycle);
        var empty=g.text(act.getString(R.string.m3_no_schedules));
        var numadapt = new ProfileScheduleAdapter(page.root); 
        recycle.setAdapter(numadapt);
        Runnable showempty=()-> {
            boolean none=numadapt.getItemCount()==0;
            listrow.setVisibility(none?GONE:VISIBLE);
            ((View)empty.getParent()).setVisibility(none?VISIBLE:GONE);
            };
        showempty.run();
        numadapt.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { showempty.run(); }
            @Override public void onItemRangeInserted(int positionStart, int itemCount) { showempty.run(); }
            @Override public void onItemRangeRemoved(int positionStart, int itemCount) { showempty.run(); }
            });
        newone.setOnClickListener(v->{
             changeProfile(act, -1, numadapt,page.root);
            });
        page.buttons(help,newone);
        page.show(()->EnableControls(parview,true));
        }
    }static public void setProfile(MainActivity act,int profile) {
       Natives.setProfile(profile);
       SuperGattCallback.initAlarmTalk();
       }
static private void alarmsettings(MainActivity context,View parview) {
    parview.setVisibility(GONE);
    TextView alarmlow,alarmhigh;
    View[] lowalarm= mkalarm(context,context.getString(R.string.lowglucosealarm),Natives.hasalarmlow(),Natives.alarmlow(),0);
    View[] highalarm=mkalarm(context,context.getString(R.string.highglucosealarm),Natives.hasalarmhigh(),Natives.alarmhigh(),1);
    alarmlow=(TextView)lowalarm[1];
    alarmhigh=(TextView)highalarm[1];
    alarmlow.setText( float2string(Natives.alarmlow()));
    alarmhigh.setText( float2string(Natives.alarmhigh()));
    CheckDirectionBox isvalue = new CheckDirectionBox(context);
    final boolean hasvalue=Natives.hasvaluealarm();
    isvalue.setChecked(hasvalue); //Value
    isvalue.setText(R.string.valueavailablenotification);
    Button ringisvalue=getbutton(context,R.string.ringtonename);
    Button help=getbutton(context,R.string.helpname);
    help.setOnClickListener(v->{help(R.string.alarmhelp,(MainActivity)(v.getContext())); });
    if(!hasvalue) ringisvalue.setVisibility(INVISIBLE);
    isvalue.setOnCheckedChangeListener(
             (buttonView,  isChecked) -> {
             if(isChecked) {
                ringisvalue.setVisibility(VISIBLE);
                }
            else {
                ringisvalue.setVisibility(INVISIBLE);
                }
            });


//    var usealarm=getcheckbox(context, R.string.USE_ALARM, Natives.getUSEALARM());

    var alarmis=getlabel(context,R.string.alarmis);
    var alarmtype=new RadioGroup(context);

    int id=0;
    alarmtype.addView(getradiobuttonId(context,R.string.alarm,id++));
    alarmtype.addView(getradiobuttonId(context,R.string.notification,id++));
    alarmtype.addView(getradiobuttonId(context,R.string.media,id++));
    alarmtype.check(getalarmSoundType());

    final boolean alarmloss= Natives.hasalarmloss();
        CheckDirectionBox lossalarm = new CheckDirectionBox(context);
        lossalarm.setChecked(alarmloss); //Value
        lossalarm.setText(R.string.lossofsignalalarm);
    Button ringlossalarm=getbutton(context,R.string.ringtonename);
        EditText losswait = new EditText(context);
        losswait.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//    losswait.setImeOptions(editoptions);
        losswait.setImeOptions(editoptions);
        losswait.setMinEms(2);
        int waitloss=Natives.readalarmsuspension(4);
        losswait.setText(waitloss+"");

        var min=getlabel(context,R.string.minutes);
        lossalarm.setOnCheckedChangeListener(
                (buttonView,  isChecked) -> {
                    if(isChecked) {
                        ringlossalarm.setVisibility(VISIBLE);
                        losswait.setVisibility(VISIBLE);
                        min.setVisibility(VISIBLE);

                    }
                    else {
                        ringlossalarm.setVisibility(INVISIBLE);
                        losswait.setVisibility(INVISIBLE);
                        min.setVisibility(INVISIBLE);
                    }
                });
    if(!alarmloss) {
        ringlossalarm.setVisibility(INVISIBLE);
        losswait.setVisibility(INVISIBLE);
        min.setVisibility(INVISIBLE);
        }


    var Save=getbutton(context,R.string.closename);
     var advanced=getbutton(context,R.string.advanced);
    var spin=getProfileSpinner(context);
   int pos=Natives.getProfile();
    spin.setSelection(pos);
    final View lay;
    if(isWearable) {
        View[][] views;
        var ala=getlabel(context,R.string.alarms);
        final   int pad=(int)(tk.glucodata.GlucoseCurve.metrics.density*5.0);
           ala.setPadding(pad,pad,pad,0);
        final var width= GlucoseCurve.getwidth();
        int hormarg=(int)(width*.08);

        getMargins(lowalarm[1]).setMarginStart(hormarg);
        getMargins(highalarm[1]).setMarginStart(hormarg);
        getMargins(Save).topMargin=pad;
        views=new View[][]{new View[]{ala},new View[]{spin},new View[]{lowalarm[0]},new View[]{lowalarm[1],lowalarm[2]}, new View[]{highalarm[0]},new View[]{highalarm[1],highalarm[2]},
new View[]{lossalarm},new View[]{losswait,min,ringlossalarm},
new View[]{isvalue},new View[]{ringisvalue},new View[]{alarmis,alarmtype},new View[]{advanced},new View[]{Save}};
        Layout layout = new Layout(context, (l, w, h) -> {
            hideSystemUI();
        int[] ret={w,h};
        return ret;
        },views);
      final int sidepad=(int)(GlucoseCurve.metrics.density*5);
       layout.setPaddingRelative((int)(GlucoseCurve.metrics.density*8), sidepad,(int)(GlucoseCurve.metrics.density*12),sidepad);
    var scroll=new ScrollView(context);    
    scroll.addView(layout);
    scroll.setFillViewport(true);
    scroll.setSmoothScrollingEnabled(false);
   scroll.setScrollbarFadingEnabled(true);
   scroll.setVerticalScrollBarEnabled(Applic.scrollbar);
    lay=scroll;
        lay.setBackgroundColor(Applic.backgroundcolor);
    context.addMyContentView(lay, new ViewGroup.LayoutParams( MATCH_PARENT ,MATCH_PARENT));
        }
    else {
        var page=new M3.Page(context,R.string.alarms,true);
        var prof=page.group(null);
        prof.value(R.string.m3_alarm_profile,spin);

        var glu=page.group(R.string.m3_glucose_alarms);
        m3alarm(glu,lowalarm);
        m3alarm(glu,highalarm);
        glu.nav(R.string.m3_advanced_alarms,null,v->advanced.performClick());

        var sig=page.group(R.string.m3_signal);
        sig.toggle(lossalarm);
        losswait.setVisibility(VISIBLE);
        min.setVisibility(VISIBLE);
        ringlossalarm.setVisibility(VISIBLE);
        m3dependent(lossalarm,sig.value(R.string.m3_delay,losswait,min),sig.nav(ringlossalarm));
        sig.toggle(isvalue);
        ringisvalue.setVisibility(VISIBLE);
        m3dependent(isvalue,sig.nav(ringisvalue));

        var type=page.group(R.string.alarmis);
        type.item(alarmtype);
        page.buttons(help);
        lay=page.root;
        page.showOnly();
        }

    lowalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(0).mkviews(context,context.getString(R.string.lowglucosealarm),lay);
        });
    highalarm[2].setOnClickListener(v->{
        new tk.glucodata.RingTones(1).mkviews(context,context.getString(R.string.highglucosealarm),lay);
        });
      alarmtype.setOnCheckedChangeListener( (g,i)-> {
            Natives.setalarmSoundType(i);
         });

    BooleanSupplier saver=() -> {
      final boolean hasloss= lossalarm.isChecked();
        if(hasloss) {
            String str = losswait.getText().toString();
             try  {
                if(str != null) {
                     short wa = Short.parseShort(str);
                     if(wa!=Natives.readalarmsuspension(4)) {
                        Natives.writealarmsuspension(4, wa);
                        tk.glucodata.SuperGattCallback.glucosealarms.setLossAlarm();
                        }
                    }
                  else {
                        Applic.argToaster(context,context.getString(R.string.cantsetminutes)+" nothing",Toast.LENGTH_SHORT);
                        return false;
                        }
                } catch(Throwable e) {
                    Log.stack(LOG_ID,"parseShort",e);
                    Applic.argToaster(context,context.getString(R.string.cantsetminutes)+str,Toast.LENGTH_SHORT);
                    return false;
                }
            }
         boolean haslow=((CheckDirectionBox) lowalarm[0]).isChecked();
         boolean hashigh=((CheckDirectionBox) highalarm[0]).isChecked();
         Natives.setalarms(str2float(((EditText)lowalarm[1]).getText().toString()),
                    str2float(((EditText)highalarm[1]).getText().toString()),
                     haslow, hashigh, isvalue.isChecked(),hasloss);
         return true;
         };
    Save.setOnClickListener(v->{
        if(!saver.getAsBoolean()) {
            return;
            }
        context.poponback();
        removeContentView(lay) ;
        parview.setVisibility(VISIBLE);
        tk.glucodata.help.hidekeyboard(context);
        });
    context.setonback(() -> {
        saver.getAsBoolean();
        parview.setVisibility(VISIBLE);
        tk.glucodata.help.hidekeyboard(context);
        removeContentView(lay) ;
        });

   spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public  void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
            if(position!=pos) {
               saver.getAsBoolean();
               removeContentView(lay) ;
               MainActivity.poponback();
               setProfile(context,position);
               alarmsettings(context,parview);
               }
            }
        @Override
        public  void onNothingSelected (AdapterView<?> parent) {

        } });

     advanced.setOnClickListener(v->{ 
        saver.getAsBoolean();
        context.poponback();
        removeContentView(lay) ;
        advancedalarm(context,parview); 
        });
    ringisvalue.setOnClickListener(v->{
        new tk.glucodata.RingTones(2).mkviews(context,context.getString(R.string.valuenotification), lay);
        });
    ringlossalarm.setOnClickListener(v->{
        new tk.glucodata.RingTones(4).mkviews(context,context.getString(R.string.lossofsignal),lay);
        });
}


final private static String  codestr=String.valueOf(BuildConfig.VERSION_CODE);


static public Spinner getGenSpin(Activity context) {
    var spin=  new Spinner(context,isWearable?MODE_DIALOG: MODE_DROPDOWN);
    avoidSpinnerDropdownFocus(spin);
    /*
   if(isWearable) {
      var width= GlucoseCurve.getwidth();
      spin.setDropDownWidth(width);
      spin.setDropDownHorizontalOffset(0);
      }
      */
     return spin;
     }

static void mkrangelabel(TextView view,int res,float low, float high) {
     view.setText(view.getContext().getString(res)+" "+float2string(low)+"-"+float2string(high));
   }
static private void displaysettings(MainActivity context,Settings settings) {

        TextView graphlabel = new TextView(context);
        graphlabel.setText(R.string.graphrange);
        var glow = new EditText(context);
        glow.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        glow.setImeOptions(editoptions);
        glow.setMinEms(1);


        TextView line = new TextView(context);
        line.setText("-");
        var ghigh = new EditText(context);

        ghigh.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ghigh.setImeOptions(editoptions);
        ghigh.setMinEms(2);
        glow.setText(float2string(Natives.graphlow()));
        ghigh.setText(float2string(Natives.graphhigh()));
        Object[] graphrow = {graphlabel, new View[] {glow, line, ghigh}};

    TextView targetlabel = getlabel(context,R.string.targetrange);
        var tlow = new EditText(context);

        tlow.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        tlow.setMinEms(1);
        tlow.setImeOptions(editoptions);
        TextView line2=new TextView(context); line2.setText("-");
        var thigh = new EditText(context);

        thigh.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        thigh.setMinEms(2);
        thigh.setImeOptions(editoptions);
        Object[] targetrow = {targetlabel, new View[]{tlow, line2, thigh}};



        tlow.setText(float2string(Natives.targetlow()));
        thigh.setText(float2string(Natives.targethigh()));

    var colbut=getbutton(context,R.string.colors);
   var help=getbutton(context,R.string.helpname);
  help.setOnClickListener(v->{help(R.string.displayhelp,context); });


    TextView scalelabel=getlabel(context,R.string.manuallyscale);
    CheckDirectionBox fixatex =new CheckDirectionBox(context);

    CheckDirectionBox fixatey =new CheckDirectionBox(context);


    fixatex.setText(R.string.time);
    fixatey.setText(R.string.glucose);
    fixatex.setChecked(!Natives.getfixatex());
    fixatey.setChecked(!Natives.getfixatey());

        final var threslabel=getlabel(context,R.string.threshold);

        final var thresstring=float2string(Natives.getthreshold());
        final var threshold=getnumedit(context,thresstring);

      
      var close=getbutton(context,R.string.closename);

      final View top;
    if(isWearable)  {
      Layout lay;
      var iob=getcheckbox(context,"IOB",Natives.getIOB());
      iob.setOnCheckedChangeListener( (buttonView,  isChecked) -> {
                if(!Natives.setIOB(isChecked)) {
                    iob.setChecked(false);
                    tk.glucodata.help.help(R.string.IOBhelp,context);
                    }
                }
            ); 
    if(!useclose)
          close.setVisibility(GONE);
    targetlabel.setPaddingRelative((int)(tk.glucodata.GlucoseCurve.metrics.density*8.0),0,0,0);
    graphlabel.setPaddingRelative((int)(tk.glucodata.GlucoseCurve.metrics.density*8.0),0,0,0);
    //colbut.setPadding(0,0,0,0);
    threslabel.setPaddingRelative((int)(tk.glucodata.GlucoseCurve.metrics.density*7.0),0,0,0);
     var setuseclose=getcheckbox(context,R.string.useclose,useclose) ;
    setuseclose.setOnCheckedChangeListener( (buttonView,  isChecked) -> { 
         Specific.setclose(isChecked);
         Natives.setdontuseclose(!isChecked); 
         context.finish();
         context.startActivity(context.getIntent());
      });
   //      Button display=getbutton(context,context.getString(R.string.display));
   /*
     var Scans=getcheckbox(context,R.string.scansname,getshowscans()) ;
     var Calibrated=getcheckbox(context,R.string.calibrated,getshowcalibratedstream()) ;
     var History=getcheckbox(context,R.string.historyname,getshowhistories()) ;
     var Stream=getcheckbox(context,R.string.streamname,getshowstream()) ;
     var Amounts=getcheckbox(context,R.string.amountshort,getshownumbers()) ;

Calibrated.setOnCheckedChangeListener( (buttonView,  isChecked) -> { Natives.setshowcalibratedstream(isChecked); });
Scans.setOnCheckedChangeListener( (buttonView,  isChecked) -> { Natives.setshowscans(isChecked); });
    History.setOnCheckedChangeListener( (buttonView,  isChecked) -> { Natives.setshowhistories(isChecked); });
    Stream.setOnCheckedChangeListener( (buttonView,  isChecked) -> { Natives.setshowstream(isChecked); });
    Amounts.setOnCheckedChangeListener( (buttonView,  isChecked) -> { Natives.setshownumbers(isChecked); });
    */
        lay = new Layout(context, (l, w, h) -> {
                  int[] ret={w,h};
                 return ret;
               },new View[]{colbut},graphrow,targetrow,new View[]{scalelabel}, new View[]{fixatex},new View[]{fixatey},new View[]{threslabel,threshold},new View[]{iob}/*,new View[]{Scans},new View[]{History},new View[]{Stream},new View[]{Calibrated},new View[]{Amounts}*/,new View[]{setuseclose},new View[]{close});
     lay.setBackgroundColor(Applic.backgroundcolor);
      final   int pad=(int)(tk.glucodata.GlucoseCurve.metrics.density*10.0);
       lay.setPaddingRelative((int)(tk.glucodata.GlucoseCurve.metrics.density*6.0),(int)(tk.glucodata.GlucoseCurve.metrics.density*11.0),(int)(tk.glucodata.GlucoseCurve.metrics.density*7.0),pad*2);
    var scroll=new ScrollView(context);
    scroll.addView(lay);
    scroll.setFillViewport(true);
    scroll.setSmoothScrollingEnabled(false);
   scroll.setScrollbarFadingEnabled(true);
   scroll.setVerticalScrollBarEnabled(Applic.scrollbar);
   context.addMyContentView(scroll, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
   top=scroll;
         }
      else {    
//      var iob=getcheckbox(context,"IOB",Natives.getIOB());
      var iob=getbutton(context,"IOB");
      iob.setOnClickListener(v-> {
        tk.glucodata.IOB.mkview(context);
        });
        var dexfuture=getcheckbox(context,R.string.dexfuture,Natives.getdexcomPredict());
        dexfuture.setOnCheckedChangeListener( (buttonView,  isChecked) -> Natives.setdexcomPredict(isChecked) );
        var reverseorientation=getcheckbox(context,R.string.invertscreen,(Natives.getScreenOrientation()&SCREEN_ORIENTATION_REVERSE_LANDSCAPE)!=0);

    CheckDirectionBox levelleft= new CheckDirectionBox(context);
    levelleft.setText(R.string.glucoseaxisleft);
    levelleft.setChecked(Natives.getlevelleft());

    levelleft.setOnCheckedChangeListener( (buttonView,  isChecked) -> {
             Natives.setlevelleft(isChecked);
            });

        /* G7 fork: auto-rotate is always off. Rotate 180° only has an effect on
         * elongated large screens without the Android 16 large-screen restriction;
         * phones draw the graph directly in portrait (MainActivity.applyScreenOrientation). */
        {
            final var config=context.getResources().getConfiguration();
            final boolean largeScreen=config.smallestScreenWidthDp>=600;
            final int shortSide=Math.min(config.screenWidthDp,config.screenHeightDp);
            final int longSide=Math.max(config.screenWidthDp,config.screenHeightDp);
            final boolean elongated=shortSide>0&&((float)longSide/shortSide)>=1.45f;
            final boolean android16LargeRestriction=context.getApplicationInfo().targetSdkVersion>=36;
            reverseorientation.setVisibility(largeScreen&&elongated&&!android16LargeRestriction?VISIBLE:GONE);
        }

        reverseorientation.setOnCheckedChangeListener( (buttonView,  isChecked) ->  {
                int ori= (isChecked?SCREEN_ORIENTATION_REVERSE_LANDSCAPE:SCREEN_ORIENTATION_LANDSCAPE);
                Natives.setScreenOrientation(ori);
                context.applyScreenOrientation(context.getResources().getConfiguration());
                });

        var page=new M3.Page(context,R.string.display,true);
        var graph=page.group(R.string.m3_graph);
        graph.value(R.string.m3_graphrange,glow,line,ghigh);
        graph.value(R.string.m3_targetrange,tlow,line2,thigh);
        graph.toggle(levelleft);
        graph.toggle(reverseorientation);
        var scale=page.group(R.string.manuallyscale);
        scale.toggle(fixatex);
        scale.toggle(fixatey);
        var other=page.group(R.string.m3_other);
        other.value(R.string.threshold,threshold);
        other.toggle(dexfuture);
        other.nav("IOB",null,v->iob.performClick());
        other.nav(R.string.colors,null,v->colbut.performClick());
        page.buttons(help);
        page.showOnly();
        top=page.root;
         }

   colbut.setOnClickListener(v-> {
           MainActivity.doonback();
        settings.finish();
        SetColors.show(context);
        });

Runnable closerun= () -> {
      var newthreshold=threshold.getText().toString();
      if(!thresstring.equals(newthreshold)) {
        float thres=str2float(newthreshold);
        if(thres>0.8f||thres<0.0f) {
          Applic.argToaster(context, "Próg powinien wynosić od 0.0 do 0.8",Toast.LENGTH_LONG);
           }
         else
           setthreshold(thres);
        }
        Natives.setfixatex(!fixatex.isChecked());
        Natives.setfixatey(!fixatey.isChecked());
       Natives.setGraphRange(str2float(glow.getText().toString()), str2float(ghigh.getText().toString()));
      Natives.setTargetRange(str2float(tlow.getText().toString()), str2float(thigh.getText().toString()));
        removeContentView(top) ;
        };
    MainActivity.setonback(closerun);

      close.setOnClickListener(v->{
           context.poponback();
         closerun.run();
      });

   }
       /*graphrange.setLabelFormatter(f->{
            var str= Settings.float2string(f);
          {if(doLog) {Log.i(LOG_ID,"setLabelformatter "+str);};};
          return str;}); */
//       graphrange.setLabelBehavior(LabelFormatter.LABEL_WITHIN_BOUNDS);
//       graphrange.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
 //      graphrange.setHaloRadius((int)(tk.glucodata.GlucoseCurve.metrics.density*15.0f));
private void mkM3settings(MainActivity context) {
    var page=new tk.glucodata.M3.Page(context,R.string.settings,false);
    final View parent=page.root;

    var meas=page.group(R.string.m3_measurement);
    meas.nav(R.string.alarms,null,v->alarmsettings(context,parent));
    meas.nav(R.string.calibration,null,v->Calibration.show(context,parent));
    var glucosenotify=getcheckbox(context,R.string.glucosestatusbar,Natives.getshowalways());
    glucosenotify.setOnCheckedChangeListener((buttonView,isChecked)->Notify.glucosestatus(isChecked));
    meas.toggle(glucosenotify);
    if(MainActivity.hasnfc) {
        var nfcsound=getcheckbox(context,R.string.nfcsound,Natives.nfcsound());
        nfcsound.setOnCheckedChangeListener((buttonView,isChecked)-> {
            Natives.setnfcsound(isChecked);
            context.setnfc();
            });
        meas.toggle(nfcsound);
        scanenabled=IntentscanEnabled();
        var globalscan=getcheckbox(context,R.string.startsapp,scanenabled);
        globalscan.setOnCheckedChangeListener((buttonView,isChecked)->EnableIntentScanning(isChecked));
        meas.toggle(globalscan);
        final int diskey=Natives.camerakey();
        if(diskey>0) {
            var camera=getcheckbox(context,R.string.disablecamerakey,diskey==1);
            camera.setOnCheckedChangeListener((buttonView,isChecked)->Natives.setcamerakey(isChecked?1:2));
            meas.toggle(camera);
            }
        }
    var googlescan=getcheckbox(context,R.string.googlescan,Natives.getGoogleScan());
    googlescan.setOnCheckedChangeListener((buttonView,isChecked)->Natives.setGoogleScan(isChecked));
    meas.toggle(googlescan);

    var data=page.group(R.string.m3_data);
    data.nav(R.string.exchanges,null,v->exchanges(context,parent));
    if(doLog)
        data.nav(R.string.logging,null,v->LogConfig.make(context,parent));

    var look=page.group(R.string.m3_appearance);
    look.nav(R.string.display,null,v->displaysettings(context,this));

    var other=page.group(R.string.m3_other);
    other.action(R.string.helpname,null,v->help(R.string.settinghelp,context));
    other.action("Wprowadzenie",null,v->help(R.string.introhelp,context));
    final String advhelp=Natives.advanced();
    if(advhelp!=null)
        other.action(R.string.advanced,null,v-> {
            EnableControls(parent,false);
            help(advhelp,context,l->EnableControls(parent,true));
            });
    other.value(context.getString(R.string.m3_version),getlabel(context,BuildConfig.VERSION_NAME));

    settinglayout=page.root;
    page.showOnly();
    }
private    void mksettings(MainActivity context) {

    if(settinglayout==null) {
    if(!isWearable) {
        mkM3settings(context);
        setvalues();
        return;
        }


        Button changelabels=new Button(context);
        Button help =new Button(context);
        help.setText(R.string.helpname);
        help.setOnClickListener(v->{help(R.string.settinghelp,(MainActivity)(v.getContext())); });

      var close=getbutton(context,R.string.closename);
        if(!isWearable) {
            int hormarg= (int)(tk.glucodata.GlucoseCurve.metrics.density*15.0);
            getMargins(help).setMarginStart(hormarg);
            getMargins(close).setMarginEnd(hormarg);
             }


      // G7 fork: Settings is a tab of the bottom bar, leave it via the bar.
      if(!useclose||!isWearable)
     close.setVisibility(GONE);
//    CheckDirectionBox bluetooth= new CheckDirectionBox(context);
   CheckDirectionBox globalscan = new CheckDirectionBox(context);
    globalscan.setText(R.string.startsapp);

    final var hasnfc=MainActivity.hasnfc;
      final  CheckDirectionBox nfcsound=hasnfc?new CheckDirectionBox(context):null;
    if(hasnfc)  {
        nfcsound.setText(R.string.nfcsound);
        nfcsound.setChecked(Natives.nfcsound());
        scanenabled=IntentscanEnabled();
        globalscan.setChecked(scanenabled) ; //Value
        globalscan.setOnCheckedChangeListener( (buttonView,  isChecked) -> 
                EnableIntentScanning(isChecked));
           nfcsound.setOnCheckedChangeListener((buttonView,  isChecked) -> {
                Natives.setnfcsound(isChecked);
                context.setnfc();
                });
        }
    CheckDirectionBox camera=!isWearable?new CheckDirectionBox(context):null;
    if(!isWearable) {
        final int diskey=!isWearable?Natives.camerakey():0;
        if(diskey>0)  {
            camera.setText(R.string.disablecamerakey);
            camera.setChecked(diskey==1);
            camera.setOnCheckedChangeListener((buttonView,  isChecked) -> {
                 final int setto = isChecked ? 1 : 2;
                 Natives.setcamerakey(setto);
                 });

            }
            
        else
            camera.setVisibility(GONE);

        }

   var exchanges=getbutton(context,R.string.exchanges);
    final boolean blueused=Natives.getusebluetooth();

    //bluetooth.setChecked(blueused);
    var alarmbut=getbutton(context,R.string.alarms);
        alarmbut.setOnClickListener(v->{
            alarmsettings(context,settinglayout);
            });



    close.setOnClickListener(v->{
           context.poponback();
            hidekeyboard();
            finish();
     context.lightBars(!getInvertColors( ));
         if(tk.glucodata.Menus.on) tk.glucodata.Menus.show(context);

            });
   var displayview=getbutton(context,R.string.display);
    Layout[] thelayout=new Layout[1];
    if(!isWearable) {
        changelabels.setText(R.string.numberlabels);
        changelabels.setOnClickListener(v-> {
                hidekeyboard();
                new LabelsClass(context).mklabellayout(thelayout[0]);});
              }
    Button advanced=null;


    View[][] views;
    View[][] portraitViews=null;
    final String advhelp=isWearable?null:Natives.advanced();

        var calibration=  getbutton(context,R.string.calibration);
        calibration.setOnClickListener(v-> {
            Calibration.show(context,thelayout[0]);
        });
    if(isWearable) {
        Button talk=null;
        Button complications;
        if(BuildConfig.minSDK>=26) {
            complications = getbutton(context, R.string.complications);
            complications.setOnClickListener(v -> tk.glucodata.glucosecomplication.ColorConfig.show(context, thelayout[0]));
                        final var margins=getMargins(complications);
                        margins.topMargin= (int)(tk.glucodata.GlucoseCurve.metrics.density*3.0);
                        margins.bottomMargin= (int)(tk.glucodata.GlucoseCurve.metrics.density*4.0);

        }

//      alarmbut.setMinimumWidth(0); alarmbut.setMinWidth(0);
        var uppad=(int)(tk.glucodata.GlucoseCurve.metrics.density*9.0);
      alarmbut.setPadding(uppad,alarmbut.getPaddingTop(),uppad,alarmbut.getPaddingBottom());

     View[] talkrow;
        if(doLog) {
                Button logview=getbutton(context,R.string.logging);
                logview.setOnClickListener(v->LogConfig.make(context,thelayout[0]));
                talkrow=new View[]{talk,logview};
                }
         else {
                talkrow=new View[]{talk};
            }


        View[] camornum=new View[] {alarmbut};
        if(BuildConfig.minSDK>=26) {
            views = new View[][]{new View[]{displayview}, hasnfc ? (new View[]{globalscan, nfcsound}) : null,new View[]{complications},talkrow, new View[]{exchanges },new View[]{calibration},   camornum, new View[]{close}, new View[]{getlabel(context, tk.glucodata.BuildTime.TIME)}, new View[]{getlabel(context, BuildConfig.VERSION_NAME)}, new View[]{getlabel(context, codestr)}};
            ;
        }
        else{
            views = new View[][]{new View[]{displayview},    hasnfc ? (new View[]{globalscan, nfcsound}) : null, talkrow,  new View[]{exchanges },new View[]{calibration},     camornum,new View[]{close},  new View[]{getlabel(context, tk.glucodata.BuildTime.TIME)}, new View[]{getlabel(context, BuildConfig.VERSION_NAME)}, new View[]{getlabel(context, codestr)}};
            ;
        }
        }
    else {
        var alarmmarg=getMargins(alarmbut);
        var hormarg=(int)(tk.glucodata.GlucoseCurve.metrics.density*30);
        alarmmarg.setMarginEnd(hormarg);
        View[] row9;
        var intro=getbutton(context,"Wprowadzenie");
         intro.setOnClickListener(v-> help(R.string.introhelp,context));
        if(advhelp!=null) {
            advanced=new Button(context);
            advanced.setText(R.string.advanced);
            row9= new View[]{help,advanced,intro,close} ;
            }
        else {
            row9= new View[]{help,intro,close} ;
            }

//      var oldxdrip=getbutton(context,"send old"); oldxdrip.setOnClickListener(v-> tk.glucodata.Natives.sendxdripold());
      CheckDirectionBox glucosenotify=new CheckDirectionBox(context);
        glucosenotify.setText(R.string.glucosestatusbar);
        glucosenotify.setChecked(Natives.getshowalways()) ;
        glucosenotify.setOnCheckedChangeListener( (buttonView,  isChecked) -> Notify.glucosestatus(isChecked) );
      View[] numdis;
      var googlescan=getcheckbox(context, R.string.googlescan, Natives.getGoogleScan());
       googlescan.setOnCheckedChangeListener( (buttonView,  isChecked) -> Natives.setGoogleScan(isChecked) );

        if(doLog) {
                Button logview;
                logview=getbutton(context,R.string.logging);
                logview.setOnClickListener(v->LogConfig.make(context,thelayout[0]));
                numdis= new View[]{logview,googlescan,displayview};
                }
          else {
                numdis= new View[]{googlescan,displayview};
                }

        View[] rowglu=new View[]{calibration,glucosenotify};
        views=new View[][]{hasnfc?new View[]{nfcsound, globalscan,camera}:null,rowglu,new View[]{exchanges,alarmbut},numdis, row9};

        if(advhelp!=null) {
            portraitViews=new View[][]{
                hasnfc?new View[]{nfcsound}:null,hasnfc?new View[]{globalscan}:null,hasnfc?new View[]{camera}:null,
                new View[]{calibration,glucosenotify},
                new View[]{exchanges,alarmbut},
                new View[]{numdis[0],numdis[1]},doLog?new View[]{numdis[2]}:null,
                new View[]{help,advanced},new View[]{intro,close}};
            }
        else {
            portraitViews=new View[][]{
                hasnfc?new View[]{nfcsound}:null,hasnfc?new View[]{globalscan}:null,hasnfc?new View[]{camera}:null,
                new View[]{calibration,glucosenotify},
                new View[]{exchanges,alarmbut},
                new View[]{numdis[0],numdis[1]},doLog?new View[]{numdis[2]}:null,
                new View[]{help,intro,close}};
            }
        }

    help.setFocusableInTouchMode(true);
    help.setFocusable(true);
     help.requestFocus();
     help.requestFocusFromTouch();

        Layout lay = new Layout(context, (l, w, h) -> {
            hideSystemUI(); int[] ret={w,h};
        
        return ret;
        },views);
    if(portraitViews!=null)
        lay.portraitLayout(portraitViews);

     exchanges.setOnClickListener(v->{
        exchanges(context,lay);
        });
    thelayout[0]=lay;
        if(advhelp!=null) {
            advanced.setOnClickListener(v -> {
                EnableControls(thelayout[0],false);
                help(advhelp, (MainActivity) (v.getContext()),l->
                    EnableControls(thelayout[0],true)
                    
                    );
            });
            }


        lay.setBackgroundColor(colorwindowbackground);
/*var    horlayout= new HorizontalScrollView(context);
    horlayout.addView(lay);
    horlayout.setHorizontalScrollBarEnabled(false);
    horlayout.setFillViewport(true);
   horlayout.setPadding(0,0,0,0); */

    ScrollView scroller=new ScrollView(context);
    scroller.addView(lay);
    scroller.setSmoothScrollingEnabled(false);
   scroller.setVerticalScrollBarEnabled(Applic.scrollbar);
   scroller.setScrollbarFadingEnabled(true);//Crash with NestedScrollView
    scroller.setFillViewport(true);
    scroller.setPadding(0,0,0,0);

   settinglayout=scroller;


       final   int pad=(int)(tk.glucodata.GlucoseCurve.metrics.density*7.0);
    if(isWearable) {
   //   lay.setPadding((int)(tk.glucodata.GlucoseCurve.metrics.density*14.0),(int)(tk.glucodata.GlucoseCurve.metrics.density*11.0),(int)(tk.glucodata.GlucoseCurve.metrics.density*14.0),pad);
      lay.setPaddingRelative((int)(tk.glucodata.GlucoseCurve.metrics.density*5.5),(int)(tk.glucodata.GlucoseCurve.metrics.density*11.0),(int)(tk.glucodata.GlucoseCurve.metrics.density*14.0),pad);
        }
     else {
       lay.systembarPadding((left,top,right,bottom)->
               new int[]{left+pad,top*3/4,pad+right,pad+tk.glucodata.DexTabs.bottomPart(bottom)});
      }

    final    int laywidth=MATCH_PARENT;
     context.addMyContentView(settinglayout, new ViewGroup.LayoutParams( laywidth ,MATCH_PARENT));
    displayview.setOnClickListener(v-> {
         displaysettings(context,this);
         });
        }
    else {
        settinglayout.setVisibility(VISIBLE);
        settinglayout.bringToFront();
    }

setvalues();
}

@SuppressLint("UseCompatLoadingForDrawables")
static private void exchanges(MainActivity context, View parent) {
  parent.setVisibility(GONE);
 /* 
   var emulator=getbutton(context,R.string.l3emu_title);
   emulator.setOnClickListener(v -> tk.glucodata.Libre3Emulator.show(context));
   if(BuildConfig.libreVersion!=3 || Build.VERSION.SDK_INT<26) emulator.setVisibility(GONE);

   */
   var mirrorview=getbutton(context,R.string.mirror);
   mirrorview.setOnClickListener(v ->{ (new Backup()).realmkbackupview(context,false); });
    View[] thelayout = {null};
   var ok = getbutton(context, R.string.closename);
    if(!useclose)
        ok.setVisibility(GONE);
    ok.setOnClickListener(
        v->{
          context.doonback();
        });
   final View lay;
    if (isWearable) {
        var wlay = new Layout(context, (l, w, h) -> {
            int[] ret = {w, h};
            return ret;
        },new View[]{mirrorview}, new View[]{ok});

   final var density=tk.glucodata.GlucoseCurve.metrics.density;
        wlay.setPadding((int)(density*8.0),(int)(density*25.0),(int)(density*8.0),(int)(density*2.0));
    lay=wlay;
    lay.setBackgroundColor(Applic.backgroundcolor);
       context.addMyContentView(lay, new ViewGroup.LayoutParams( MATCH_PARENT ,MATCH_PARENT));
    } else {
        final var healthconnect = getcheckbox(context, "Health Connect", Natives.gethealthConnect());
        healthconnect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Natives.sethealthConnect(isChecked);
                    if (isChecked) {
                        MainActivity.tryHealth = 5;
                        HealthConnection.Companion.init(context);
                    } else {
                        MainActivity.tryHealth = 0;
                        HealthConnection.Companion.stop();
                    }
                }
        );
        var help = getbutton(context, R.string.helpname);
      help.setOnClickListener(v->{help(R.string.exchangehelp,context); });

        var page=new M3.Page(context,R.string.exchanges,true);
        var share=page.group(R.string.m3_sharing);
        share.nav(R.string.webserver,null,v -> tk.glucodata.Nightscout.show(context, thelayout[0]));
        share.nav("Dexcom Clarity",null,v -> tk.glucodata.Clarity.show(context,thelayout[0]));
        share.nav(R.string.mirror,null,v ->{ (new Backup()).realmkbackupview(context,false); });
        var health=page.group("Health Connect");
        health.toggle(healthconnect);
        health.nav(R.string.activity_import,null,v -> tk.glucodata.ActivitySettings.show(context, thelayout[0]));
        page.buttons(help);
        page.showOnly();
        lay=page.root;
      }

    thelayout[0] = lay;

    context.setonback(() -> {
     parent.setVisibility(VISIBLE);
        removeContentView(lay) ;
        });
}

//ViewGroup labellayout=null;



public static void   removeContentView(View view) {
    ViewGroup parent= (ViewGroup)view.getParent();
    if(parent!=null)
        parent.removeView(view);
    }
}
