package com.google.android.gms.carrierauth.internal;

import com.google.android.gms.common.api.Status;

interface ICarrierAuthCallback {
    void onResult(in Status status, String response) = 0;
}
