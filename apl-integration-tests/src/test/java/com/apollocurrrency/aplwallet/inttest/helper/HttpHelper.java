package com.apollocurrrency.aplwallet.inttest.helper;


import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.apollocurrrency.aplwallet.inttest.model.Parameters.file;
import static com.apollocurrrency.aplwallet.inttest.model.Parameters.messageFile;
import static org.junit.jupiter.api.Assertions.fail;


public class HttpHelper {
    private static final String baseURL_API = "http://" +
        TestConfiguration.getTestConfiguration().getBaseURL() + ":" +
        TestConfiguration.getTestConfiguration().getPort() + "/apl?";
    public static ObjectMapper mapper = new ObjectMapper();
    private static HashMap<String, Object> reqestParam = new HashMap<>();
    private static OkHttpClient client;


    public static OkHttpClient getClient() {
        if (client == null)
            client = new OkHttpClient();
        return client;
    }

    public static Response httpCallGet() throws IOException {
        Request request = new Request.Builder()
            .url(buildGetReqestUrl())
            .get()
            .build();
        return getClient().newCall(request).execute();
    }

    public static Response httpCallGet(String peerUrl) throws IOException {
        Request request = new Request.Builder()
            .url(buildGetReqestUrl(peerUrl))
            .get()
            .build();
        return getClient().newCall(request).execute();
    }

    public static Response httpCallPost() throws IOException {
        RequestBody body = RequestBody.create(null, new byte[]{});

        if (reqestParam.containsKey(messageFile.toString()) || reqestParam.containsKey(file.toString())) {
            String param = reqestParam.containsKey(messageFile.toString()) ? messageFile.toString() : file.toString();
            body = uploadImage(param);
          /*  File file = (File) reqestParam.get(messageFile.toString());
            final MediaType MEDIA_TYPE = file.getName().endsWith("png") ?
                    MediaType.parse("image/png") : MediaType.parse("image/jpeg");
             body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("messageFile", file.getName(), RequestBody.create(MEDIA_TYPE, file))
                    .build();*/
        }
        Request request = new Request.Builder()
            .url(buildGetReqestUrl())
            .post(body)
            .build();
        return getClient().newCall(request).execute();
    }


    public static void addParameters(Enum parameter, Object value) {
        if (value instanceof String)
            reqestParam.put(parameter.toString(), value);
        else if (value instanceof Integer || value instanceof Boolean)
            reqestParam.put(parameter.toString(), String.valueOf(value));
        else if (value instanceof Enum)
            reqestParam.put(parameter.toString(), value.toString());
        else
            reqestParam.put(parameter.toString(), value);
    }


    private static String buildGetReqestUrl() {
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append(baseURL_API);
        for (Map.Entry<String, Object> pair : reqestParam.entrySet()) {
            if (pair.getKey().equals("wallet")) {
                Wallet wallet = (Wallet) pair.getValue();
                requestUrl.append("account=" + wallet.getUser());
                requestUrl.append("&");
                if (!wallet.isVault()) {
                    requestUrl.append("secretPhrase=" + wallet.getPass());
                    requestUrl.append("&");
                } else {
                    requestUrl.append("secretBytes=" + wallet.isVault());
                    requestUrl.append("&");
                    requestUrl.append("sender=" + wallet.getUser());
                    requestUrl.append("&");
                    requestUrl.append("passphrase=" + wallet.getPass());
                    requestUrl.append("&");
                }
            } else {
                requestUrl.append(pair.getKey() + "=" + pair.getValue().toString());
                requestUrl.append("&");
            }
        }
        reqestParam.clear();
        if (TestBase.testInfo != null) {
            Allure.addAttachment("Request URL", requestUrl.toString());
        }
        return requestUrl.toString();
    }

    private static String buildGetReqestUrl(String peerURL_API) {
        StringBuilder reqestUrl = new StringBuilder();
        reqestUrl.append("http://" + peerURL_API + ":" + TestConfiguration.getTestConfiguration().getPort() + "/apl?");
        for (Map.Entry<String, Object> pair : reqestParam.entrySet()) {
            if (pair.getKey().equals("wallet")) {
                Wallet wallet = (Wallet) pair.getValue();
                reqestUrl.append("account=" + wallet.getUser());
                reqestUrl.append("&");
                if (!wallet.isVault()) {
                    reqestUrl.append("secretPhrase=" + wallet.getPass());
                    reqestUrl.append("&");
                } else {
                    reqestUrl.append("secretBytes=" + wallet.isVault());
                    reqestUrl.append("&");
                    reqestUrl.append("sender=" + wallet.getUser());
                    reqestUrl.append("&");
                    reqestUrl.append("passphrase=" + wallet.getPass());
                    reqestUrl.append("&");
                }
            } else {
                reqestUrl.append(pair.getKey() + "=" + pair.getValue().toString());
                reqestUrl.append("&");
            }
        }
        reqestParam.clear();
        return reqestUrl.toString();
    }

    public static <T> T getInstanse(Class clazz) {
        Response response;
        String responseBody = "";
        try {
            response = httpCallPost();
            responseBody = response.body().string();
            Assert.assertEquals(200, response.code());
            //System.out.println(responseBody);
            if (TestBase.testInfo != null && TestBase.testInfo.getTags() != null && !TestBase.testInfo.getTags().contains("NEGATIVE")) {
                Assertions.assertFalse(responseBody.contains("errorDescription"), responseBody);
            }
            if (TestBase.testInfo != null) {
                Allure.addAttachment("Response Body", responseBody);
            }
            return (T) mapper.readValue(responseBody, clazz);
        } catch (Exception e) {
            Allure.addAttachment("Response Body", responseBody);
            return fail(responseBody + "\n" + e.getMessage());
        }
    }

    private static RequestBody uploadImage(String param) {
        File file = (File) reqestParam.get(param);
        final MediaType MEDIA_TYPE = file.getName().endsWith("png") ?
            MediaType.parse("image/png") : MediaType.parse("image/jpeg");
        return new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(param, file.getName(), RequestBody.create(MEDIA_TYPE, file))
            .build();

    }

}
