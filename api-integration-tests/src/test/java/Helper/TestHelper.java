package Helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestHelper {
    private static final String baseURL_API = "http://"+
            TestConfiguration.getTestConfiguration().getBaseURL()+":"+
            TestConfiguration.getTestConfiguration().getPort()+"/apl?";
    private static final String baseURL = "http://"+
                                          TestConfiguration.getTestConfiguration().getBaseURL()+":"+
                                          TestConfiguration.getTestConfiguration().getPort();
    private static HashMap<String,String> reqestParam = new HashMap<>();

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
    }


    private static String buildGetReqestUrl(){
        StringBuilder reqestUrl =  new StringBuilder();
        reqestUrl.append(baseURL_API);
        for(Map.Entry<String,String> pair: reqestParam.entrySet()) {
            reqestUrl.append(pair.getKey()+"="+pair.getValue());
            reqestUrl.append("&");
        }
        reqestParam.clear();
        return reqestUrl.toString();
    }

    private static String buildGetReqestUrl(String peerURL_API){
        StringBuilder reqestUrl =  new StringBuilder();
        reqestUrl.append("http://"+peerURL_API+":"+TestConfiguration.getTestConfiguration().getPort()+"/apl?");
        for(Map.Entry<String,String> pair: reqestParam.entrySet()) {
            reqestUrl.append(pair.getKey()+"="+pair.getValue());
            reqestUrl.append("&");
        }
        reqestParam.clear();
        return reqestUrl.toString();
    }

}
