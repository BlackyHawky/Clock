// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import com.best.deskclock.R;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QrCaptureActivity extends CaptureActivity {

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.zxing_capture_qr);
        return findViewById(R.id.zxing_barcode_scanner);
    }
}
