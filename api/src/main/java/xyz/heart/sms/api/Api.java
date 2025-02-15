/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.heart.sms.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.heart.sms.api.service.AccountService;
import xyz.heart.sms.api.service.ActivateService;
import xyz.heart.sms.api.service.ArticleService;
import xyz.heart.sms.api.service.AutoReplyService;
import xyz.heart.sms.api.service.BetaService;
import xyz.heart.sms.api.service.BlacklistService;
import xyz.heart.sms.api.service.ContactService;
import xyz.heart.sms.api.service.ConversationService;
import xyz.heart.sms.api.service.DeviceService;
import xyz.heart.sms.api.service.DraftService;
import xyz.heart.sms.api.service.FolderService;
import xyz.heart.sms.api.service.MediaService;
import xyz.heart.sms.api.service.MessageService;
import xyz.heart.sms.api.service.PurchaseService;
import xyz.heart.sms.api.service.ScheduledMessageService;
import xyz.heart.sms.api.service.TemplateService;
import xyz.heart.sms.api.service.AccountService;
import xyz.heart.sms.api.service.ActivateService;
import xyz.heart.sms.api.service.AutoReplyService;
import xyz.heart.sms.api.service.BetaService;
import xyz.heart.sms.api.service.BlacklistService;
import xyz.heart.sms.api.service.ContactService;
import xyz.heart.sms.api.service.ConversationService;
import xyz.heart.sms.api.service.DeviceService;
import xyz.heart.sms.api.service.DraftService;
import xyz.heart.sms.api.service.FolderService;
import xyz.heart.sms.api.service.MessageService;
import xyz.heart.sms.api.service.PurchaseService;
import xyz.heart.sms.api.service.ScheduledMessageService;
import xyz.heart.sms.api.service.TemplateService;

/**
 * Direct access to the messenger APIs using retrofit.
 */
public class Api {

    private static final String API_DEBUG_URL = "http://10.0.2.2:5000";
    public static final String API_PATH = "/api/v1/";

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static CallAdapter.Factory callAdapterFactory = new CallAdapter.Factory() {
        @Override
        public CallAdapter<Object, Object> get(final Type returnType, Annotation[] annotations,
                                       Retrofit retrofit) {
            // if returnType is retrofit2.Call, do nothing
            if (returnType.getClass().getPackage().getName().contains("retrofit2.Call")) {
                return null;
            }

            return new CallAdapter<Object, Object>() {
                @Override
                public Type responseType() {
                    return returnType;
                }

                @Override
                public Object adapt(Call call) {
                    Response response;
                    Call retry = call.clone();

                    try {
                        response = call.execute();
                    } catch (Exception e) {
                        response = null;
                    }

                    if (response == null || !response.isSuccessful()) {
                        try {
                            response = retry.execute();
                        } catch (Exception e) {
                            response = null;
                        }
                    }

                    if (response == null || !response.isSuccessful()) {
                        return null;
                    } else {
                        return response.body();
                    }
                }
            };
        }
    };

    private static Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setFieldNamingStrategy(new FieldNamingStrategy() {
                @Override
                public String translateName(Field f) {
                    return separateCamelCase(f.getName(), "_").toLowerCase(Locale.ROOT);
                }

                private String separateCamelCase(String name, String separator) {
                    StringBuilder translation = new StringBuilder();
                    for (int i = 0; i < name.length(); i++) {
                        char character = name.charAt(i);
                        if (Character.isUpperCase(character) && translation.length() != 0) {
                            translation.append(separator);
                        }
                        translation.append(character);
                    }
                    return translation.toString();
                }
            })
            .create();

    private Retrofit retrofit;
    private String baseUrl;

    public enum Environment {
        DEBUG
    }

    public static String validateApiUrl(String url_input) {
        URL url;
        String protocol;
        String host;
        Integer port;

        try {
            try {
                // Protocol may be empty resulting in a parse failure
                url = new URL(url_input);
                protocol = url.getProtocol();
            } catch (Exception e) {
                // Try setting to https
                protocol = "https";
                // If it still doesn't parse, they didn't just forget the protocol
                // Throw the outer exception
                url = new URL(protocol + "://" + url_input);
            }

            host = url.getHost();
            port = (url.getPort() > 0) ? url.getPort() : null;

            if (host.isEmpty()) {
                // In some instances, the hostname could be empty.
                throw new Exception("Error parsing URL. Try again");
            }

            // Now we have everything we need.
            String final_url = protocol + "://" + host;
            if (port != null) {
                final_url += ":" + port;
            }
            return final_url;

        } catch(Exception e) {
            // Actually handle the error
            // Tell user the correct format and have them try again
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a new API access object that will connect to the correct environment.
     *
     * @param environment the Environment to use to connect to the APIs.
     */
    public Api(Environment environment) {
        // Previously there was STAGING and RELEASE urls. Now we use the stored preference and send baseUrl directly
        this(API_DEBUG_URL + "/api/v1/");
    }

    /**
     * Creates a new API access object that will automatically attach your API key to all
     * requests.
     */
    public Api(String baseUrl) {
//        httpClient.addInterceptor(new Interceptor() {
//            @Override
//            public okhttp3.Response intercept(Chain chain) throws IOException {
//                Request request = chain.request();
//                HttpUrl url = request.url().newBuilder().build();
//                request = request.newBuilder().url(url).build();
//                return chain.proceed(request);
//            }
//        });

        // gzip all bodies, the server should automatically unzip them
//        httpClient.addInterceptor(new GzipRequestInterceptor());

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        //httpClient.addInterceptor(logging);

        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create(gson));
                        //.addCallAdapterFactory(callAdapterFactory);

        this.retrofit = builder.client(httpClient.build()).build();
        this.baseUrl = baseUrl;
    }

    /**
     * Gets a service that can be used for account requests such as signup and login.
     */
    public AccountService account() {
        return retrofit.create(AccountService.class);
    }

    /**
     * Gets a service that can be used for device requests.
     */
    public DeviceService device() {
        return retrofit.create(DeviceService.class);
    }

    /**
     * Gets a service that can be used for message requests.
     */
    public MessageService message() {
        return retrofit.create(MessageService.class);
    }

    /**
     * Gets a service that can be used for contact requests.
     */
    public ContactService contact() {
        return retrofit.create(ContactService.class);
    }

    /**
     * Gets a service that can be used for conversation requests.
     */
    public ConversationService conversation() {
        return retrofit.create(ConversationService.class);
    }

    /**
     * Gets a service that can be used for draft requests.
     */
    public DraftService draft() {
        return retrofit.create(DraftService.class);
    }

    /**
     * Gets a service that can be used for scheduled message requests.
     */
    public ScheduledMessageService scheduled() {
        return retrofit.create(ScheduledMessageService.class);
    }

    /**
     * Gets a service that can be used for blacklist requests.
     */
    public BlacklistService blacklist() {
        return retrofit.create(BlacklistService.class);
    }

    /**
     * Gets a service that can be used for template requests.
     */
    public TemplateService template() {
        return retrofit.create(TemplateService.class);
    }
    /**
     * Gets a service that can be used for template requests.
     */
    public PurchaseService purchases() {
        return retrofit.create(PurchaseService.class);
    }

    /**
     * Gets a service that can be used for auto reply requests.
     */
    public AutoReplyService autoReply() {
        return retrofit.create(AutoReplyService.class);
    }

    /**
     * Gets a service that can be used for folder requests.
     */
    public FolderService folder() {
        return retrofit.create(FolderService.class);
    }

    /**
     * Gets a service that can be used for beta requests.
     */
    public BetaService beta() {
        return retrofit.create(BetaService.class);
    }

    /**
     * Gets a service that can be used for article requests.
     */
    public ArticleService article() {
        return retrofit.create(ArticleService.class);
    }

    /**
     * Gets a service that can be used for media requests.
     */
    public MediaService media() {
        return retrofit.create(MediaService.class);
    }

    /**
     * Gets a service that can be used to activate your account on the web instead of on the device.
     */
    public ActivateService activate() {
        return retrofit.create(ActivateService.class);
    }

    public String baseUrl() {
        return baseUrl;
    }

    final class GzipRequestInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
            Request originalRequest = chain.request();
            if (originalRequest.body() == null ||
                    originalRequest.header("Content-Encoding") != null) {
                return chain.proceed(originalRequest);
            }

            Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return body.contentType();
                }

                @Override
                public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }

}
