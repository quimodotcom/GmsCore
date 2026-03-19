/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.carrierauth.internal.ICarrierAuthCallback;
import com.google.android.gms.carrierauth.internal.ICarrierAuthService;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

public class CarrierAuthService extends BaseService {
    private static final String TAG = "CarrierAuthService";

    public CarrierAuthService() {
        super(TAG, GmsService.CARRIER_AUTH);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, new CarrierAuthServiceImpl(this, packageName).asBinder(), null);
    }

    private static class CarrierAuthServiceImpl extends ICarrierAuthService.Stub {
        private final Context context;
        private final String packageName;

        public CarrierAuthServiceImpl(Context context, String packageName) {
            this.context = context;
            this.packageName = packageName;
        }

        @Override
        public void getIccAuthentication(ICarrierAuthCallback callback, String packageName, int subId, String authAppId, String authData, Bundle extras) throws RemoteException {
            Log.d(TAG, "getIccAuthentication: subId=" + subId + " authAppId=" + authAppId);
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                callback.onResult(new Status(CommonStatusCodes.INTERNAL_ERROR, "TelephonyManager not available"), null);
                return;
            }

            try {
                // Determine authentication type (EAP-SIM or EAP-AKA)
                int authType = TelephonyManager.AUTHTYPE_EAP_AKA;
                if (authData.length() <= 32) {
                    authType = TelephonyManager.AUTHTYPE_EAP_SIM;
                }

                String response = tm.getIccAuthentication(authType, authType, authData);
                callback.onResult(Status.SUCCESS, response);
            } catch (Exception e) {
                Log.w(TAG, "getIccAuthentication failed", e);
                callback.onResult(new Status(CommonStatusCodes.INTERNAL_ERROR, e.getMessage()), null);
            }
        }
    }
}
