/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers                             *
 * Copyright © 2016-2017 Jelurida IP B.V.                                     *
 * Copyright © 2017-2018 Apollo Foundation                                    *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,*
 * no part of the Apl software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS) {

    // The methods below are invoked by Java code
    NRS.growl = function(msg) {
        $.growl(msg);
    };
    NRS.javabridgeLoader = function() {
        console.log('loaded javabridge loader');
    };

    return NRS;
}(NRS || {}, jQuery));