// Sensor warmup JNI, moved from aidexx/java.cpp (AidexX support removed).
#include <stdint.h>
#include <jni.h>
#include "logs.hpp"
#include "SensorGlucoseData.hpp"
#include "fromjava.h"
#include "datbackup.hpp"

extern "C" JNIEXPORT jint JNICALL   fromjava(getManualWarmupMinutes)(JNIEnv *env, jclass cl,jlong dataptr) {
     if(!dataptr) {
          LOGAR("getManualWarmupMinute dataptr==0");
          return 60;
         }
     const SensorGlucoseData *sens=reinterpret_cast<const SensorGlucoseData *>(dataptr);
    return sens->getinfo()->manualwarmup;
    } 

extern "C" JNIEXPORT void JNICALL   fromjava(setManualWarmupMinutes)(JNIEnv *env, jclass cl,jlong sensorptr,jint min) {
     if(!sensorptr) {
          LOGAR("setManualWarmupMinute sensorptr==0");
          return;
         }
     SensorGlucoseData *sens=reinterpret_cast<SensorGlucoseData *>(sensorptr);
    sens->getinfo()->manualwarmup=min;
    if(sens->isAidexX()||sens->isLibre3()) {
        LOGGER("setManualWarmupMinutes(%d) warmupstartpos=%d\n",min,min);
        sens->getinfo()->warmupstartpos=min;
        }
    else {
        if(min==0) {
            LOGAR("setManualWarmupMinutes(0) warmupstartpos=0");
            sens->getinfo()->warmupstartpos=0;
            }
        else {
            const auto starttime=sens->getstarttime();
            const uint32_t endtime=starttime+min*60;
            const ScanData *polls=sens->beginpolls();
            const int count=sens->pollcount();
            const int nr=std::min(count,min+1);
            for(int it=1;it<nr;++it) {
                const ScanData &glu=polls[it];
                if(glu.valid(it)&&glu.t>endtime) {
                    LOGGER("setManualWarmupMinutes(%d) warmupstartpos=%d\n",min,it-1);
                    sens->getinfo()->warmupstartpos=it-1;
                    return;
                    }
                }
            }
        }
    } 


extern "C" JNIEXPORT jint JNICALL   fromjava(getMinimalWarmup)(JNIEnv *env, jclass cl,jlong dataptr) {
     if(!dataptr) {
          LOGAR("getMinimalWarmup dataptr==0");
          return 60;
         }
     const SensorGlucoseData *sens=reinterpret_cast<const SensorGlucoseData *>(dataptr);
     return sens->getMinimalWarmup();
     }
