
package tk.glucodata.settings;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Applic.backgroundcolor;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Natives.setshowcalibratedstream;
import static tk.glucodata.NumberView.avoidSpinnerDropdownFocus;
import static tk.glucodata.settings.Settings.removeContentView;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getcheckbox;
import static tk.glucodata.util.getlabel;
import static tk.glucodata.RingTones.EnableControls;


import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import tk.glucodata.Applic;
import tk.glucodata.GlucoseCurve;
import tk.glucodata.LabelAdapter;
import tk.glucodata.M3;
import tk.glucodata.Layout;
import tk.glucodata.MainActivity;
import tk.glucodata.Natives;
import tk.glucodata.R;

public class Calibration  {


static public void show(MainActivity act,View parent) {
    if(parent!=null)  {
//           parent.setVisibility(GONE);
           EnableControls(parent,false);
//           if(!isWearable) act.lightBars(!Natives.getInvertColors());
            }
    Spinner spinner=new Spinner(act);
    avoidSpinnerDropdownFocus(spinner);
    LabelAdapter<String> numspinadapt=new LabelAdapter<String>(act, Natives.getLabels(),0);
    spinner.setAdapter(numspinadapt);
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public  void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
            Natives.setbloodvar((byte)position);
            }
        @Override
        public  void onNothingSelected (AdapterView<?> parent) {

        } });
   spinner.setSelection(Natives.getbloodvar());
   var bloodvar=getlabel(act,R.string.bloodvar);
    float density=GlucoseCurve.metrics.density;
   bloodvar.setPaddingRelative((int)(density*10),0,(int)(4*density),0);
   var close=getbutton(act, R.string.closename);
   var allvalues=getcheckbox(act,act.getString(R.string.allvalues),Natives.getAllValues());
   allvalues.setOnCheckedChangeListener( (buttonView,  isChecked)-> {
          Natives.setAllValues(isChecked);
          act.requestRender();
          });
   var showcalibrated=getcheckbox(act,act.getString(R.string.showcalibratedstream),Natives.getshowcalibratedstream());
   showcalibrated.setOnCheckedChangeListener( (buttonView,  isChecked)-> {
          setshowcalibratedstream(isChecked);
          act.requestRender();
          });
   var docalibrate=getcheckbox(act,act.getString(R.string.active),Natives.getDoCalibrate());
   docalibrate.setOnCheckedChangeListener( (buttonView,  isChecked)-> {
          Natives.setDoCalibrate(isChecked);
          if(isChecked) {
            showcalibrated.setChecked(true);
            }
          act.requestRender();
          });
   var calibratepast=getcheckbox(act,act.getString(R.string.calibratepast),Natives.getCalibratePast());
   calibratepast.setOnCheckedChangeListener( (buttonView,  isChecked)-> {
          Natives.setCalibratePast(isChecked);
          act.requestRender();
          });
   var calibrateA=getcheckbox(act,act.getString(R.string.calibrate_a),Natives.getCalibrateA());
   calibrateA.setOnCheckedChangeListener( (buttonView,  isChecked)-> {
          Natives.setCalibrateA(isChecked);
          });
  if(isWearable) {
  View[][] views;
        var buttons=new View[]{docalibrate,close};
        views=new View[][]{new View[]{calibrateA},
       new View[]{bloodvar,spinner},buttons,new View[]{calibratepast},new View[]{allvalues}} ;
    ViewGroup  layout=new Layout(act, (lay, w, h) -> { return new int[] {w,h};},views );
    layout.setBackgroundColor(backgroundcolor);
    var height=    GlucoseCurve.getheight();
    layout.setPaddingRelative((int)(height*.02f),(int)(height*.17f), (int)(height*.05f), 0);
    var pasmarg=Layout.getMargins(allvalues);
    pasmarg.bottomMargin=(int)(height*.14f);
        var scroll=new ScrollView(act);
        scroll.addView(layout);
        scroll.setFillViewport(true);
        scroll.setSmoothScrollingEnabled(false);
       scroll.setScrollbarFadingEnabled(true);
       scroll.setVerticalScrollBarEnabled(Applic.scrollbar);
    act.addMyContentView(scroll, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    MainActivity.setonback( () -> {
      if(parent!=null)  {
           EnableControls(parent,true);
            }
        removeContentView(scroll);
        });

    close.setOnClickListener(v->{
    	MainActivity.doonback();
    	});
  }
  else {
       var help=getbutton(act, R.string.helpname);
        help.setOnClickListener(v->{
             tk.glucodata.help.help(R.string.calibrationhelp,act);
            });
       var page=new M3.Page(act,R.string.calibration,true);
       var g=page.group(null);
       g.toggle(docalibrate);
       g.toggle(calibrateA);
       g.toggle(calibratepast);
       g.value(act.getString(R.string.bloodvar).replace(":","").trim(),spinner);
       var show=page.group(R.string.m3_show);
       show.toggle(showcalibrated);
       show.toggle(allvalues);
       page.buttons(help);
       page.show(()-> {
          if(parent!=null)
               EnableControls(parent,true);
          });
    }
   }

};
