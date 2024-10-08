package de.hernan.qu1ckr00t;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
//import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hernan.qu1ckr00t.R;
import de.hernan.qu1ckr00t.DeviceInfo;

public class MainActivity extends Activity {

    Button rootButton;
    String pocPath;
    String magiskInstPath;
    String magiskPath;
    TextView textView;
    ScrollView scrollView;
    TextView deviceInfo;

    private ExecutorService executorService;
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootButton = findViewById(R.id.button); // No casting needed
        textView = findViewById(R.id.textView2); // No casting needed
        deviceInfo = findViewById(R.id.deviceInfo); // No casting needed
        scrollView = findViewById(R.id.scrollView2); // No casting needed

        executorService = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(getMainLooper());

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        addLabel(ssb, getString(R.string.device_label),
                String.format("%s (Android %s)", DeviceInfo.getDeviceName(), DeviceInfo.getAndroidVersion()));
        addLabel(ssb, getString(R.string.kernel_label),
                String.format("%s (%s)", DeviceInfo.getKernelVersion(), DeviceInfo.getDeviceArchitecture()));
        addLabel(ssb, getString(R.string.patch_label), DeviceInfo.getAndroidPatchLevel());
        addLabel(ssb, getString(R.string.fingerprint_label), DeviceInfo.getBuildFingerprint());

        deviceInfo.setText(ssb);
        textView.setMovementMethod(new ScrollingMovementMethod());

        rootButton.setOnClickListener(v -> {
            rootButton.setText(getString(R.string.rooting_text));
            addStatus(getString(R.string.starting_root_process));
            rootButton.setClickable(false);
            rootButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            startRootingProcess();
        });
    }

    private static void addLabel(SpannableStringBuilder ssb, String label, String text) {
        int start = ssb.length();
        ssb.append(label).append(": ");
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, ssb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        ssb.append(text).append("\n");
    }

    private void addStatus(String status) {
        textView.setText(textView.getText().toString() + status + "\n");
        int bottom = textView.getBottom() + scrollView.getPaddingBottom();
        int sy = scrollView.getScrollY();
        int sh = scrollView.getHeight();
        int delta = bottom - (sy + sh);

        scrollView.smoothScrollBy(0, delta);
    }

    private void startRootingProcess() {
        executorService.execute(() -> {
            extractPoc();
            extractMagisk();

            try {
                String[] args = {pocPath, "shell_exec", magiskInstPath + " " + magiskPath};
                boolean success = executeNativeCode(args);
                uiHandler.post(() -> onRootingFinished(success));
            } catch (IOException | InterruptedException e) {
                uiHandler.post(() -> addStatus(e.toString()));
            }
        });
    }

    private void extractPoc() {
        InputStream poc = getResources().openRawResource(R.raw.poc);
        File pocDir = getApplicationContext().getFilesDir();
        File pocFile = new File(pocDir, "do_root");
        pocPath = pocFile.getPath();
        uiHandler.post(() -> addStatus(getString(R.string.extracting_native_code)));
        copyFile(poc, pocFile.getPath());
        if (!pocFile.setExecutable(true)) {
            uiHandler.post(() -> addStatus(getString(R.string.failed_to_set_executable)));
        }
    }

    private void extractMagisk() {
        uiHandler.post(() -> addStatus(getString(R.string.extracting_magisk)));

        InputStream magisk = getResources().openRawResource(R.raw.magiskinit64);
        File fileDir = getApplicationContext().getFilesDir();
        File magiskFile = new File(fileDir, "magiskinit64");
        magiskPath = magiskFile.getPath();
        copyFile(magisk, magiskPath);
        if (!magiskFile.setExecutable(true)) {
            uiHandler.post(() -> addStatus(getString(R.string.failed_to_set_executable)));
        }

        uiHandler.post(() -> addStatus(getString(R.string.extracting_installer)));

        InputStream magiskInst = getResources().openRawResource(R.raw.magisk_install);
        File magiskInstFile = new File(fileDir, "magisk_install");
        magiskInstPath = magiskInstFile.getPath();
        copyFile(magiskInst, magiskInstPath);
        if (!magiskInstFile.setExecutable(true)) {
            uiHandler.post(() -> addStatus(getString(R.string.failed_to_set_executable)));
        }
    }

    private boolean executeNativeCode(String[] args) throws IOException, InterruptedException {
        uiHandler.post(() -> addStatus(getString(R.string.executing_native_binary)));
        Process nativeApp = Runtime.getRuntime().exec(args);

        BufferedReader reader = new BufferedReader(new InputStreamReader(nativeApp.getInputStream()));

        String str;
        while ((str = reader.readLine()) != null) {
            String finalStr = str;
            uiHandler.post(() -> addStatus("[NATIVE] " + finalStr));
        }

        reader.close();
        nativeApp.waitFor();
        return nativeApp.exitValue() == 0;
    }

    private void onRootingFinished(boolean success) {
        if (!success) {
            addStatus(getString(R.string.root_failed));
            rootButton.setText(getString(R.string.root));
            rootButton.setClickable(true);
            rootButton.getBackground().setColorFilter(null);
        } else {
            addStatus(getString(R.string.root_success));
            rootButton.setText(getString(R.string.rooted));
        }
    }

    private static void copyFile(InputStream in, String localPath) {
        try {
            FileOutputStream out = new FileOutputStream(localPath);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
