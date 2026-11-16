package tk.glucodata;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Log.doLog;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tk.glucodata.settings.Settings;

/* G7 fork: Dexcom-app-like bottom tab bar (Glukoza | Statystyki | Połączenia | Ustawienia).
 * The bar stays on top of every screen.  Screens keep out of it because the
 * bottom inset dispatched inside android.R.id.content is enlarged by the bar
 * height (MainActivity.systembarBottom and the native graph see the same value).
 * Also hosts the "Stop alarm" popup, shown on top of everything while an alarm rings. */
public final class DexTabs {
private static final String LOG_ID="DexTabs";
public static final int GLUCOSE=0,STATISTICS=1,CONNECTIONS=2,SETTINGS=3;
private static final int BARCOLOR=0xFF262626;
private static final int TEXTCOLOR=0xFFE0E0E0;
private static final int SELECTEDCOLOR=0xFFE0E0E0;
private static final int SELECTEDTEXT=0xFF1A1A1A;

private static MainActivity activity=null;
private static FrameLayout content=null;
private static LinearLayout bar=null;
private static TextView[] labels=null;
private static FrameLayout alarmview=null;
private static boolean visible=false;
private static int current=GLUCOSE;
// Back stack checks posted while a tab is being opened are stale.
private static int generation=0;

private static int dp(float val) {
    return (int)(val*GlucoseCurve.metrics.density+.5f);
    }
private static int barHeight() {
    return dp(64);
    }

public static boolean isShown() {
    return visible;
    }
/* Several screens use only part of the bottom inset; the tab bar needs all of it. */
public static int bottomPart(int bottom) {
    return visible?bottom:bottom*3/4;
    }

static void attach(MainActivity act) {
    activity=act;
    content=act.findViewById(android.R.id.content);
    final int[] names={R.string.tabglucose,R.string.tabstatistics,R.string.connections,R.string.settings};
    bar=new LinearLayout(act);
    bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.setBackgroundColor(BARCOLOR);
    bar.setClickable(true);
    bar.setVisibility(View.GONE);
    // Z order keeps the bar above screens that call bringToFront().
    bar.setOutlineProvider(null);
    bar.setTranslationZ(dp(100));
    labels=new TextView[names.length];
    for(int i=0;i<names.length;i++) {
        final int tab=i;
        var cell=new FrameLayout(act);
        cell.setOnClickListener(v->select(tab));
        var label=new TextView(act);
        label.setText(names[i]);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(14),dp(6),dp(14),dp(6));
        cell.addView(label,new FrameLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT,Gravity.CENTER));
        labels[i]=label;
        bar.addView(cell,new LinearLayout.LayoutParams(0,MATCH_PARENT,1.0f));
        }
    content.addView(bar,new FrameLayout.LayoutParams(MATCH_PARENT,barHeight(),Gravity.BOTTOM));
    current=Menus.on?CONNECTIONS:GLUCOSE;
    showSelected();
    ViewCompat.setOnApplyWindowInsetsListener(content,(v,insets)-> {
        final Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
        final boolean show=wantsBar();
        if(show!=visible) {
            visible=show;
            bar.setVisibility(show?View.VISIBLE:View.GONE);
            if(doLog) {Log.i(LOG_ID,"visible="+show);};
            }
        if(!show)
            return insets;
        var params=bar.getLayoutParams();
        final int height=barHeight()+bars.bottom;
        if(params.height!=height) {
            params.height=height;
            bar.setLayoutParams(params);
            }
        bar.setPadding(bars.left,0,bars.right,bars.bottom);
        final Insets nav=insets.getInsets(WindowInsetsCompat.Type.navigationBars());
        return new WindowInsetsCompat.Builder(insets).setInsets(WindowInsetsCompat.Type.navigationBars(),
            Insets.of(nav.left,nav.top,nav.right,bars.bottom+barHeight())).build();
        });
    ViewCompat.requestApplyInsets(content);
    if(Natives.getisalarm())
        showAlarm(true);
    }

static void detach(MainActivity act) {
    if(activity!=act)
        return;
    activity=null;
    content=null;
    bar=null;
    labels=null;
    alarmview=null;
    visible=false;
    }

/* Same condition as the native Dexcom layout (JCurve::dexPhoneLayout). */
private static boolean wantsBar() {
    if(activity==null||!MainActivity.graphUsesCurrentOrientationAsLandscape)
        return false;
    Rect bounds=activity.getWindowManager().getCurrentWindowMetrics().getBounds();
    return bounds.height()>bounds.width()*1.4f;
    }

/* Re-evaluate visibility after the graph orientation mode changed. */
static void refresh() {
    if(content!=null)
        ViewCompat.requestApplyInsets(content);
    }

/* Called after a screen is added to the content view: keep bar and alarm popup on top. */
static void toFront() {
    if(bar!=null)
        bar.bringToFront();
    if(alarmview!=null)
        alarmview.bringToFront();
    }

private static void showSelected() {
    if(labels==null)
        return;
    for(int i=0;i<labels.length;i++) {
        final var label=labels[i];
        if(i==current) {
            var pill=new GradientDrawable();
            pill.setColor(SELECTEDCOLOR);
            pill.setCornerRadius(dp(16));
            label.setBackground(pill);
            label.setTextColor(SELECTEDTEXT);
            label.setTypeface(null,Typeface.BOLD);
            }
        else {
            label.setBackground(null);
            label.setTextColor(TEXTCOLOR);
            label.setTypeface(null,Typeface.NORMAL);
            }
        }
    }

/* A tab screen closed by system Back leaves an empty back stack: the graph is shown again. */
static void backStackChanged() {
    if(bar==null)
        return;
    final int startgen=generation;
    bar.post(()-> {
        if(startgen==generation&&current!=GLUCOSE&&!(current==CONNECTIONS&&Menus.on)&&MainActivity.onbacknr()==0) {
            current=GLUCOSE;
            showSelected();
            }
        });
    }

private static void closeAll(MainActivity act) {
    Menus.on=false;
    var imm=(InputMethodManager)act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
    if(imm!=null&&content!=null)
        imm.hideSoftInputFromWindow(content.getWindowToken(),0);
    for(int i=0;i<30&&act.backinapp();i++)
        ;
    MainActivity.clearonback();
    }

static void select(int tab) {
    final MainActivity act=activity;
    if(act==null)
        return;
    if(doLog) {Log.i(LOG_ID,"select "+tab);};
    closeAll(act);
    switch(tab) {
        case STATISTICS:
            if(!Natives.makepercentages()) {
                tab=GLUCOSE;
                act.requestRender();
                break;
                }
            Stats.mkstats(act);
            act.requestRender();
            break;
        case CONNECTIONS:
            Menus.show(act);
            break;
        case SETTINGS:
            Settings.set(act);
            break;
        default:
            tab=GLUCOSE;
            Natives.dexShowNow();
            act.requestRender();
        }
    current=tab;
    ++generation;
    showSelected();
    }

public static void showAlarm(boolean on) {
    new Handler(Looper.getMainLooper()).post(()-> {
        final MainActivity act=activity;
        if(act==null||content==null)
            return;
        if(!on) {
            if(alarmview!=null) {
                content.removeView(alarmview);
                alarmview=null;
                }
            return;
            }
        if(alarmview!=null)
            return;
        var scrim=new FrameLayout(act);
        scrim.setBackgroundColor(0x99000000);
        scrim.setClickable(true);
        var card=new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        var cardback=new GradientDrawable();
        cardback.setColor(BARCOLOR);
        cardback.setCornerRadius(dp(24));
        card.setBackground(cardback);
        final int pad=dp(24);
        card.setPadding(pad,pad,pad,pad);
        var stop=new Button(act);
        stop.setText(R.string.stopalarmbutton);
        stop.setAllCaps(false);
        stop.setTextSize(TypedValue.COMPLEX_UNIT_SP,22);
        stop.setTextColor(Color.WHITE);
        var red=new GradientDrawable();
        red.setColor(0xFFD32F2F);
        red.setCornerRadius(dp(32));
        stop.setBackground(red);
        stop.setPadding(dp(32),dp(16),dp(32),dp(16));
        stop.setMinHeight(dp(64));
        stop.setOnClickListener(v->Notify.stopalarm());
        card.addView(stop,new LinearLayout.LayoutParams(dp(260),WRAP_CONTENT));
        scrim.addView(card,new FrameLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT,Gravity.CENTER));
        scrim.setOutlineProvider(null);
        scrim.setTranslationZ(dp(200));
        alarmview=scrim;
        content.addView(scrim,new FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT));
        });
    }
}
