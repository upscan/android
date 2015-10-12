package io.upscan.android;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;

/**
 *
 */
public interface BarcodeDetectionListener {

    void onBarcodeDetected(Detector.Detections<Barcode> detectionResults, Barcode item);
}
