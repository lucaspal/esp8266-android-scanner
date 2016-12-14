package dk.au.grp5.ti_ipwi_mini_project;

/**
 * Created by Luca on 12/11/2016.
 */

public class Helper {
    public static String formatIpAddress(int ip) {
        String ipString = String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));

        return ipString;
    }

    public static String formatData(String ssid, String pw) {
        return ssid + ";" + pw;
    }
}
