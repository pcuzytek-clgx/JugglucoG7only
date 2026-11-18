package tk.glucodata;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Log.doLog;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationBarView;

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
private static MainActivity activity=null;
private static FrameLayout content=null;
private static BottomNavigationView bar=null;
private static boolean selecting=false;
private static FrameLayout alarmview=null;
private static boolean visible=false;
private static int current=GLUCOSE;
// Back stack checks posted while a tab is being opened are stale.
private static int generation=0;

private static int dp(float val) {
    return (int)(val*GlucoseCurve.metrics.density+.5f);
    }
private static int barHeight() {
    return dp(80);
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
    final int[] icons={R.drawable.ic_show_chart,R.drawable.ic_bar_chart,R.drawable.ic_bluetooth,R.drawable.ic_settings};
    bar=new BottomNavigationView(act);
    bar.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
    var menu=bar.getMenu();
    for(int i=0;i<names.length;i++)
        menu.add(Menu.NONE,i,i,names[i]).setIcon(icons[i]);
    // The app-wide textViewStyle adds paddingRight, which shifts the labels off the icons.
    clearPadding(bar);
    // Insets are handled below (the bar is placed in android.R.id.content which gets enlarged insets).
    ViewCompat.setOnApplyWindowInsetsListener(bar,(v,insets)->insets);
    bar.setOnItemSelectedListener(item-> {
        if(selecting)
            return true;
        select(item.getItemId());
        return item.getItemId()==current;
        });
    bar.setOnItemReselectedListener(item-> {
        if(!selecting)
            select(item.getItemId());
        });
    bar.setClickable(true);
    bar.setVisibility(View.GONE);
    // Z order keeps the bar above screens that call bringToFront().
    bar.setTranslationZ(dp(100));    content.addView(bar,new FrameLayout.LayoutParams(MATCH_PARENT,barHeight(),Gravity.BOTTOM));
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

private static void clearPadding(View v) {
    if(v instanceof android.widget.TextView)
        v.setPadding(0,v.getPaddingTop(),0,v.getPaddingBottom());
    else if(v instanceof android.view.ViewGroup g)
        for(int i=0;i<g.getChildCount();i++)
            clearPadding(g.getChildAt(i));
    }

static void detach(MainActivity act) {
    if(activity!=act)
        return;
    activity=null;
    content=null;
    bar=null;
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
    if(bar==null||bar.getSelectedItemId()==current)
        return;
    selecting=true;
    try {
        bar.setSelectedItemId(current);
        }
    finally {
        selecting=false;
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
                var page=new M3.Page(act,R.string.tabstatistics,false);
                page.group(null).text(act.getString(R.string.nostatsdata));
                page.show(null);
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
        cardback.setColor(M3.color(act,com.google.android.material.R.attr.colorSurfaceContainerHigh,0xFF2B2B2B));
        cardback.setCornerRadius(dp(28));
        card.setBackground(cardback);
        final int pad=dp(24);
        card.setPadding(pad,pad,pad,pad);
        var stop=new MaterialButton(act);
        stop.setText(R.string.stopalarmbutton);
        stop.setAllCaps(false);
        stop.setTextSize(TypedValue.COMPLEX_UNIT_SP,22);
        stop.setTextColor(M3.color(act,com.google.android.material.R.attr.colorOnError,Color.WHITE));
        stop.setBackgroundTintList(ColorStateList.valueOf(M3.color(act,androidx.appcompat.R.attr.colorError,0xFFD32F2F)));
        stop.setCornerRadius(dp(32));
        stop.setPadding(dp(32),dp(16),dp(32),dp(16));
        stop.setMinHeight(dp(72));
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
