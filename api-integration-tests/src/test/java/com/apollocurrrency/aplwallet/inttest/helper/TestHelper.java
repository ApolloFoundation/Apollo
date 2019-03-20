package com.apollocurrrency.aplwallet.inttest.helper;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import static org.junit.jupiter.api.Assertions.*;


public class TestHelper {
    public static ObjectMapper mapper = new ObjectMapper();
    private static final String baseURL_API = "http://"+
            TestConfiguration.getTestConfiguration().getBaseURL()+":"+
            TestConfiguration.getTestConfiguration().getPort()+"/apl?";
    private static final String baseURL = "http://"+
                                          TestConfiguration.getTestConfiguration().getBaseURL()+":"+
                                          TestConfiguration.getTestConfiguration().getPort();
    private static HashMap<String,Object> reqestParam = new HashMap<>();
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
        Request request = new Request.Builder()
                .url(buildGetReqestUrl())
                .post(body)
                .build();
        return getClient().newCall(request).execute();
    }


    public static void addParameters(Enum parameter, Object value){
        if (value instanceof String)
            reqestParam.put(parameter.toString(), (String) value);
        else if (value instanceof Integer || value instanceof Boolean)
            reqestParam.put(parameter.toString(), String.valueOf(value));
        else if (value instanceof Enum)
            reqestParam.put(parameter.toString(), value.toString());
        else
            reqestParam.put(parameter.toString(), value);
    }


    private static String buildGetReqestUrl(){
        StringBuilder reqestUrl =  new StringBuilder();
        reqestUrl.append(baseURL_API);
        for(Map.Entry<String,Object> pair: reqestParam.entrySet()) {
            if (pair.getKey().equals("wallet"))
            {
                Wallet wallet = (Wallet) pair.getValue();
                reqestUrl.append("account="+wallet.getUser());
                reqestUrl.append("&");
                if (wallet.getSecretKey() == null)
                {
                    reqestUrl.append("secretPhrase="+wallet.getPass());
                    reqestUrl.append("&");
                }
                else
                {
                    reqestUrl.append("secretBytes="+wallet.getSecretKey());
                    reqestUrl.append("&");
                    reqestUrl.append("sender="+wallet.getUser());
                    reqestUrl.append("&");
                    reqestUrl.append("passphrase="+wallet.getPass());
                    reqestUrl.append("&");
                }
            }
            else
            {
                reqestUrl.append(pair.getKey()+"="+pair.getValue().toString());
                reqestUrl.append("&");
            }
        }
        reqestParam.clear();
        return reqestUrl.toString();
    }

    private static String buildGetReqestUrl(String peerURL_API){
        StringBuilder reqestUrl =  new StringBuilder();
        reqestUrl.append("http://"+peerURL_API+":"+TestConfiguration.getTestConfiguration().getPort()+"/apl?");
        for(Map.Entry<String,Object> pair: reqestParam.entrySet()) {
            if (pair.getKey().equals("wallet"))
            {
                Wallet wallet = (Wallet) pair.getValue();
                reqestUrl.append("account="+wallet.getUser());
                reqestUrl.append("&");
                if (wallet.getSecretKey() == null)
                {
                    reqestUrl.append("secretPhrase="+wallet.getPass());
                    reqestUrl.append("&");
                }
                else
                {
                    reqestUrl.append("secretBytes="+wallet.getSecretKey());
                    reqestUrl.append("&");
                    reqestUrl.append("sender="+wallet.getUser());
                    reqestUrl.append("&");
                    reqestUrl.append("passphrase="+wallet.getPass());
                    reqestUrl.append("&");
                }
            }
            else
            {
                reqestUrl.append(pair.getKey()+"="+pair.getValue().toString());
                reqestUrl.append("&");
            }
        }
        reqestParam.clear();
        return reqestUrl.toString();
    }

    public static <T> T getInstanse(Class clas) {
        Response response;
        String responseBody= "";
        try {
        response =  httpCallPost();
        responseBody = response.body().string();
        //System.out.println(responseBody);
        Assert.assertEquals(200, response.code());
        if (TestBase.testInfo != null && TestBase.testInfo.getTags()!=null && !TestBase.testInfo.getTags().contains("NEGATIVE"))
        assertFalse(responseBody.contains("errorDescription"), responseBody);
        return (T) mapper.readValue(responseBody, clas);
        }
        catch (Exception e)
        {
                throw new UnknownFormatConversionException(responseBody+" : \n"+ e.getMessage());
        }
    }
}
