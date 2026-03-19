package com.google.android.gms.rcs.internal;

import com.google.android.gms.common.api.Status;

interface IRcsConfigCallback {
    void onResult(in Status status, String configXml) = 0;
}
