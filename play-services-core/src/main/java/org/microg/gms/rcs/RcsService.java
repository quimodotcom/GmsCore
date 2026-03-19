/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.rcs.internal.IRcsConfigCallback;
import com.google.android.gms.rcs.internal.IRcsService;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RcsService extends BaseService {
    private static final String TAG = "RcsService";

    public RcsService() {
        super(TAG, GmsService.RCS);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, new RcsServiceImpl(this, packageName).asBinder(), null);
    }

    private static class RcsServiceImpl extends IRcsService.Stub {
        private final Context context;
        private final String packageName;

        public RcsServiceImpl(Context context, String packageName) {
            this.context = context;
            this.packageName = packageName;
        }

        @Override
        public void getRcsConfig(IRcsConfigCallback callback, String packageName, String msisdn, Bundle extras) throws RemoteException {
            Log.d(TAG, "getRcsConfig: packageName=" + packageName + " msisdn=" + msisdn);
            new Thread(() -> {
                try {
                    String config = performAcsRequest(msisdn, extras);
                    callback.onResult(Status.SUCCESS, config);
                } catch (Exception e) {
                    Log.w(TAG, "getRcsConfig failed", e);
                    try {
                        callback.onResult(new Status(CommonStatusCodes.INTERNAL_ERROR, e.getMessage()), null);
                    } catch (RemoteException ignored) {}
                }
            }).start();
        }

        private String performAcsRequest(String msisdn, Bundle extras) throws Exception {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = tm.getSimOperator();
            if (simOperator == null || simOperator.length() < 5) {
                throw new Exception("Invalid SIM operator");
            }

            String mcc = simOperator.substring(0, 3);
            String mnc = simOperator.substring(3);
            String imsi = tm.getSubscriberId();
            String imei = tm.getImei();

            String acsUrl = "https://config.rcs.mnc" + mnc + ".mcc" + mcc + ".jibecloud.net/";

            Uri.Builder uriBuilder = Uri.parse(acsUrl).buildUpon()
                    .appendQueryParameter("vers", "0")
                    .appendQueryParameter("rcs_state", "0")
                    .appendQueryParameter("IMSI", imsi != null ? imsi : "")
                    .appendQueryParameter("IMEI", imei != null ? imei : "")
                    .appendQueryParameter("terminal_model", "Pixel 3")
                    .appendQueryParameter("terminal_vendor", "Goog")
                    .appendQueryParameter("terminal_sw_version", "12")
                    .appendQueryParameter("client_vendor", "Goog")
                    .appendQueryParameter("client_version", "20240603-01.01")
                    .appendQueryParameter("rcs_profile", "UP_T")
                    .appendQueryParameter("rcs_version", "5.1B")
                    .appendQueryParameter("token", extras.getString("token", ""))
                    .appendQueryParameter("SMS_port", "0");

            if (msisdn != null && !msisdn.isEmpty()) {
                uriBuilder.appendQueryParameter("msisdn", msisdn);
            }

            if (extras.containsKey("otp")) {
                uriBuilder.appendQueryParameter("OTP", extras.getString("otp"));
            }

            URL url = new URL(uriBuilder.build().toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "IM-client/OMA1.0 Google/Pixel_3-14 Goog/messages.android_20240603_01_rc01");
            conn.setRequestProperty("client_channel", "PUBLIC");
            if (msisdn != null && !msisdn.isEmpty()) {
                conn.setRequestProperty("msisdn_source", "msisdn_source_sim");
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 511) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return response.toString();
            } else {
                throw new Exception("ACS request failed with code: " + responseCode);
            }
        }
    }
}
