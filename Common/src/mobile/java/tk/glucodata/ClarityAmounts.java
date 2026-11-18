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

// SPDX-License-Identifier: GPL-3.0-or-later
package tk.glucodata;

import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.ArrayList;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.settings.Settings.removeContentView;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getlabel;
import android.widget.TextView;


final class ClarityAmounts {
    private static final String LOG_ID="ClarityAmounts";
    private static final int[] names={R.string.clarity_category_choose,R.string.clarity_category_fast,
        R.string.clarity_category_long,R.string.clarity_category_carbs,R.string.clarity_category_comment,
        R.string.clarity_category_not,R.string.clarity_category_blood};

    static boolean ready() {
        int[] kinds=Natives.clarityCategories();
        if(kinds==null || kinds.length==0)return false;
        for(int i=0;i<kinds.length;++i) {
            if(kinds[i]<1 || kinds[i]>6)return false;
            if(kinds[i]==3) {
                float weight=Natives.getlibrefoodweight(0,i);
                if(Float.isNaN(weight) || Float.isInfinite(weight) || weight<=0)return false;
            }
        }
        return true;
    }

    static void show(MainActivity act,View parent,Runnable saved) {
        final int[] kinds=Natives.clarityCategories();
        final ArrayList<String> labels=Natives.getLabels();
        if(kinds==null || labels==null || labels.size()!=kinds.length+1) {
            Log.e(LOG_ID,"Cannot read amount labels/categories");
            Applic.Toaster(act.getString(R.string.clarity_read_labels_error));
            return;
        }
        final float[] weights=new float[kinds.length];
        final Runnable[] redraw={null};
        final TextView[] summaries=new TextView[kinds.length];
        var save=getbutton(act,R.string.save);
        var page=new M3.Page(act,R.string.clarity_categories,true);
        var group=page.group(R.string.m3_share_clarity_labels);
        redraw[0]=()->{
            boolean complete=kinds.length>0;
            for(int i=0;i<kinds.length;++i) {
                int kind=kinds[i];
                String text=act.getString(names[kind]);
                if(kind==3)text=act.getString(R.string.clarity_category_carbs_weight,text,Float.toString(weights[i]));
                summaries[i].setText(text);
                if(kind==0)complete=false;
            }
            save.setEnabled(complete);
            save.setAlpha(complete?1f:.38f);
           };
        for(int i=0;i<kinds.length;++i) {
            final int pos=i;

            weights[i]=Natives.getlibrefoodweight(0,i);
            if(kinds[i]==3 && (Float.isNaN(weights[i]) || Float.isInfinite(weights[i]) || weights[i]<=0))
                kinds[i]=0;
            var button=group.action(labels.get(i)," ",null);
            summaries[i]=button.findViewWithTag("summary");
            button.setOnClickListener(v->{
                String[] choices=new String[names.length-1];
                for(int j=0;j<choices.length;++j)choices[j]=act.getString(names[j+1]);
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(act).setTitle(labels.get(pos))
                    .setSingleChoiceItems(choices,kinds[pos]-1,(dialog,which)->{
                        int chosen=which+1;
                        if(chosen==6) {
                            for(int j=0;j<kinds.length;++j)
                                if(j!=pos && kinds[j]==6) {
                                    Applic.Toaster(act.getString(R.string.clarity_change_blood_first,labels.get(j)));
                                    return;
                                }
                        }
                        dialog.dismiss();
                        if(chosen==3) {
                            var weight=new EditText(act);
                            weight.setSingleLine(true);
                            weight.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            float initial=weights[pos];
                            weight.setText(initial>0 && !Float.isInfinite(initial)?Float.toString(initial):"1");
                            var editor=new com.google.android.material.dialog.MaterialAlertDialogBuilder(act).setTitle(act.getString(R.string.clarity_grams_per_unit,labels.get(pos)))
                                .setView(weight).setNegativeButton(R.string.cancel,null)
                                .setPositiveButton(R.string.save,null).create();
                            editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
                                try {
                                    float factor=Float.parseFloat(weight.getText().toString().trim().replace(',','.'));
                                    if(Float.isNaN(factor) || Float.isInfinite(factor) || factor<=0)
                                        throw new NumberFormatException();
                                    weights[pos]=factor;
                                    kinds[pos]=3;
                                    redraw[0].run();
                                    editor.dismiss();
                                } catch(NumberFormatException ex) {
                                    Log.e(LOG_ID,"Invalid carbohydrate category weight");
                                    weight.setError(act.getString(R.string.clarity_positive_number));
                                }
                            }));
                            editor.show();
                        } else {
                            kinds[pos]=chosen;
                            redraw[0].run();
                        }
                    }).setNegativeButton(R.string.cancel,null).show();
            });
        }

        redraw[0].run();

        save.setOnClickListener(v->{
            // A concurrent label edit must not silently reassign these indexes.
            if(!labels.equals(Natives.getLabels())) {
                Log.e(LOG_ID,"Labels changed while Categories was open");
                Applic.Toaster(act.getString(R.string.clarity_labels_changed));
                return;
            }
            String error=Natives.claritySetCategories(kinds,weights);
            if(error==null || !error.isEmpty()) {
                Applic.Toaster(error==null?act.getString(R.string.clarity_save_categories_error):error);
                return;
            }
            act.doonback();
            saved.run();
        });
        parent.setVisibility(View.GONE);
        page.buttons(save);
        page.show(()->parent.setVisibility(View.VISIBLE));
    }
}
