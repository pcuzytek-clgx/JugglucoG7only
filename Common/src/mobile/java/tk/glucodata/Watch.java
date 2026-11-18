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
/*      Fri Jan 27 15:32:11 CET 2023                                                 */


package tk.glucodata;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Applic.mgdLmult;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MessageSender.initwearos;
import static tk.glucodata.settings.Settings.removeContentView;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getcheckbox;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

class Watch {
static private final boolean TestBridge=BuildConfig.DEBUG;
static private float glucose=80f;
static private float trend=-5.2f;
static private final  String LOG_ID="Watch";

static private void enableMessageReceiver(boolean val) {
    try{
       Application app= Applic.app;
       PackageManager manage = app.getPackageManager();
       ComponentName scan= new ComponentName(app, tk.glucodata.MessageReceiver.class);
       int com=val?COMPONENT_ENABLED_STATE_ENABLED:COMPONENT_ENABLED_STATE_DISABLED;
       manage.setComponentEnabledSetting(scan,com , DONT_KILL_APP);
      }
    catch (Throwable e) {
        Log.stack(LOG_ID,e);
        }
    }
static private void setuseWearos(boolean value) {
    enableMessageReceiver(value);
    }
static public void show(MainActivity context) {
       var notify=getcheckbox(context,context.getString(R.string.notify), Notify.alertwatch);
    
    notify.setOnCheckedChangeListener(
        (buttonView,  isChecked) ->  {
            Applic.app.setnotify(isChecked);
            });

       var separate=getcheckbox(context,context.getString(R.string.separate), Notify.alertseparate);
    separate.setOnCheckedChangeListener(
        (buttonView,  isChecked) ->  {
            Notify.alertseparate=isChecked;
            Natives.setSeparate(isChecked);
            });

       var gadget=getcheckbox(context,"GadgetBridge", SuperGattCallback.doGadgetbridge);
        gadget.setOnCheckedChangeListener(
            (buttonView,  isChecked) ->  {
                Natives.setgadgetbridge(isChecked);
                SuperGattCallback.doGadgetbridge=isChecked;
                });
    var test=TestBridge?getbutton(context,"Test"):null;
    View[] mibandrow;
    if(TestBridge) {
        test.setOnClickListener(v-> {
            int mgdl;
            trend += 0.6f;
            if (trend > 5f)
                trend = -5f;
            if(Applic.unit==1) {
                glucose += 0.6f;
                if (glucose > 28f)
                    glucose = 2.2f;

            mgdl=(int)Math.round(glucose*mgdLmult);
            }
            else {
                glucose += 13f;
                if (glucose > 500f)
                    glucose = 40f;
                mgdl=(int)glucose;

            }
                
                Gadgetbridge.sendglucose(""+glucose,mgdl,glucose,trend, System.currentTimeMillis());
                });

        mibandrow=new View[]{gadget,test};
        }
    else  {
        mibandrow=new View[]{gadget};
        }
    var usexdripserver=Natives.getusexdripwebserver();
    var server=getcheckbox(context,R.string.webserver,usexdripserver);
    server.setOnCheckedChangeListener( (buttonView,  isChecked) -> Natives.setusexdripwebserver(isChecked));
    var usegarmin=Natives.getusegarmin();
    var kerfstok=getcheckbox(context,"Kerfstok",usegarmin); 
    final View[] statusRow={null};
    final View[] wearRow={null};
    kerfstok.setOnCheckedChangeListener(
             (buttonView,  isChecked) -> {
             if(isChecked&&!usegarmin)
                Applic.app.numdata.reinit(context);
            if(statusRow[0]!=null) statusRow[0].setVisibility(isChecked?VISIBLE:GONE);
             });
    var useWearos=Applic.useWearos();
    if(useWearos) {
        var sender=tk.glucodata.MessageSender.getMessageSender();
            if(sender!=null) sender.finddevices();
         Natives.switchSync();
        }

    var wearbox=getcheckbox( context, "WearOS", useWearos);
    wearbox.setOnCheckedChangeListener(
             (buttonView,  isChecked) -> {
            setuseWearos(isChecked);
             if(isChecked) {
                 if(!useWearos) {
                    initwearos(Applic.app);
                    }
                else  {
                    var sender=tk.glucodata.MessageSender.getMessageSender();
                    if(sender!=null) sender.finddevices();
                    }
               // Natives.networkpresent();
                Natives.switchSync();
                }
            if(wearRow[0]!=null) wearRow[0].setVisibility(isChecked?VISIBLE:GONE);
             });


    var Help=getbutton(context,R.string.helpname);
    Help.setOnClickListener(v-> {
        context.themeLightBars();
        help.help(R.string.watchinfo,context,l->context.lightBars(!Natives.getInvertColors( ))); 
        });

    notify.setText(R.string.m3_watch_notify);
    separate.setText(R.string.m3_watch_separate);
    var page=new M3.Page(context,R.string.m3_watch_title,true);
    final View layout=page.root;

    var gnotify=page.group(R.string.m3_watch_notifications);
    gnotify.toggle(notify);
    gnotify.toggle(separate);

    var gwear=page.group("Wear OS");
    gwear.toggle(wearbox);
    wearRow[0]=gwear.nav(R.string.m3_watch_wearos_config,null,v->Wearos.show(context,layout));
    wearRow[0].setVisibility(useWearos?VISIBLE:GONE);

    var ggarmin=page.group(R.string.m3_watch_garmin);
    ggarmin.toggle(kerfstok);
    statusRow[0]=ggarmin.nav(R.string.m3_watch_garmin_status,null,v->new GarminStatus(context,Applic.app.numdata,layout));
    statusRow[0].setVisibility(usegarmin?VISIBLE:GONE);

    var gother=page.group(R.string.m3_watch_other);
    gother.toggle(gadget);
    if(test!=null)
        gother.action("Test",null,v->test.performClick());
    gother.toggle(server);
    gother.nav(R.string.m3_watch_server_config,null,v->Nightscout.show(context,layout));

    page.buttons(Help);
    page.show(()-> {
           if(usegarmin!=kerfstok.isChecked()) {
               Natives.setusegarmin(!usegarmin);
            if(usegarmin) {
                Natives.sethasgarmin(false);
                Applic.app.numdata.stop();
                }
               }
        context.hideSystemUI(); 
        if(Menus.on) {
            Menus.show(context);
            }
        });
    }
}



