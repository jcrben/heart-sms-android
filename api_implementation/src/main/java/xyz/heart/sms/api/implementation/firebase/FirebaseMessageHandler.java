package xyz.heart.sms.api.implementation.firebase;

import android.app.Application;

public interface FirebaseMessageHandler {
    void handleMessage(Application application, String operation, String data);
    void handleDelete(Application application);
}
