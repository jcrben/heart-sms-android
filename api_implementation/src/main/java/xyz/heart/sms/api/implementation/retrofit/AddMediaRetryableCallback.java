package xyz.heart.sms.api.implementation.retrofit;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Response;
import xyz.heart.sms.api.implementation.media.MediaUploadCallback;

public class AddMediaRetryableCallback<T> extends LoggingRetryableCallback<T> {

    private final long messageId;
    private final MediaUploadCallback callback;

    public AddMediaRetryableCallback(Call<T> call, int totalRetries, long messageId, MediaUploadCallback callback) {
        super(call, totalRetries, "add media");
        this.messageId = messageId;
        this.callback = callback;
    }

    @Override
    public void onFinalResponse(Call<T> call, Response<T> response) {
        super.onFinalResponse(call, response);
        callback.onUploadFinished();
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        super.onFinalFailure(call, t);
        callback.onUploadFinished();
    }
}
