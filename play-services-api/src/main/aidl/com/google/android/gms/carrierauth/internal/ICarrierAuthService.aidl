package com.google.android.gms.carrierauth.internal;

import com.google.android.gms.carrierauth.internal.ICarrierAuthCallback;
import android.os.Bundle;

interface ICarrierAuthService {
    void getIccAuthentication(ICarrierAuthCallback callback, String packageName, int subId, String authAppId, String authData, in Bundle extras) = 0;
}
