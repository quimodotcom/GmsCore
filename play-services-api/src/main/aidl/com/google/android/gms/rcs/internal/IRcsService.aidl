package com.google.android.gms.rcs.internal;

import com.google.android.gms.rcs.internal.IRcsConfigCallback;
import android.os.Bundle;

interface IRcsService {
    void getRcsConfig(IRcsConfigCallback callback, String packageName, String msisdn, in Bundle extras) = 0;
}
