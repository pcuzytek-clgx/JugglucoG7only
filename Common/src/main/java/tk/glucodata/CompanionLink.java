package tk.glucodata;

import static tk.glucodata.Log.doLog;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.IntentSender;
import android.widget.Toast;

/*
 * Optional CompanionDeviceManager association of the active sensor. An association makes the app a
 * companion app, which exempts it from background foreground-service start restrictions
 * (REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND / REQUEST_COMPANION_RUN_IN_BACKGROUND).
 * A sensor gets a new address, so associations of previous sensors are removed.
 */
final class CompanionLink {
    private static final String LOG_ID = "CompanionLink";
    static final int REQUEST_CODE = 0x4C4D;

    private CompanionLink() {}

    private static CompanionDeviceManager manager(Activity act) {
        return act.getSystemService(CompanionDeviceManager.class);
    }

    static void associate(Activity act, String address) {
        if (address == null) {
            Applic.argToaster(act, act.getString(R.string.companion_nosensor), Toast.LENGTH_LONG);
            return;
        }
        try {
            var cdm = manager(act);
            if (cdm == null) {
                Applic.argToaster(act, act.getString(R.string.companion_failed, "no CompanionDeviceManager"), Toast.LENGTH_LONG);
                return;
            }
            // Bonded sensors are matched directly; the LE filter covers a sensor that is only advertising.
            var request = new AssociationRequest.Builder()
                    .addDeviceFilter(new BluetoothDeviceFilter.Builder().setAddress(address).build())
                    .addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                            .setScanFilter(new ScanFilter.Builder().setDeviceAddress(address).build()).build())
                    .setSingleDevice(true)
                    .build();
            cdm.associate(request, act.getMainExecutor(), new CompanionDeviceManager.Callback() {
                @Override
                public void onAssociationPending(IntentSender sender) {
                    try {
                        act.startIntentSenderForResult(sender, REQUEST_CODE, null, 0, 0, 0);
                    } catch (Throwable th) {
                        Log.stack(LOG_ID, "startIntentSenderForResult", th);
                    }
                }

                @Override
                public void onAssociationCreated(AssociationInfo created) {
                    if (doLog) Log.i(LOG_ID, "associated " + created.getId());
                    try {
                        for (AssociationInfo old : cdm.getMyAssociations()) {
                            if (old.getId() != created.getId()) cdm.disassociate(old.getId());
                        }
                    } catch (Throwable th) {
                        Log.stack(LOG_ID, "disassociate", th);
                    }
                    Applic.argToaster(act, act.getString(R.string.companion_done), Toast.LENGTH_LONG);
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.i(LOG_ID, "onFailure " + error);
                    Applic.argToaster(act, act.getString(R.string.companion_failed, String.valueOf(error)), Toast.LENGTH_LONG);
                }
            });
        } catch (Throwable th) {
            Log.stack(LOG_ID, "associate", th);
            Applic.argToaster(act, act.getString(R.string.companion_failed, th.getMessage()), Toast.LENGTH_LONG);
        }
    }
}
