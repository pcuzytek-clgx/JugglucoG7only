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
/*      Sun Apr 16 20:56:20 CEST 2023                                                 */


package tk.glucodata.NovoPen;

import static tk.glucodata.Log.doLog;
import static tk.glucodata.Log.showbytes;
import static tk.glucodata.ScanNfcV.failure;
import static tk.glucodata.ScanNfcV.getvibrator;
import static tk.glucodata.ScanNfcV.startvibration;

import android.nfc.Tag;

import androidx.appcompat.app.AlertDialog;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tk.glucodata.Applic;
import tk.glucodata.Log;
import tk.glucodata.MainActivity;
import tk.glucodata.NovoPen.opennov.OpContext;
import tk.glucodata.NovoPen.opennov.OpenNov;
import tk.glucodata.R;

// G7-only: doses read from a NovoPen are only shown, never stored or drawn.
public class Scan {
    static final private String LOG_ID="Scan";
    static final private int SHOWN_DOSES=10;
    static final private int DOSE_SIZE=12;

    static public void onTag(MainActivity act, Tag tag) {
        {if(doLog){showbytes("onTag", tag.getId());};}
        var vibrator = getvibrator(act);
        startvibration(vibrator);
        var openNov = new OpenNov();
        var op = openNov.processTag(tag);
        vibrator.cancel();
        if (op == null) {
            failure(vibrator);
            Log.e(LOG_ID, "processTag failed");
        } else if (op.specification == null) {
            Log.e(LOG_ID, "op.specification==null");
        } else if (op.doses == null) {
            Log.e(LOG_ID, "op.doses==null");
        } else {
            Applic.RunOnUiThread(() -> showDoses(act, op));
            return;
        }
        Applic.Toaster(act.getString(R.string.penfailed));
    }

    record Dose(long time, float units) {}

    private static int u8(byte b) { return b&0xFF; }

    /* Same record layout and validity checks as the former native savedoses():
       reltime[4] big endian, sign1 (0x00FF), units[2] big endian *0.1, sign2 (low 24 bits == 8). */
    static List<Dose> parseDoses(List<OpContext.Doses> blocks,long nowsec) {
        var all=new ArrayList<Dose>();
        final long old=nowsec-100L*24*60*60;
        for(var block:blocks) {
            final byte[] raw=block.rawdoses;
            if(raw==null)
                continue;
            for(int off=0;off+DOSE_SIZE<=raw.length;off+=DOSE_SIZE) {
                if(u8(raw[off+4])!=0xFF||raw[off+5]!=0||raw[off+8]!=8||raw[off+9]!=0||raw[off+10]!=0)
                    continue;
                long rel=((long)u8(raw[off])<<24)|(u8(raw[off+1])<<16)|(u8(raw[off+2])<<8)|u8(raw[off+3]);
                long time=block.referencetime+rel;
                float units=((u8(raw[off+6])<<8)|u8(raw[off+7]))*0.1f;
                if(time<old||time>nowsec||!(units>0.0f)||units>60.0f)
                    continue;
                all.add(new Dose(time,units));
            }
        }
        all.sort((a,b)->Long.compare(b.time,a.time));
        var result=new ArrayList<Dose>();
        boolean used=false;
        long nexttime=0;
        for(var dose:all) {
            if(used&&dose.units<=2.0f&&(nexttime-dose.time)<60) {
                // priming shot just before a real dose
                used=false;
                continue;
            }
            used=true;
            nexttime=dose.time;
            result.add(dose);
            if(result.size()>=SHOWN_DOSES)
                break;
        }
        return result;
    }

    // Stop reading the pen as soon as enough doses for the list have been received.
    public static boolean enoughDoses(List<OpContext.Doses> doses) {
        return doses!=null&&parseDoses(doses,System.currentTimeMillis()/1000L).size()>=SHOWN_DOSES;
    }

    static void showDoses(MainActivity context, OpContext op) {
        final String serial=op.specification.getSerial();
        final var doses=parseDoses(op.doses,System.currentTimeMillis()/1000L);
        final String message;
        if(doses.isEmpty()) {
            message=context.getString(R.string.nodoses);
        } else {
            var format=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
            var unit=context.getString(R.string.insulinunit);
            var sb=new StringBuilder();
            for(var dose:doses) {
                if(sb.length()>0)
                    sb.append('\n');
                sb.append(format.format(dose.time*1000L)).append("   ")
                  .append(String.format(Locale.getDefault(),"%.1f",dose.units)).append(' ').append(unit);
            }
            message=sb.toString();
        }
        new AlertDialog.Builder(context)
            .setTitle("NovoPen "+serial)
            .setMessage(message)
            .setPositiveButton(R.string.ok,null)
            .show();
    }
}