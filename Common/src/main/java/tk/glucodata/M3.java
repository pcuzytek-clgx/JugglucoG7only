package tk.glucodata;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * G7 fork: builder for Android 17 (Material 3 Expressive) style settings pages:
 * a large title, sections with a coloured header and "segmented" rounded rows
 * (24dp outer corners, 4dp inner corners, 2dp gaps), switches on the right and
 * chevrons for sub screens.
 *
 * Usage:
 * <pre>
 *  var page=new M3.Page(act,R.string.display,true);
 *  var g=page.group(R.string.something);
 *  g.nav(R.string.colors,null,v->...);
 *  g.toggle(someCheckDirectionBox);
 *  g.value(R.string.threshold,editText);
 *  page.buttons(helpButton);
 *  page.show(()->{ save... });   // registers MainActivity.setonback, removes the page on back
 * </pre>
 */
public final class M3 {
    private M3() {}

    public static int dp(Context c, float v) {
        return (int)(c.getResources().getDisplayMetrics().density*v+.5f);
    }

    public static int color(Context c, int attr, int fallback) {
        return MaterialColors.getColor(c, attr, fallback);
    }
    public static int surface(Context c) { return color(c, com.google.android.material.R.attr.colorSurface, 0xFF121212); }
    public static int itemColor(Context c) { return color(c, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0xFF2B2B2B); }
    public static int onSurface(Context c) { return color(c, com.google.android.material.R.attr.colorOnSurface, 0xFFE6E6E6); }
    public static int onSurfaceVariant(Context c) { return color(c, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFFC4C4C4); }
    public static int primary(Context c) { return color(c, androidx.appcompat.R.attr.colorPrimary, 0xFF9ECAFF); }

    public static TextView title(Context c, CharSequence text) {
        var t=new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTextColor(onSurface(c));
        t.setPadding(0,0,0,0);
        return t;
    }
    public static TextView summary(Context c, CharSequence text) {
        var t=new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(onSurfaceVariant(c));
        t.setPadding(0,0,0,0);
        return t;
    }

    /** Filled (primary) pill button. */
    public static MaterialButton filled(Context c, CharSequence text) {
        var b=new MaterialButton(c);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }
    /** Tonal (secondary container) pill button, the default for actions in pages. */
    public static MaterialButton tonal(Context c, CharSequence text) {
        var b=filled(c,text);
        return tonal(b);
    }
    public static MaterialButton tonal(MaterialButton b) {
        var c=b.getContext();
        b.setBackgroundTintList(ColorStateList.valueOf(color(c, com.google.android.material.R.attr.colorSecondaryContainer, 0xFF3A4A5A)));
        b.setTextColor(color(c, com.google.android.material.R.attr.colorOnSecondaryContainer, 0xFFE0E0E0));
        return b;
    }
    /** Turns any existing Button into a tonal M3 look (used for buttons created with new Button()). */
    public static Button style(Button b) {
        if(b instanceof MaterialButton mb)
            return tonal(mb);
        var c=b.getContext();
        var bg=new GradientDrawable();
        bg.setCornerRadius(dp(c,40));
        bg.setColor(color(c, com.google.android.material.R.attr.colorSecondaryContainer, 0xFF3A4A5A));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),bg,null));
        b.setTextColor(color(c, com.google.android.material.R.attr.colorOnSecondaryContainer, 0xFFE0E0E0));
        b.setAllCaps(false);
        int h=dp(c,24), v=dp(c,10);
        b.setPadding(h,v,h,v);
        b.setMinHeight(dp(c,48));
        return b;
    }

    static void detach(View v) {
        ViewParent p=v.getParent();
        if(p instanceof ViewGroup g)
            g.removeView(v);
    }

    /** A full screen settings page. */
    public static final class Page {
        public final MainActivity act;
        public final FrameLayout root;
        public final NestedScrollView scroll;
        public final LinearLayout list;
        private final int side;

        public Page(MainActivity act, int titleRes, boolean back) {
            this(act, act.getString(titleRes), back);
        }
        public Page(MainActivity act, CharSequence title, boolean back) {
            this.act=act;
            Context c=act;
            side=dp(c,16);
            root=new FrameLayout(c);
            root.setBackgroundColor(surface(c));
            root.setClickable(true);
            root.setFocusable(true);
            scroll=new NestedScrollView(c);
            scroll.setFillViewport(true);
            scroll.setClipToPadding(false);
            scroll.setVerticalScrollBarEnabled(false);
            root.addView(scroll,new FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT));
            list=new LinearLayout(c);
            list.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(list,new FrameLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            scroll.setPadding(MainActivity.systembarLeft+side, MainActivity.systembarTop, MainActivity.systembarRight+side, MainActivity.systembarBottom+dp(c,16));

            if(back) {
                var arrow=new ImageView(c);
                arrow.setImageResource(R.drawable.baseline_arrow_back_24);
                arrow.setImageTintList(ColorStateList.valueOf(onSurface(c)));
                int p=dp(c,12);
                arrow.setPadding(p,p,p,p);
                var ripple=new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),null,new GradientDrawable());
                ((GradientDrawable)ripple.getDrawable(0)).setShape(GradientDrawable.OVAL);
                arrow.setBackground(ripple);
                arrow.setContentDescription(c.getString(R.string.closename));
                arrow.setOnClickListener(v-> MainActivity.doonback());
                var lp=new LinearLayout.LayoutParams(dp(c,48),dp(c,48));
                lp.topMargin=dp(c,8);
                lp.setMarginStart(-dp(c,4));
                list.addView(arrow,lp);
            }
            var head=new TextView(c);
            head.setText(title);
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
            head.setTextColor(onSurface(c));
            head.setPadding(dp(c,4), back?dp(c,16):dp(c,56), 0, dp(c,20));
            list.addView(head,new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
        }

        public Group group(int headerRes) { return group(act.getString(headerRes)); }
        /** New section; header may be null. */
        public Group group(CharSequence header) {
            Context c=act;
            if(header!=null) {
                var h=new TextView(c);
                h.setText(header);
                h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
                h.setTextColor(primary(c));
                h.setPadding(dp(c,16), dp(c,20), dp(c,16), dp(c,8));
                list.addView(h,new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            }
            else
                spacer(12);
            var g=new Group(c);
            list.addView(g,new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            return g;
        }

        /** Row of buttons below the groups, aligned to the end. */
        public LinearLayout buttons(View... views) {
            Context c=act;
            var row=new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            for(var v:views) {
                if(v==null) continue;
                detach(v);
                if(v instanceof Button b && !(v instanceof CompoundButton))
                    style(b);
                var lp=new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT);
                lp.setMarginStart(dp(c,8));
                row.addView(v,lp);
            }
            var lp=new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT);
            lp.topMargin=dp(c,20);
            list.addView(row,lp);
            return row;
        }

        /** Secondary text outside of groups. */
        public TextView note(CharSequence text) {
            var t=summary(act,text);
            t.setPadding(dp(act,16),dp(act,12),dp(act,16),0);
            list.addView(t,new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            return t;
        }
        public void add(View v) {
            detach(v);
            list.addView(v,new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
        }
        public void spacer(int dpHeight) {
            var s=new View(act);
            list.addView(s,new LinearLayout.LayoutParams(MATCH_PARENT,dp(act,dpHeight)));
        }

        /** Adds the page full screen and registers onback: the page is removed and onclose (may be null) runs. */
        public Page show(Runnable onclose) {
            act.addMyContentView(root,new ViewGroup.LayoutParams(MATCH_PARENT,MATCH_PARENT));
            MainActivity.setonback(()-> {
                tk.glucodata.settings.Settings.removeContentView(root);
                if(onclose!=null)
                    onclose.run();
            });
            return this;
        }
        /** Adds the page full screen without registering onback (for callers managing back themselves). */
        public Page showOnly() {
            act.addMyContentView(root,new ViewGroup.LayoutParams(MATCH_PARENT,MATCH_PARENT));
            return this;
        }
        public void remove() {
            tk.glucodata.settings.Settings.removeContentView(root);
        }
    }

    /** A group of segmented rows. Corners are recomputed for the visible children. */
    public static final class Group extends LinearLayout {
        private final int big, small, gap, itemcolor;
        public Group(Context c) {
            super(c);
            setOrientation(VERTICAL);
            big=dp(c,24);
            small=dp(c,4);
            gap=dp(c,2);
            itemcolor=itemColor(c);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int n=getChildCount();
            int first=-1,last=-1;
            for(int i=0;i<n;i++) {
                if(getChildAt(i).getVisibility()!=GONE) {
                    if(first<0) first=i;
                    last=i;
                }
            }
            for(int i=0;i<n;i++) {
                var ch=getChildAt(i);
                if(ch.getVisibility()==GONE) continue;
                int top=i==first?big:small, bottom=i==last?big:small;
                int key=(top<<16)|bottom;
                Object old=ch.getTag(R.id.m3_corners);
                if(!(old instanceof Integer oi)||oi!=key) {
                    ch.setTag(R.id.m3_corners,key);
                    ch.setBackground(background(top,bottom,ch.isClickable()));
                }
                var lp=(LayoutParams)ch.getLayoutParams();
                int want=i==first?0:gap;
                if(lp.topMargin!=want) {
                    lp.topMargin=want;
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private Drawable background(int top, int bottom, boolean clickable) {
            var shape=new GradientDrawable();
            shape.setColor(itemcolor);
            shape.setCornerRadii(new float[]{top,top,top,top,bottom,bottom,bottom,bottom});
            if(!clickable)
                return shape;
            var mask=new GradientDrawable();
            mask.setColor(0xFFFFFFFF);
            mask.setCornerRadii(new float[]{top,top,top,top,bottom,bottom,bottom,bottom});
            return new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF),shape,mask);
        }

        private LinearLayout row() {
            Context c=getContext();
            var r=new LinearLayout(c);
            r.setOrientation(HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setMinimumHeight(dp(c,64));
            r.setPadding(dp(c,20),dp(c,14),dp(c,16),dp(c,14));
            return r;
        }
        private LinearLayout texts(CharSequence title, CharSequence summary) {
            Context c=getContext();
            var col=new LinearLayout(c);
            col.setOrientation(VERTICAL);
            col.addView(M3.title(c,title));
            if(!TextUtils.isEmpty(summary)) {
                var s=M3.summary(c,summary);
                s.setTag("summary");
                col.addView(s);
            }
            return col;
        }

        private View addRow(View r) {
            addView(r,new LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            return r;
        }

        public View nav(int titleRes, CharSequence summary, View.OnClickListener l) {
            return nav(getContext().getString(titleRes),summary,l);
        }
        /** Row that opens a sub screen (chevron at the end). */
        public View nav(CharSequence title, CharSequence summary, View.OnClickListener l) {
            Context c=getContext();
            var r=row();
            r.addView(texts(title,summary),new LinearLayout.LayoutParams(0,WRAP_CONTENT,1f));
            var chev=new ImageView(c);
            chev.setImageResource(R.drawable.baseline_navigate_next_24);
            chev.setImageTintList(ColorStateList.valueOf(onSurfaceVariant(c)));
            r.addView(chev,new LinearLayout.LayoutParams(dp(c,24),dp(c,24)));
            r.setClickable(true);
            r.setFocusable(true);
            r.setOnClickListener(l);
            return addRow(r);
        }
        /** Clickable row without chevron (performs an action). */
        public View action(CharSequence title, CharSequence summary, View.OnClickListener l) {
            var r=row();
            r.addView(texts(title,summary),new LinearLayout.LayoutParams(0,WRAP_CONTENT,1f));
            r.setClickable(true);
            r.setFocusable(true);
            r.setOnClickListener(l);
            return addRow(r);
        }
        public View action(int titleRes, CharSequence summary, View.OnClickListener l) {
            return action(getContext().getString(titleRes),summary,l);
        }
        /** Turns an existing button into a navigation row with the same text and click listener. */
        public View nav(Button b) {
            var r=nav(b.getText(),null,v->b.performClick());
            r.setVisibility(b.getVisibility());
            r.setEnabled(b.isEnabled());
            return r;
        }

        /** Existing switch (e.g. CheckDirectionBox) as a full width row; its text is the title. */
        public CompoundButton toggle(CompoundButton sw) {
            return toggle(sw,null);
        }
        public CompoundButton toggle(CompoundButton sw, CharSequence summary) {
            Context c=getContext();
            detach(sw);
            if(summary==null) {
                sw.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
                sw.setTextColor(onSurface(c));
                sw.setMinHeight(dp(c,64));
                sw.setPadding(dp(c,20),dp(c,10),dp(c,16),dp(c,10));
                if(sw instanceof MaterialSwitch ms)
                    ms.setSwitchPadding(dp(c,16));
                addView(sw,new LayoutParams(MATCH_PARENT,WRAP_CONTENT));
                return sw;
            }
            var r=row();
            var title=sw.getText();
            sw.setText(null);
            r.addView(texts(title,summary),new LinearLayout.LayoutParams(0,WRAP_CONTENT,1f));
            r.addView(sw,new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT));
            r.setClickable(true);
            r.setOnClickListener(v->sw.toggle());
            addRow(r);
            return sw;
        }

        public View value(int titleRes, View... controls) {
            return value(getContext().getString(titleRes),controls);
        }
        /** Title at the start, controls (EditText, Spinner, labels, buttons) at the end. */
        public View value(CharSequence title, View... controls) {
            Context c=getContext();
            var r=row();
            r.addView(texts(title,null),new LinearLayout.LayoutParams(0,WRAP_CONTENT,1f));
            for(var v:controls) {
                if(v==null) continue;
                detach(v);
                if(v instanceof TextView tv && !(v instanceof Button) && !(v instanceof android.widget.EditText))
                    tv.setTextColor(onSurfaceVariant(c));
                if(v instanceof Button b && !(v instanceof CompoundButton))
                    style(b);
                var lp=new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT);
                lp.setMarginStart(dp(c,8));
                r.addView(v,lp);
            }
            return addRow(r);
        }

        /** Arbitrary content inside a row (padded). */
        public View item(View v) {
            Context c=getContext();
            detach(v);
            var f=new FrameLayout(c);
            f.setPadding(dp(c,20),dp(c,12),dp(c,16),dp(c,12));
            f.addView(v,new FrameLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            return addRow(f);
        }
        /** Row containing several views horizontally (e.g. buttons), aligned to the end. */
        public View buttons(View... views) {
            Context c=getContext();
            var r=row();
            r.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            for(var v:views) {
                if(v==null) continue;
                detach(v);
                if(v instanceof Button b && !(v instanceof CompoundButton))
                    style(b);
                var lp=new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT);
                lp.setMarginStart(dp(c,8));
                r.addView(v,lp);
            }
            return addRow(r);
        }
        /** Plain informative text row. */
        public TextView text(CharSequence t) {
            var tv=M3.summary(getContext(),t);
            item(tv);
            return tv;
        }
    }
}
