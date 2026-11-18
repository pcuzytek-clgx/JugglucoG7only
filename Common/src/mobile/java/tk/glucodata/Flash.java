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

import static tk.glucodata.Log.doLog;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Flash  {
static private final String LOG_ID="Flash";
    private interface Flasher {
        void on();
        void off();
        void end();
    };

static    class flash23 implements Flasher {

        private CameraManager camManager ;
        String cameraId = null;
        public void end() {

        };
        flash23(Context context) {
            try {
                camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (camManager != null) {
                    var camas=camManager.getCameraIdList();
                    if(camas!=null&&camas.length>0) {
                        for (var c : camas) {
                            CameraCharacteristics cha = camManager.getCameraCharacteristics(c);
                            if (cha.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                                cameraId = c;
                                break;
                            }
                        }
                        if (cameraId == null) {
                            {if(doLog) {Log.i(LOG_ID, "No cameraId=");};};
                            cameraId = camas[0];
                        }

                        {if(doLog) {Log.i(LOG_ID, "cameraId=" + cameraId);};};
                    }

                }
            } catch (Throwable e) {
                    Log.stack(LOG_ID,"flash23",e);
            }
        }
        void set(boolean val) {
            try {
                if(cameraId!=null) {
            	    {if(doLog) {Log.i(LOG_ID,"set("+val+")");};};
                    camManager.setTorchMode(cameraId, val);
		    }
		 else {
            	    {if(doLog) {Log.i(LOG_ID,"cameraID==null");};};
		 	}
            }
            catch(Throwable e) {
                    Log.stack(LOG_ID,"set",e);
            }
        }
        public void on() {
            set(true);
        }
        public void off() {
            set(false);
        }
    };

static public boolean hasFlash(Context context) {
	try {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)&& context.getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
            // this device has a camera
            {if(doLog) {Log.i(LOG_ID,"Can Flash");};};
            return true;
        } else {
            {if(doLog) {Log.i(LOG_ID,"Can not Flash");};};
            // no camera on this device
            return false;
        }
	}
	catch(Throwable e) {
         Log.stack(LOG_ID,"hasFlash",e);
            return false;
		}
    }
static    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
   static boolean onflag=false;
   static volatile boolean stopflash=false;;
static     Flasher flash=null;
   static  Runnable flashmore=() ->{
   	if(stopflash)
		return;
 //       tk.glucodata.Applic.RunOnUiThread(()-> {
 	var wasflash=flash;
	if(wasflash!=null) {
	    if (onflag) {
		wasflash.off();
	    } else wasflash.on();
	    onflag=!onflag;
	    }
  //      });
};
static    ScheduledFuture<?> flashschedule=null;
static void start(Context context) {

	flash = new flash23(context);

	stopflash=false;
	flashschedule  = scheduler.scheduleAtFixedRate(flashmore, 0,150 , TimeUnit.MILLISECONDS);
	}
static private synchronized void dostop() {
	if(doLog) {Log.i(LOG_ID,"stop");};
	stopflash=true;
	onflag=true;
    if(flashschedule !=null)  {
            flashschedule.cancel(true);
	    flashschedule=null;
	    onflag=true;
 	//    {if(doLog) {Log.i(LOG_ID,"cancel");};};
	    }
    if(flash!=null) {
//tk.glucodata.Applic.RunOnUiThread(()-> {
        flash.off();
        flash.end();
        flash=null;
//	});
 	{if(doLog) {Log.i(LOG_ID,"endstop");};};
	}
	}

static  void stop() {
     Applic.scheduler.schedule(Flash::dostop, 0, TimeUnit.MILLISECONDS);
     }
}

