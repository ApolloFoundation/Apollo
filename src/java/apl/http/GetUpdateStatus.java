package apl.http;

import apl.AplException;
import apl.UpdaterMediator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetUpdateStatus extends APIServlet.APIRequestHandler {

    static final GetUpdateStatus instance = new GetUpdateStatus();

    private GetUpdateStatus() {
        super(new APITag[]{APITag.UPDATE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        JSONObject result = new JSONObject();
        UpdaterMediator.UpdateInfo info = UpdaterMediator.getInstance().getUpdateInfo();
        result.put("isUpdate", info.isUpdate());
        if (info.isUpdate()) {
            result.put("level", info.getUpdateLevel());
            result.put("availableVersion", info.getUpdateVersion().toString());
            result.put("estimatedUpdateHeight", info.getUpdateHeight());
            result.put("receivedUpdateHeight", info.getReceivedUpdateHeight());
        }
        return result;
    }
}
