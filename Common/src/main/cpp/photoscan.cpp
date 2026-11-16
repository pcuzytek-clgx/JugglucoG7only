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
/*      Thu Apr 04 20:14:31 CEST 2024                                                 */


// Barcode/data-matrix scan entry point shared by all sensors (moved from sibionics/start.cpp).
#include "config.h"
#include <string_view>
#include "SensorGlucoseData.hpp"
#include "sensoren.hpp"
#include "fromjava.h"
#include "destruct.hpp"
#include "datbackup.hpp"
#include "scanstate.hpp"
#include "jniclass.hpp"
extern void    sendstreaming(SensorGlucoseData *hist);
extern Sensoren *sensors;

extern "C" JNIEXPORT jstring JNICALL   fromjava(addSIscangetName)(JNIEnv *env, jclass cl,jstring jgegs,jintArray jindexptr) {
  if(!jgegs) {
        LOGAR("addSIscangetName(null)");
        return nullptr;
        }
   const char *gegs = env->GetStringUTFChars( jgegs, NULL);
   if(!gegs) {
    LOGAR("addSIscangetName GetStringUTFChars()=null");
    return nullptr;
    }
   destruct   dest([jgegs,gegs,env]() {env->ReleaseStringUTFChars(jgegs, gegs);});
   const size_t gegslen= env->GetStringUTFLength( jgegs);
  std::string_view scangegs{gegs,gegslen};
   auto [sensindex,sens]= sensors->makePhotoScanSensorIndex(scangegs,time(nullptr));
   if(sens) {
      const char *name=sens->shortsensorname()->data();
      LOGGER("addSIscangetName(%s)=%s\n",gegs,name);
      sendstreaming(sens);  //TODO??
      backup->resendResetDevices();
      backup->wakebackup(wakeall);
      return env->NewStringUTF(name);
      }
   return nullptr;
   }
