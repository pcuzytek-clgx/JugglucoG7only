package tk.glucodata;

public class Libre3GattCallback extends SuperGattCallback {
    public Libre3GattCallback(String name, long dataptr) {
        super(name,dataptr,0);
    }
    public static byte[] getSavedGarminProvisioningSecret(String serial) { return null; }
    public static boolean saveGarminLibre3Minute(String serial,byte[] decr,long timmsec) { return false; }
    public static boolean saveGarminLibre3Clinical(String serial,byte[] decr) { return false; }
}

