/*
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

package apl.updater;

import apl.Attachment;

public class AuthorityChecker {
    private static class AuthorityCheckerHolder {
        private static final AuthorityChecker HOLDER_INSTANCE = new AuthorityChecker();
    }

    public static AuthorityChecker getInstance() {
        return AuthorityCheckerHolder.HOLDER_INSTANCE;
    }
    public boolean checkSignature(Attachment.UpdateAttachment attachment) {
        return true;
    }

}
