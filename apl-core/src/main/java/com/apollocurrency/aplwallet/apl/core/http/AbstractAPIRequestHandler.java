

package com.apollocurrency.aplwallet.apl.core.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

public abstract class AbstractAPIRequestHandler {

    private List<String> parameters;
    private String fileParameter;
    private Set<APITag> apiTags;

    public AbstractAPIRequestHandler(APITag[] apiTags, String... parameters) {
        this(null, apiTags, parameters);
    }

    public AbstractAPIRequestHandler(String fileParameter, APITag[] apiTags, String... origParameters) {
        List<String> parameters = new ArrayList<>();
        Collections.addAll(parameters, origParameters);
        if ((requirePassword() || parameters.contains("lastIndex")) && ! API.disableAdminPassword) {
            parameters.add("adminPassword");
        }
        if (allowRequiredBlockParameters()) {
            parameters.add("requireBlock");
            parameters.add("requireLastBlock");
        }
        String vaultAccountParameterName = vaultAccountName();
        if (vaultAccountParameterName != null && !vaultAccountParameterName.isEmpty()) {
            parameters.add(vaultAccountParameterName);
            parameters.add("passphrase");
        }
        if (is2FAProtected()) {
            parameters.add("code2FA");
        }
        this.parameters = Collections.unmodifiableList(parameters);
        this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
        this.fileParameter = fileParameter;
    }

    public final List<String> getParameters() {
        return parameters;
    }

    public final Set<APITag> getAPITags() {
        return apiTags;
    }

    public final String getFileParameter() {
        return fileParameter;
    }

    public abstract JSONStreamAware processRequest(HttpServletRequest request) throws AplException;

    public JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws AplException {
        return processRequest(request);
    }

    protected boolean requirePost() {
        return false;
    }

    protected boolean startDbTransaction() {
        return false;
    }

    protected boolean requirePassword() {
        return false;
    }

    protected boolean allowRequiredBlockParameters() {
        return true;
    }

    protected boolean requireBlockchain() {
        return true;
    }

    protected boolean requireFullClient() {
        return false;
    }

    protected boolean logRequestTime() { return false; }

    protected boolean is2FAProtected() {
        return false;
    }

    protected String vaultAccountName() {
        return null;
    }

}
