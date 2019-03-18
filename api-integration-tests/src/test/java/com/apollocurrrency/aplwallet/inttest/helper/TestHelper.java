package com.apollocurrrency.aplwallet.inttest.helper;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import static org.junit.jupiter.api.Assertions.*;


public class TestHelper {
    private static final String baseURL_API = "http://"+
            TestConfiguration.getTestConfiguration().getBaseURL()+":"+
            TestConfiguration.getTestConfiguration().getPort()+"/apl?";
    private static final String baseURL = "http://"+
                                          TestConfiguration.getTestConfiguration().getBaseURL()+":"+
                                          TestConfiguration.getTestConfiguration().getPort();
    private static HashMap<String,Object> reqestParam = new HashMap<>();

    private static OkHttpClient client;
    private  static ObjectMapper mapper = new ObjectMapper(); 

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
        Assert.assertEquals(200, response.code());
        assertFalse(responseBody.contains("errorDescription"),responseBody);
       // System.out.println(responseBody);

        if (CreateTransactionResponse.class.equals(clas)) {
           return (T) mapper.readValue(responseBody, CreateTransactionResponse.class);
        } else if (TransactionDTO.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, TransactionDTO.class);
        }else if (BlockListInfoResponse.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, BlockListInfoResponse.class);
        }else if (GetAccountResponse.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, GetAccountResponse.class);
        }else if (GetAccountBlockCount.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, GetAccountBlockCount.class);
        }else if (AccountBlockIdsResponse.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, AccountBlockIdsResponse.class);
        } else if (AccountDTO.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, AccountDTO.class);
        }else if (AccountLedgerResponse.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, AccountLedgerResponse.class);
        }else if (AccountPropertiesResponse.class.equals(clas)) {
            return (T) mapper.readValue(responseBody, AccountPropertiesResponse.class);
        }

        }
        catch (Exception e)
        {
            throw new UnknownFormatConversionException(responseBody+" : \n"+ e.getMessage());
        }

      throw new UnknownFormatConversionException("Class not found: "+responseBody);
    }


}
