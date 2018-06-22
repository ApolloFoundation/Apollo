    /*
     * Copyright © 2013-2016 The Nxt Core Developers.
     * Copyright © 2016-2017 Jelurida IP B.V.
     * Copyright © 2017-2018 Apollo Foundation
     *
     * See the LICENSE.txt file at the top-level directory of this distribution
     * for licensing information.
     *
     * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
     * no part of the Apl software, including this file, may be copied, modified,
     * propagated, or distributed except according to the terms contained in the
     * LICENSE.txt file.
     *
     * Removal or modification of this copyright notice is prohibited.
     *
     */

    package apl.http;

    import apl.UpdaterUtil;
    import org.json.simple.JSONStreamAware;

    import javax.servlet.http.HttpServletRequest;

    public class StopForgingAndBlockAcceptance extends APIServlet.APIRequestHandler {

        static final StopForgingAndBlockAcceptance instance = new StopForgingAndBlockAcceptance();

        private StopForgingAndBlockAcceptance() {
            super(new APITag[] {APITag.FORGING}, "adminPassword");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
            API.verifyPassword(req);
            UpdaterUtil.stopForgingAndBlockAcceptance();
            return null;
        }

        @Override
        protected boolean requirePost() {
            return true;
        }

        @Override
        protected boolean allowRequiredBlockParameters() {
            return false;
        }

        @Override
        protected boolean requireFullClient() {
            return true;
        }

    }

