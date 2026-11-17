package tk.glucodata;


import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.materialswitch.MaterialSwitch;

/* G7 fork: all former checkboxes are Material 3 switches (Android 17 settings style). */
public class CheckDirectionBox extends MaterialSwitch {
    public CheckDirectionBox(Context context) {
        super(context);
        init();
    }

    public CheckDirectionBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckDirectionBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSwitchPadding((int)(getResources().getDisplayMetrics().density*16));
        setTextSize(16);
    }
}
