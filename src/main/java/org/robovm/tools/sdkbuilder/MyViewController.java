package org.robovm.tools.sdkbuilder;

import fi.iki.elonen.SimpleWebServer;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;
import org.robovm.sdk.SdkGenerator;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyViewController extends UIViewController {
    private final UIButton genSdkButton;
    private final UIButton downloadCacheButton;
    private final UITextView statusTextView;

    public MyViewController() {
        // Get the view of this view controller.
        UIView view = getView();

        // Setup background.
        view.setBackgroundColor(UIColor.white());

        // Setup label.
        statusTextView = new UITextView(new CGRect(20, 250, view.getFrame().getWidth() - 40, 100));
        statusTextView.setUserInteractionEnabled(false);
        statusTextView.setTextAlignment(NSTextAlignment.Left);
        view.addSubview(statusTextView);

        // Setup buttons.
        genSdkButton = new UIButton(UIButtonType.RoundedRect);
        genSdkButton.setFrame(new CGRect(20, 150, view.getFrame().getWidth() - 40, 40));
        genSdkButton.setTitle("Generate SDK", UIControlState.Normal);
        genSdkButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(22));
        genSdkButton.addOnTouchUpInsideListener((control, event) -> startParsing());
        view.addSubview(genSdkButton);

        downloadCacheButton = new UIButton(UIButtonType.RoundedRect);
        downloadCacheButton.setFrame(new CGRect(20, 200, view.getFrame().getWidth() - 40, 40));
        downloadCacheButton.setTitle("Download dyld cache", UIControlState.Normal);
        downloadCacheButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(22));
        downloadCacheButton.addOnTouchUpInsideListener((control, event) -> startDownloadServer());
        view.addSubview(downloadCacheButton);

    }

    private void startDownloadServer() {
        try {
            statusTextView.setText(InetAddress.getLocalHost().getHostAddress());
            startWebServer(new File("/System/Library/Caches/com.apple.dyld/"), 8081);
            startWebServer(new File("/System/Library/CoreServices/"), 8082);

            statusTextView.setText("dyld_shared_cache_arm64:  http://" + InetAddress.getLocalHost().getHostAddress() + ":8081\n" +
                    "SystemVersion.plist:  http://" + InetAddress.getLocalHost().getHostAddress() + ":8082\n");
            downloadCacheButton.setEnabled(false);
            showMessage("Web servers started", "Download these files and use them with Java SDK builder. Check log view for URLs");
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }


    private void startParsing() {
        genSdkButton.setEnabled(false);

        new NSOperationQueue().addOperation(() -> {
            try {
                File cacheFile64b = new File("/System/Library/Caches/com.apple.dyld/dyld_shared_cache_arm64");
                NSURL url = NSFileManager.getDefaultManager().getURLsForDirectory(NSSearchPathDirectory.CachesDirectory,
                        NSSearchPathDomainMask.UserDomainMask).last();
                File outputDir = new File(url.getPath());
                File systemVersionPlist = new File("/System/Library/CoreServices/SystemVersion.plist");

                SdkGenerator.generateSdk(cacheFile64b, systemVersionPlist, outputDir, (msg, progress) -> {
                    NSOperationQueue.getMainQueue().addOperation(() -> statusTextView.setText(msg));
                });

                // start a server to pick files
                startWebServer(outputDir, 8083);

                NSOperationQueue.getMainQueue().addOperation(() -> {
                    try {
                        String msg = "Xcode.zip:  http://" + InetAddress.getLocalHost().getHostAddress() + ":8083";
                        statusTextView.setText(msg);
                        showMessage("SDK generated", "SDK files were packed to Xcode.zip, web server has been started, check log view for URLs");
                    } catch (UnknownHostException e) {
                        showErrorMessage(e.getMessage());
                    }
                });
            } catch (Throwable e) {
                NSOperationQueue.getMainQueue().addOperation(() -> showErrorMessage(e.getMessage()));
            }
        });
    }

    private void startWebServer(File location, int port) throws IOException {
        SimpleWebServer server = new SimpleWebServer(null, port, location, false);
        server.start();

    }

    private void showErrorMessage(String msg) {
        showMessage("Error", msg);
    }

    private void showMessage(String title, String msg) {
        UIAlertController alert = new UIAlertController(title, msg, UIAlertControllerStyle.Alert);
        alert.addAction(new UIAlertAction("ok", UIAlertActionStyle.Default, null));
        presentViewController(alert, true, null);
    }
}
