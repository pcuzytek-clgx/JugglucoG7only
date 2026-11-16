#include "net/clarity/clarity.hpp"

#define setthreadname(buf) prctl(PR_SET_NAME, buf, 0, 0, 0)
/*#ifndef NOLOG
#define TESTGEN2 1
#endif       */
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
/*      Fri Jan 27 12:35:35 CET 2023                                                 */


//
// Created by jka on 27-11-20.
//
#include <cinttypes>
#include <algorithm>
#include <cmath>
#include <vector>
#include <string>
#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/prctl.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/uio.h>
#include <thread>
#include <future>
#include "calibrate/Calibrator.hpp"
extern "C" {
typedef void (*sighandler_t)(int);

sighandler_t bsd_signal(int signum, sighandler_t handler);
};
#define asignal signal
//#include <sys/syscall.h>
//#undef NOLOG
       #include "logs.hpp"
       #include "inout.hpp"
#include "logs.hpp"
#include "inout.hpp"
#include "serial.hpp"
#include "nfcdata.hpp"
#include "sensoren.hpp"
#include "timevalues.h"
#include "secs.h"
#include "streamdata.hpp"
#include "serial.hpp"
#include "fromjava.h"

#include "settings/settings.hpp"
#include "datbackup.hpp"
#include "error_codes.h"
#ifdef DYNLINK
#define abbottdec(x) (*x)
#define abbottcall(x) x
#else
#define abbottdec(x) Java_com_abbottdiabetescare_flashglucose_sensorabstractionservice_dataprocessing_DataProcessingNative_##x
#define abbottcall(x) Java_com_abbottdiabetescare_flashglucose_sensorabstractionservice_dataprocessing_DataProcessingNative_##x
#endif
extern jbyteArray getjtoken(JNIEnv *env);
extern "C" JNIEXPORT jlong JNICALL   abbottdec(getStatusCode)(JNIEnv *env, jobject obj, jstring serial, jint elapsed1, jint elapsed2, jint jint4, jboolean endedearly) ;

//extern string getstatus(JNIEnv *env, jobject thiz, const char *serial,time_t start,time_t lastread,time_t nu,bool endedearly) ;
void testserial(JNIEnv *env, jobject thiz) ;
#ifdef NDEBUG
#define EXTRA 50
#else
#define EXTRA 20
#endif
bool setbluetoothon=false;
extern                    void setusedsensors() ;
#include "debugclone.hpp"
extern int    timestr(char *buf,time_t tim) ;
extern lastscan_t  scantoshow;
//extern SensorGlucoseData  *usedhist;
//SensorGlucoseData  *usedhist;

extern Sensoren *sensors;

//#define fromjava(x) Java_tk_glucodata_Natives_ ##x
//extern data_t * unlockKeySensor(const SensorGlucoseData *usedhist) ;
 //public static native long getdataptr(String sensorname);
//int  nusensornr=0,maxsens=4;; SensorGlucoseData ** nusensors=new SensorGlucoseData *[maxsens];
extern "C" JNIEXPORT jlong JNICALL   fromjava(getsensorptr)(JNIEnv *env, jclass cl,jlong dataptr) {
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    if(!sdata) {
        
        return 0LL;
        }

    if(sdata->hist==Sensoren::isdeleted)  {
        LOGAR("getsensorptr isdeleted");
        return 0LL;
        }
    return reinterpret_cast<jlong>(sdata->hist);
    }
extern "C" JNIEXPORT jlong JNICALL   fromjava(streamfromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr,int pos) {
    if(!sensorptr)
        return 0LL;
    const auto *sens=reinterpret_cast<const SensorGlucoseData*>(sensorptr); 
    const ScanData *start= sens->beginpolls();
    const int len=sens->pollcount();
    auto cali=make_calibrator<ScanData>(sens);
    for(int i=pos;i<len;i++) {
        const ScanData *item=start+i;
        if(item->valid()) {
            for(++i;i<len&&!start[i].valid();i++)  {
                ;
                }
            long mgdL;
            if(double calibrated=cali.calibrateONEtest(*item);!isnan(calibrated)) {
                mgdL=(long)round(calibrated);
                }
             else
                mgdL=item->getmgdL();
            return ((jlong)item->gettime())|(((jlong)mgdL)<<32|((jlong)i)<<48);
            }
        }
    return ((jlong)len)<<48;
    }
void    sendstreaming(SensorGlucoseData *hist) {
    setstreaming(hist);
    backup->wakebackup(wakeall);
}
extern "C" JNIEXPORT jboolean JNICALL   fromjava(useAgain)(JNIEnv *env, jclass cl,jlong sensorptr) {
    if(!sensorptr) {
        return false;
        }
     auto *sens=reinterpret_cast<SensorGlucoseData*>(sensorptr);
     sens->useAgain();
     sensors->useAgain(sens->sensorIndex);
     sendKAuth(sens);
     sendsiScan(sens);
     sendstreaming(sens);  
     backup->resendResetDevices();
     backup->wakebackup(wakeall);
     if(settings->data()->nobluetooth)  {
         setusedsensors();
         return false;
         }
     return true;
     }
extern "C" JNIEXPORT void JNICALL   fromjava(setHidefromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr,jboolean hide) {
    if(!sensorptr)
        return;
    reinterpret_cast<SensorGlucoseData*>(sensorptr)->hide=hide;
     }
extern "C" JNIEXPORT jboolean JNICALL   fromjava(getHidefromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr) {
    if(!sensorptr) return false;
    return reinterpret_cast<const SensorGlucoseData*>(sensorptr)->hide;
     }
extern "C" JNIEXPORT jboolean JNICALL   fromjava(hasHistory)(JNIEnv *env, jclass cl,jlong sensorptr) {
    if(!sensorptr) return false;
    return reinterpret_cast<const SensorGlucoseData*>(sensorptr)->hasHistory();
     }

extern std::vector<int> usedsensors;
extern "C" JNIEXPORT jboolean JNICALL   fromjava(activeSensor)(JNIEnv *env, jclass cl,jlong sensorptr) {
    const int index= reinterpret_cast<const SensorGlucoseData*>(sensorptr)->sensorIndex;
    return ranges::find(usedsensors, index) != usedsensors.end();
     }
extern "C" JNIEXPORT void JNICALL   fromjava(healthConnectReset)(JNIEnv *env, jclass cl) {
    sensors->onallsensors([](SensorGlucoseData *sens) {
            auto *info=sens->getinfo();
            info->healthconnectiter=info->pollstart;
            }) ;

    }
extern "C" JNIEXPORT jint JNICALL   fromjava(healthConnectfromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr) {
    LOGGER("healthConnectfromSensorptr(%p)\n",sensorptr);
    if(!sensorptr)
        return 0;
    auto *info=reinterpret_cast<SensorGlucoseData*>(sensorptr)->getinfo();
    int start=info->healthconnectiter;
    if(!start) {
        info->healthconnectiter=start=info->pollstart;
        }
    auto res=start|((int)info->pollcount<<16);
    LOGGER("healthConnectfromSensorptr=%x\n",res);
    return res;
    }
extern "C" JNIEXPORT void JNICALL   fromjava(healthConnectWritten)(JNIEnv *env, jclass cl,jlong sensorptr,jint pos) {
    if(!sensorptr)
        return;
    reinterpret_cast<SensorGlucoseData*>(sensorptr)->getinfo()->healthconnectiter=pos;
    }
extern "C" JNIEXPORT jlong JNICALL   fromjava(getSensorStartmsec)(JNIEnv *env, jclass cl,jlong dataptr) {
    if(!dataptr)
        return 0LL;
    const streamdata *sdata=reinterpret_cast<const streamdata *>(dataptr);
    return sdata->hist->getstarttime()*1000LL; 
    }

extern "C" JNIEXPORT jint JNICALL   fromjava(getSerialLength)(JNIEnv *env, jclass cl,jlong dataptr) {
    if(!dataptr)
        return 0;
    const streamdata *sdata=reinterpret_cast<const streamdata *>(dataptr);
    return sdata->hist->getSerialLength(); 
    }

extern "C" JNIEXPORT jint JNICALL   fromjava(getmaxmgdL)(JNIEnv *env, jclass cl,jint sensorgen) {
    return SensorGlucoseData::getmaxmgdL(sensorgen);
    }
extern "C" JNIEXPORT jint JNICALL   fromjava(getminmgdL)(JNIEnv *env, jclass cl,jint sensorgen) {
    return SensorGlucoseData::getminmgdL(sensorgen);
    }



extern "C" JNIEXPORT void JNICALL   fromjava(updateUsedSensors)(JNIEnv *env, jclass cl) {
    setusedsensors();
    }

static void finishsensor(SensorGlucoseData*sensorptr,int sensorindex) {
    LOGGER("finishSensor %s\n",sensorptr->showsensorname().data());
    sensors->finishsensor(sensorindex);
    setstreaming(sensorptr); 
    setusedsensors();
    backup->wakebackup(wakeall);
    }
extern "C" JNIEXPORT void JNICALL   fromjava(finishSensor)(JNIEnv *env, jclass cl,jlong dataptr) {
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    if(!sdata) {
       LOGAR("finishSensor dataptr=null");
        return;
      }
    SensorGlucoseData* sensorptr=sdata->hist;
    int sensorindex=sdata->sensorindex;
    finishsensor(sensorptr,sensorindex);
    }
#ifdef SIBIONICS
extern bool siInit(bool);
#endif
extern "C" JNIEXPORT jlong JNICALL   fromjava(str2sensorptr)(JNIEnv *env, jclass cl,jstring jsensor) {
    if(!sensors) {
        LOGAR("ERROR: sensors==null");
        return 0LL;
      }
    constexpr const  int shortsensorlen=11;
    jint getlen= env->GetStringUTFLength( jsensor);
    if(getlen!=shortsensorlen) {
        LOGGER("sensorlen=%d\n",getlen);
        }    
    char sensor[shortsensorlen+1];
    env->GetStringUTFRegion( jsensor, 0,shortsensorlen, sensor);
    sensor[sizeof(sensor)-1]='\0';

    int sensorindex=sensors->sensorindexshort(sensor);
    if(sensorindex<0) {
      LOGGER("ERROR: %s unknown sensor\n",sensor);
      return 0LL;
      }
    SensorGlucoseData *sens= sensors->getSensorData(sensorindex);
    sens->sensorerror=false;
    return reinterpret_cast<jlong>(sens);
    }

extern "C" JNIEXPORT jlong JNICALL   fromjava(getSensorEndData)(JNIEnv *env, jclass cl,jstring jsensor) {
    if(!sensors) {
      LOGAR("ERROR: sensors==null");
      return 0LL;
      }
    constexpr const  int shortsensorlen=11;
    jint getlen= env->GetStringUTFLength( jsensor);
    if(getlen!=shortsensorlen) {
        LOGGER("sensorlen=%d\n",getlen);
        }    
    char sensor[shortsensorlen+1];
    env->GetStringUTFRegion( jsensor, 0,shortsensorlen, sensor);
    sensor[sizeof(sensor)-1]='\0';

    const SensorGlucoseData *sens= sensors->gethistshort(sensor);
    if(sens==nullptr) {
        LOGGER("Unknown sensor %s\n",sensor);
        return 0LL;
        }
    jlong show=(!(sens->pollcount()&&sens->isLibre3()>0));
    return sens->expectedEndTime()|show<<32;
    }
extern "C" JNIEXPORT jlong JNICALL   fromjava(getdataptr)(JNIEnv *env, jclass cl,jstring jsensor) {
    if(!sensors) {
      LOGAR("ERROR: sensors==null");
      return 0LL;
      }
    constexpr const  int shortsensorlen=11;
    jint getlen= env->GetStringUTFLength( jsensor);
    if(getlen!=shortsensorlen) {
        LOGGER("sensorlen=%d\n",getlen);
        if(getlen>shortsensorlen)
            getlen=shortsensorlen;
        }    
    char sensor[shortsensorlen+1];
    env->GetStringUTFRegion( jsensor, 0,getlen, sensor);
    sensor[getlen]='\0';

    int sensorindex=sensors->sensorindexshort(sensor);
    if(sensorindex<0) {
      LOGGER("ERROR: %s unknown sensor\n",sensor);
        return 0LL;
      }
    SensorGlucoseData *sens= sensors->getSensorData(sensorindex);
    sens->sensorerror=false;
    streamdata *data;
#ifdef SIBIONICS
    if(sens->isSibionics3()) {
      LOGGER("getdataptr(%s) isSibinics3%d\n",sensor);
      data= new si3stream(sens);
      }
  else 
    if(sens->isSibionics()) {
      LOGGER("getdataptr(%s) isSibinics newSI=%d\n",sensor,sens->newSI());
       if(!siInit(sens->newSI())) {
         LOGAR("siInit()==false");
         return 0LL;
         }
      data= new sistream(sensorindex,sens);
      }
  else 
#endif
    {
#ifdef DEXCOM
        if (sens->isDexcom()) {
            LOGGER("getdataptr(%s) Dexcom\n", sensor);
            data = new dexcomstream(sensorindex, sens);
        } else
#endif
        {
#ifdef LIBRE3
            if (sens->isLibre3()) {
                LOGGER("getdataptr(%s) Libre3\n", sensor);
                data = new libre3stream(sensorindex, sens);
            } else 
#endif

            {
                LOGGER("getdataptr(%s) unsupported sensor type\n", sensor);
                return 0LL;
            }
        }
    }
    if(data->good()) {
        LOGGER("getdataptr()=%p sens=%p\n",data,sens);
        return reinterpret_cast<jlong>(data);
        }
    LOGSTRING("getdataptr(): !data->good()\n");
    delete data;
    return 0LL;
    }
extern "C" JNIEXPORT void JNICALL   fromjava(freedataptr)(JNIEnv *envin, jclass cl,jlong dataptr) {
    LOGGER("start freedataptr(%p)\n",dataptr);
    if(!dataptr) {
        return;
        }
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    auto *sens= sdata->hist;
    while(sens->processing.test_and_set()) {
           LOGGER("freedataptr(%p) processing flag set\n",dataptr);
            sens->processing.wait(true);
            };
    delete sdata;
    sens->processing.clear();
    LOGGER("end freedataptr(%p)\n",dataptr);
    }
extern "C" JNIEXPORT jboolean  JNICALL   fromjava(askstreamingEnabled)(JNIEnv *env, jclass cl,jlong dataptr) {
   if(!dataptr) return false;
    auto *sens=reinterpret_cast<streamdata *>(dataptr)->hist;
    if(sens==Sensoren::isdeleted)  {
        LOGAR("askstreamingEnabled isdeleted");
        return false;
        }
    return sens->streamingIsEnabled()==1; 
    }

#ifdef SKIPTRIEDOFTEN
#ifdef NOLOG
static constexpr const int DEXTRYADDRESSSECS=60*15;
#else
static constexpr const int DEXTRYADDRESSSECS=60*5;
#endif
#endif
extern "C" JNIEXPORT void JNICALL  fromjava(setDeviceAddress)(JNIEnv *env, jclass cl,jlong dataptr,jstring jdeviceAddress ) {
    if(!dataptr)
        return;
    SensorGlucoseData *usedhist=reinterpret_cast<streamdata *>(dataptr)->hist ; 
    if(!usedhist)
        return;
   char *deviceaddress=usedhist->deviceaddress();
    if(!jdeviceAddress) {
          deviceaddress[0]='\0';
        } 
   else {
        const jint getlen= std::min(env->GetStringUTFLength( jdeviceAddress),17);
        env->GetStringUTFRegion(jdeviceAddress, 0,getlen,deviceaddress);
        deviceaddress[getlen]='\0';
       usedhist->scannedAddress=true;

#ifdef SKIPTRIEDOFTEN
       if(usedhist->isDexcom()) {
           auto &usedAddresses=usedhist->usedAddresses;
           if(usedAddresses.empty()||memcmp(usedAddresses.back().data(),deviceaddress,getlen+1)) {
                uint32_t now=time(nullptr);
                const auto &newel=*reinterpret_cast<const address_t*>(deviceaddress);
                if(!usedAddresses.empty()&&now<usedhist->usedAddressesTime) {
                        usedAddresses.back()=newel;
                        }
                else
                        usedAddresses.emplace_back(newel); 
                usedhist->usedAddressesTime=now+DEXTRYADDRESSSECS;
                }
             }
#endif
        }
   LOGGER("setDeviceAddress(%s)\n", deviceaddress);
   }
extern "C" JNIEXPORT int JNICALL   fromjava(getSensorptrLibreVersion)(JNIEnv *envin, jclass cl,jlong sensorptr) {
    const SensorGlucoseData *sens=reinterpret_cast<const SensorGlucoseData *>(sensorptr);
    if(!sens) {
        return -1;
        }
    return sens->getSensorgen2();
    }




extern "C" JNIEXPORT int JNICALL   fromjava(getLibreVersion)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!dataptr)
        return 0;
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    return sdata->libreversion;
    }
extern "C" JNIEXPORT jstring JNICALL   fromjava(getSensorName)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!dataptr)
        return nullptr;
    const SensorGlucoseData *usedhist=reinterpret_cast<streamdata *>(dataptr)->hist ; 
    if(!usedhist)
        return nullptr;
    const char *name=usedhist->shortsensorname()->data();
    LOGGER("getSensorName()=%s\n",name);
    return envin->NewStringUTF(name);
    }
extern "C" JNIEXPORT jstring JNICALL   fromjava(sensorptr2str)(JNIEnv *envin, jclass cl,jlong sensorptr) {
    if(!sensorptr)
        return nullptr;
    const SensorGlucoseData *usedhist=reinterpret_cast<const SensorGlucoseData *>(sensorptr); 
    const char *name=usedhist->shortsensorname()->data();
    LOGGER("getSensorName()=%s\n",name);
    return envin->NewStringUTF(name);
    }

   /*
extern "C" JNIEXPORT jstring JNICALL   fromjava(getUsedSensorName)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!dataptr)
        return nullptr;
    const SensorGlucoseData *usedhist=reinterpret_cast<streamdata *>(dataptr)->hist ; 
    if(!usedhist)
        return nullptr;
    const char *name=usedhist->shortsensorname()->data();
    LOGGER("getSensorName()=%s\n",name);
    return envin->NewStringUTF(name);
    }
extern "C" JNIEXPORT jstring JNICALL   fromjava(getShowSensorName)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!dataptr)
        return nullptr;
    const SensorGlucoseData *usedhist=reinterpret_cast<streamdata *>(dataptr)->hist ; 
    if(!usedhist)
        return nullptr;
    const char *name=usedhist->showsensorname()->data();
    LOGGER("getShowSensorName()=%s\n",name);
    return envin->NewStringUTF(name);
    } */

extern "C" JNIEXPORT jstring JNICALL   fromjava(getDeviceAddress)(JNIEnv *envin, jclass cl,jlong dataptr,jboolean getnew) {
    LOGGER("getDeviceAddress(%p,%d)\n",dataptr,getnew);
    if(!dataptr) {
      LOGAR("getDeviceAddress(null)");
      return nullptr;
      }
    const SensorGlucoseData *usedhist=reinterpret_cast<streamdata *>(dataptr)->hist ; 
    if(!usedhist) {
      LOGAR("getDeviceAddress() usedhist==null");
      return nullptr;
      }
    if(usedhist==Sensoren::isdeleted)  {
        LOGGER("getDeviceAddress()  %p deleted\n",usedhist);
        return nullptr;
        }
    if(!usedhist->getinfo()) {
      LOGAR("getDeviceAddress() getinfo()==null");
      return nullptr;
      }
    const char *address=usedhist->deviceaddress();
    if(!*address) {
      LOGAR("deviceaddress()==null");
      return nullptr;
      }
    if(usedhist->isAidexX()||usedhist->isAccuChek()||(getnew&&!usedhist->scannedAddress&&!usedhist->isLibre())) {
      LOGAR("getDeviceAddress() !libre getnew");
      return nullptr;
      }
    LOGGER("getDeviceAddress()=%s\n",address);
    return envin->NewStringUTF(address);
    }
#include "strsepconcat.hpp"
extern    strsepconcat getsensortext(const SensorGlucoseData *hist);
extern "C" JNIEXPORT jstring JNICALL   fromjava(getsensortext)(JNIEnv *envin, jclass cl,jlong dataptr) {
   const streamdata *str=reinterpret_cast<const streamdata *>(dataptr);
    const SensorGlucoseData *usedhist= str->hist;
    return envin->NewStringUTF(getsensortext(usedhist).data());
    }




extern "C" JNIEXPORT void JNICALL   fromjava(resetbluetooth)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!dataptr)
        return;
    SensorGlucoseData *usedhist= reinterpret_cast<streamdata *>(dataptr)->hist;
    if(!usedhist)
        return;
//    if(!usedhist->bluetoothfirst()||!waitstreaming())
    usedhist->setbluetoothOn(0);
    if(!backup)
        return;
    int maxint=backup->getupdatedata()->sendnr;
    LOGGER("resetbluetooth %d\n",maxint);
    usedhist->sendbluetoothOn(maxint);
    backup->wakebackup(wakestream);
    }

extern "C" JNIEXPORT jbyteArray JNICALL   fromjava(getsensorident)(JNIEnv *envin, jclass cl,jlong dataptr) { 
    streamdata *  streamptr=reinterpret_cast<streamdata *>(dataptr);
    if(!dataptr)
        return nullptr;
    if(streamptr->libreversion!=2)
        return nullptr;
    SensorGlucoseData *usedhist=streamptr->hist ; 
    if(!usedhist)
        return nullptr;
    jbyteArray uit=envin->NewByteArray(8);
    envin->SetByteArrayRegion(uit, 0, 8, usedhist->getsensorident()->data());
    return uit;
    }
extern "C" JNIEXPORT jint JNICALL   fromjava(getLibre2sensorgen)(JNIEnv *envin, jclass cl,jlong dataptr) { 
    streamdata * const streamptr=reinterpret_cast<streamdata *>(dataptr);
    if(!dataptr)
        return -1;
    const SensorGlucoseData *usedhist=streamptr->hist ; 
    if(!usedhist)
        return -1;
    return usedhist->getsensorgen();
    }

extern "C" JNIEXPORT jbyteArray JNICALL   fromjava(getstreamingAuthenticationData)(JNIEnv *envin, jclass cl,jlong dataptr) { 
    streamdata * const streamptr=reinterpret_cast<streamdata *>(dataptr);
    LOGGER("getstreamingAuthenticationData %p\n",streamptr);
        setthreadname("getstreamingAuthenticationData"  );
    if(!dataptr)
        return nullptr;
    if(streamptr->libreversion!=2)
        return nullptr;
    const SensorGlucoseData *usedhist=streamptr->hist ; 
    if(!usedhist)
        return nullptr;
    const auto auth=usedhist->getinfo()->getauth();
    const auto authlen=auth.size();
    jbyteArray uit=envin->NewByteArray(authlen);
    envin->SetByteArrayRegion(uit, 0, authlen,(const jbyte *)auth.data());
    return uit;
    }
/*
extern time_t *glucosetime;
extern float *glucoserate;
//extern  float *glucose;
extern  uint32_t *glucose;
//typedef const char **sensorname;
typedef const sensorname_t *constcharptr_t;
extern constcharptr_t *sensorname;
*/

//extern streams_t laststream;
//extern data_t *fromjbyteArray(JNIEnv *env,jbyteArray jar,jint len=-1) ;
constexpr const jlong isHighest=4LL<<48;
constexpr const jlong isHigh=6LL<<48;
constexpr const jlong isLow=7LL<<48;
constexpr const jlong isAgain=3LL<<48;
constexpr const jlong isLowest=5LL<<48;

constexpr const jlong isVeryHigh=16LL<<48;
constexpr const jlong isVeryLow=17LL<<48;

constexpr const jlong isPreHigh=18LL<<48;
constexpr const jlong isPreLow=19LL<<48;

#include "gluconfig.hpp"
extern void setbuffer(char *);

static jlong getalarmonly(const uint32_t mgL,float drate,const SensorGlucoseData *hist) {
    const int glucoselowestmgL=hist->getminmgdL()*10;
    auto res=mgL<glucoselowestmgL?isLowest:(mgL>(hist->getmaxmgdL()*10)?isHighest:
 (settings->veryhighAlarm(mgL)?isVeryHigh:(settings->highAlarm(mgL)?isHigh:(settings->verylowAlarm(mgL)?isVeryLow:(settings->lowAlarm(mgL)?isLow:(
settings->prelowAlarm(mgL,drate)?isPreLow:
(settings->prehighAlarm(mgL,drate)?isPreHigh:
(settings->availableAlarm()&&hist->waiting?isAgain:0))))))));
    LOGGER("mgL=%u, high=%d, low=%d res=%" PRId64 "\n",mgL,settings->highAlarm(mgL),settings->lowAlarm(mgL),res);
    return res;
    }
int getalarmcode(const uint32_t mgL,float drate,SensorGlucoseData *hist) {
    int res= getalarmonly(mgL,drate,hist)>>48;
    hist->waiting=false;
    return res;
    }
float threshold(float drate)  {
    return glnearnull(drate)?0.0f:drate;
    }
extern "C" JNIEXPORT jfloat JNICALL   fromjava(thresholdchange)(JNIEnv *envin, jclass cl,jfloat drate) {
    return threshold(drate);
    }

static jlong glucoselong(uint32_t nu,uint32_t glval,float drate,const SensorGlucoseData *hist) {
        if(!glval) 
            return 0LL;
        const jlong rate=roundl(((long double)drate)*1000LL);
        auto calibrate= make_calibrator<ScanData>(hist);
        const double cali=calibrate.calibrateNow(nu,glval);
        uint32_t mgL;
        if(!isnan(cali)) {
            mgL=(uint32_t)round(cali*10.0);
            }
        else 
            mgL=glval*10;

        const jlong alarmcode= getalarmonly(mgL,drate,hist);
        const jlong res= (rate&0xFFFF)<<32|alarmcode|mgL;
        LOGGER("glucoselong=%" PRId64 "\n",res);
        return res;
        }
//extern const SensorGlucoseData * getlaststream(const uint32_t);
extern std::pair<const SensorGlucoseData *,int> getlaststream(const uint32_t nu) ;
extern "C" JNIEXPORT jlongArray JNICALL   fromjava(getlastGlucose)(JNIEnv *env, jclass cl) {
    auto nu=time(nullptr);
    if(const auto [hist,_]=getlaststream(nu);hist) {
        const ScanData *poll=hist->lastValidStream();
        if(poll) {
            jlong uit[2];
            uit[0]=poll->gettime();
            uit[1]=glucoselong(poll->gettime(),poll->getmgdL(),poll->getchange(),hist);
            jlongArray juit=env->NewLongArray(2);
            env->SetLongArrayRegion(juit, 0, 2,uit );
            return juit;
            }
        }
    return nullptr;
    }
// Emulator reading snapshots. Each record: timestamp, raw mg/L, rate * 100,
// original Libre 3 life count (-1 for other sources), original start time.
// Never apply the phone's calibration or substitute stream values for history.
static void emulatorAppend(std::vector<jlong> &values,jlong time,jlong mgL,jlong rate,jlong minute,jlong start) {
    if(mgL<390 || mgL>5010 || values.size()>=32768*5) return;
    values.insert(values.end(),{time,mgL,rate,minute,start});
}
static bool emulatorInRange(jlong time,jlong minute,jlong start,jlong from,jlong through) {
    const jlong nominal=minute>=0?start+minute*60:time;
    return nominal>=from && nominal<=through;
}
static jlongArray emulatorArray(JNIEnv *env,const std::vector<jlong> &values) {
    jlongArray result=env->NewLongArray(static_cast<jsize>(values.size()));
    if(result && !values.empty()) env->SetLongArrayRegion(result,0,static_cast<jsize>(values.size()),values.data());
    return result;
}
extern "C" JNIEXPORT jlongArray JNICALL fromjava(libre3EmulatorHistory)(JNIEnv *env,jclass,jlong from,jlong through) {
    if(from<0 || through<from) return env->NewLongArray(0);
    std::vector<jlong> values;
    if(const auto [hist,index]=getlaststream(time(nullptr));hist) {
        const jlong start=hist->getstarttime();
        const int end=std::min(hist->getAllendhistory(),hist->maxpos());
        for(int pos=std::max(0,hist->getstarthistory());pos<end;pos++) {
            const Glucose &item=*hist->getglucose(pos);
            const jlong minute=hist->isLibre3()?static_cast<jlong>(item.getid()):-1;
            if(!item.valid() || !emulatorInRange(item.gettime(),minute,start,from,through)) continue;
            emulatorAppend(values,item.gettime(),item.getmgL(),0,minute,start);
        }
    }
    return emulatorArray(env,values);
}
extern "C" JNIEXPORT jlongArray JNICALL fromjava(libre3EmulatorStream)(JNIEnv *env,jclass,jlong from,jlong through) {
    if(from<0 || through<from) return env->NewLongArray(0);
    std::vector<jlong> values;
    if(const auto [hist,index]=getlaststream(time(nullptr));hist) {
        const jlong start=hist->getstarttime();
        for(const auto &item:hist->getPolldata()) {
            const jlong minute=hist->isLibre3()?static_cast<jlong>(item.getid()):-1;
            // valid(0) avoids the timestamp repair performed by valid() on gaps.
            if(!item.valid(0) || !emulatorInRange(item.gettime(),minute,start,from,through)) continue;
            emulatorAppend(values,item.gettime(),item.getmgL(),0,minute,start);
        }
    }
    return emulatorArray(env,values);
}
extern "C" JNIEXPORT jlongArray JNICALL fromjava(libre3EmulatorLatest)(JNIEnv *env,jclass) {
    std::vector<jlong> values;
    if(const auto [hist,index]=getlaststream(time(nullptr));hist) {
        if(const ScanData *item=hist->lastValidStream()) {
            const float rate=item->getchange();
            const jlong change=std::isfinite(rate)?static_cast<jlong>(std::max(-32767.0,std::min(32767.0,round(rate*100.0)))):-32768;
            emulatorAppend(values,item->gettime(),item->getmgL(),change,
                hist->isLibre3()?static_cast<jlong>(item->getid()):-1,hist->getstarttime());
        }
    }
    return emulatorArray(env,values);
}
jlong glucoseback(uint32_t nu,uint32_t glval,float drate,SensorGlucoseData *hist) {
        if(!glval) return 0LL;
        hist->setbluetoothOn(1);
        auto res= glucoselong(nu,glval,drate,hist);
        hist->waiting=false;
        settings->data()->lossSignalOff=false;
        return res;
        }


void wakewithcurrent() {
    wakeclarity();
    }


    /*
extern "C" JNIEXPORT void JNICALL   fromjava(backupstream)(JNIEnv *envin, jclass cl,jlong dataptr) {
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    backup->updatestream(sdata->hist);
    }
    */
extern "C" JNIEXPORT jstring JNICALL   fromjava(lastsensorname)(JNIEnv *envin, jclass cl) {
    if(const auto *name=sensors->shortsensorname()->data() ) {
        return envin->NewStringUTF(name);
        }
    return nullptr;
    }
extern "C" JNIEXPORT jlong JNICALL   fromjava(laststarttime)(JNIEnv *envin, jclass cl) {
    return sensors->laststarttime();
    }

extern void setusedsensors() ;
/*
extern "C" JNIEXPORT jboolean  JNICALL   fromjava(hasSibionics)(JNIEnv *env, jclass cl) {
    setusedsensors();
    const int len= usedsensors.size();
     for(int i=0;i<len;i++) {
         const int index=usedsensors[i];
      if(sensors->isSibionics(index))
            return true;
          }
   return false;
    }  */
extern "C" JNIEXPORT jboolean  JNICALL   fromjava(hasNeedScan)(JNIEnv *env, jclass cl) {
    setusedsensors();
    const int len= usedsensors.size();
     for(int i=0;i<len;i++) {
         const int index=usedsensors[i];
         if(sensors->needsScan(index))
                return true;
          }
   return false;
    } 
extern "C" JNIEXPORT jlongArray JNICALL   fromjava(activeSensorPtrs)(JNIEnv *env, jclass cl) {
    setusedsensors();
    const int len= usedsensors.size();
     jlong longar[len]; 
    LOGGER("activeSensorPtrs  len=%d\n",len);
    for(int i=0;i<len;i++) {
         int index=usedsensors[i];
         const SensorGlucoseData *sens=sensors->getSensorData(index );
         longar[i]=reinterpret_cast<jlong>(sens);
          }
    jlongArray    ptrAr = env->NewLongArray(len);
    env->SetLongArrayRegion(ptrAr, 0, len, longar);
    return ptrAr;
    } 

extern "C" JNIEXPORT jstring JNICALL   fromjava(namefromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr) {
    const auto *sens=reinterpret_cast<const SensorGlucoseData*>(sensorptr); 
    return env->NewStringUTF(sens->shortsensorname()->data());
    }

extern "C" JNIEXPORT jstring JNICALL   fromjava(sensortextfromSensorptr)(JNIEnv *envin, jclass cl,jlong sensorptr) {
    const auto *sens=reinterpret_cast<const SensorGlucoseData*>(sensorptr); 
    return envin->NewStringUTF(getsensortext(sens).data());
    }

extern "C" JNIEXPORT void JNICALL   fromjava(finishfromSensorptr)(JNIEnv *env, jclass cl,jlong sensorptr) {
    auto *sens=reinterpret_cast<SensorGlucoseData*>(sensorptr);
    const int sensorindex=sensors->sensorindex(sens->sensorname()->data());
    finishsensor(sens,sensorindex);
    }
extern jclass JNIString;
#ifdef LIBRE3
extern "C" JNIEXPORT jobjectArray  JNICALL   fromjava(activeSensors)(JNIEnv *env, jclass cl) {
    setusedsensors();
    const int len= usedsensors.size();
    jobjectArray  sensjar = env->NewObjectArray(len,JNIString,nullptr);
    
    LOGGER("activeSensors libre3 len=%d\n",len);
     for(int i=0;i<len;i++) {
         int index=usedsensors[i];
         const char *name=sensors->shortsensorname(index)->data();
//       const char *name=sensors->showsensorname(index); //gives problems in getdataptr
         LOGGER("active: %s\n",name);
         env->SetObjectArrayElement(sensjar,i,env->NewStringUTF(name));
         }

    return sensjar;
    } 
#else
extern "C" JNIEXPORT jobjectArray  JNICALL   fromjava(activeSensors)(JNIEnv *env, jclass cl) {
    LOGSTRING("activeSensors oldlibre\n");
    setusedsensors();
    const int len= usedsensors.size();
    const char *names[len];
    int uitlen=0;
     for(int i=0;i<len;i++) {
          int index=usedsensors[i];
          const SensorGlucoseData *sens=sensors->getSensorData(index );
          if(sens&&!sens->isLibre3())
              names[uitlen++]=sensors->shortsensorname(index)->data();
          }
    jobjectArray  sensjar = env->NewObjectArray(uitlen,JNIString,nullptr);
    for(int i=0;i<uitlen;i++)
         env->SetObjectArrayElement(sensjar,i,env->NewStringUTF(names[i]));

    return sensjar;
    } 
#endif

//extern "C" JNIEXPORT void JNICALL   fromjava(saveState)(JNIEnv *envin, jclass cl,jlong dataptr) {

/*
extern "C" JNIEXPORT void JNICALL   fromjava(setwaiting)(JNIEnv *envin, jclass cl,jlong dataptr,jboolean val) {
    streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
    SensorGlucoseData *usedhist=sdata->hist ;
    usedhist->waiting=val;
    }
extern "C" JNIEXPORT jboolean JNICALL   fromjava(iswaiting)(JNIEnv *envin, jclass cl,jlong dataptr) {
    if(!settings->availableAlarm())
        return false;
    else {
        streamdata *sdata=reinterpret_cast<streamdata *>(dataptr);
        const SensorGlucoseData *usedhist=sdata->hist ; 
        return usedhist->waiting;
        }
    }
    */

#if defined(__aarch64__)
#define archstring "arm64-v8a"
#elif defined(__x86_64__)
#define archstring "x86_64"
#elif defined(__i386__)
#define archstring "x86"
#else //defined(__arm__)
#define archstring "armeabi-v7a"
#endif


extern "C" JNIEXPORT jstring  JNICALL   fromjava(getLibraryName)(JNIEnv *env, jclass cl) {
    return env->NewStringUTF("lib/" archstring "/libDataProcessing.so");
    }
extern "C" JNIEXPORT jstring  JNICALL   fromjava(getCPUarch)(JNIEnv *env, jclass cl) {
    return env->NewStringUTF(archstring);
    }

/*
#include "destruct.hpp"

extern "C" JNIEXPORT jstring  JNICALL   fromjava(chmod)(JNIEnv *env, jclass cl,jstring filename,jint mode) {
    const char *str = env->GetStringUTFChars( filename, NULL);
    if (str == nullptr) return nullptr; 
    destruct   dest([filename,str,env]() {env->ReleaseStringUTFChars(filename, str);});
    if(chmod(str,mode)!=0) {
        const int errn=errno;
        return env->NewStringUTF(strerror(errn));
        }
    return nullptr;    
    }

extern "C" JNIEXPORT void  JNICALL   fromjava(startbackup)(JNIEnv *env, jclass cl) {
    if(backup ) {
        setthreadname( "Backup");
        backup->backupthread();
        }
    }*/

extern "C" JNIEXPORT jboolean  JNICALL   fromjava(sameSensor)(JNIEnv *env, jclass _cl,jlong one,jlong two) {
    if(one==0LL||two==0LL)
        return false;
    streamdata *sone=reinterpret_cast<streamdata *>(one);
    streamdata *stwo=reinterpret_cast<streamdata *>(two);
    if(sone->sensorindex==stwo->sensorindex)
        return true;
    return false;
    }

void    setstreaming(SensorGlucoseData *hist) {
        LOGSTRING("setstreaming(SensorGlucoseData)\n");
        if(!backup)
            return;
        int maxint=backup->getupdatedata()->sendnr;
        if(maxint>maxsendtohost) {
            maxint=maxsendtohost;
             backup->getupdatedata()->sendnr=maxint;
            }
        if(maxint>0)
            hist->setsendstreaming( maxint) ;
        }
void    sendKAuth(SensorGlucoseData *hist) {
        LOGSTRING("setsendKauth(SensorGlucoseData)\n");
        if(!backup)
            return;
        int maxint=backup->getupdatedata()->sendnr;
        hist->setsendKAuth( maxint) ;
        }

void    sendsiScan(SensorGlucoseData *hist) {
        int maxint=backup->getupdatedata()->sendnr;
        hist->setsiScan( maxint) ;
        }





extern "C" JNIEXPORT void  JNICALL   fromjava(reenableStreaming)(JNIEnv *env, jclass _cl) {
    setbluetoothon=true;
    }

extern "C" JNIEXPORT jboolean  JNICALL   fromjava(hasstreamed)(JNIEnv *env, jclass _cl) {
    if(sensors)
        return sensors->hasstream();
    return false;
    }
extern "C" JNIEXPORT jboolean  JNICALL   fromjava(hasscans)(JNIEnv *env, jclass _cl) {
    if(sensors)
        return sensors->hasscans();
    return false;
    }

extern std::string_view getdeltaname(float rate);
extern "C" JNIEXPORT jstring JNICALL   fromjava(getxDripTrendName)(JNIEnv *envin, jclass cl,jfloat rate) {
    return envin->NewStringUTF(getdeltaname(rate).data());
    }
extern "C" JNIEXPORT jint JNICALL   fromjava(getinfogen)(JNIEnv *env, jclass _cl, jbyteArray jinfo) {
    jsize lens=env->GetArrayLength(jinfo);
    char info[lens];
    env->GetByteArrayRegion(jinfo, 0, lens, reinterpret_cast<jbyte *>(info));
    int gen= SensorGlucoseData::getgeneration(info);

    #ifndef NOLOG
    const char label[]="getinfogen ";
    auto labellen=sizeof(label)-1;
    int totlen=labellen+lens*3+4;
    char mess[totlen];
    memcpy(mess,label,labellen);
    int pos=labellen;
    for(int i=0;i<lens;i++) {
        pos+=sprintf(mess+pos,"%02X ",info[i]);
        }
    pos+=sprintf(mess+pos,"=%d",gen);
    LOGGERN(mess,pos);
    #endif

    return gen;
    }


extern "C" JNIEXPORT jboolean JNICALL   fromjava(hasData)(JNIEnv *env, jclass _cl) {
   return sensors->last()>=0||settings->getlabelcount()>0;
   }


extern "C" JNIEXPORT jboolean  JNICALL   fromjava(optionStreamHistory)(JNIEnv *env, jclass cl,jlong dataptr) {
#if defined(__aarch64__) 
   return false;
#else
    const streamdata *sdata=reinterpret_cast<const streamdata *>(dataptr);
    if(!sdata) {
        return !settings->data()->nobluetooth;
        }
    if(sdata->libreversion>2)
        return false;
    const SensorGlucoseData* sensorptr=sdata->hist;
    if(sensorptr==Sensoren::isdeleted)  {
        LOGAR("optionStreamHistory  isdeleted");
        return false;
        }
    return !sensorptr->useLibre2rootcheck();
#endif
   }

extern "C" JNIEXPORT jint JNICALL   fromjava(sensorIndex)(JNIEnv *env, jclass cl,jstring jsensor) {
    if(!sensors) {
        LOGAR("knownSensor ERROR: sensors==null");
        return -1;
      }
    constexpr const  int shortsensorlen=11;
    jint getlen= env->GetStringUTFLength( jsensor);
    if(getlen!=shortsensorlen) {
        LOGGER("knownSensor sensorlen=%d\n",getlen);
        }    
    char sensor[shortsensorlen+1];
    env->GetStringUTFRegion( jsensor, 0,shortsensorlen, sensor);
    sensor[sizeof(sensor)-1]='\0';

    int sensorindex=sensors->sensorindexshort(sensor);
    if(sensorindex<0) {
      LOGGER("knownSensor ERROR: %s unknown sensor\n",sensor);
      }
    return sensorindex;
    }
