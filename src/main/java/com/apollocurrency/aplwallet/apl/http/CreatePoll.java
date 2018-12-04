/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_POLL_DESCRIPTION_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_POLL_NAME_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_POLL_OPTION_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ZEROOPTIONS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DESCRIPTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_NAME;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Attachment.MessagingPollCreation.PollBuilder;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.json.simple.JSONStreamAware;

public final class CreatePoll extends CreateTransaction {

    private static class CreatePollHolder {
        private static final CreatePoll INSTANCE = new CreatePoll();
    }

    public static CreatePoll getInstance() {
        return CreatePollHolder.INSTANCE;
    }

    private CreatePoll() {
        super(new APITag[] {APITag.VS, APITag.CREATE_TRANSACTION},
                "name", "description", "finishHeight", "votingModel",
                "minNumberOfOptions", "maxNumberOfOptions",
                "minRangeValue", "maxRangeValue",
                "minBalance", "minBalanceModel", "holding",
                "option00", "option01", "option02", "option00Length", "option01Length", "option02Length", "nameLength", "descriptionLength",
                "optionsNumber",
                "totalOptionsLength");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

        String nameValue = Convert.emptyToNull(req.getParameter("name"));
        String descriptionValue = req.getParameter("description");

        if (nameValue == null || nameValue.trim().isEmpty()) {
            return new CreateTransactionRequestData(MISSING_NAME);
        } else if (descriptionValue == null) {
            return new CreateTransactionRequestData(MISSING_DESCRIPTION);
        }

        if (nameValue.length() > Constants.MAX_POLL_NAME_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_POLL_NAME_LENGTH);
        }

        if (descriptionValue.length() > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_POLL_DESCRIPTION_LENGTH);
        }

        List<String> options = new ArrayList<>();
        while (options.size() < Constants.MAX_POLL_OPTION_COUNT) {
            int i = options.size();
            String optionValue = Convert.emptyToNull(req.getParameter("option" + (i < 10 ? "0" + i : i)));
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH || (optionValue = optionValue.trim()).isEmpty()) {
                return new CreateTransactionRequestData(INCORRECT_POLL_OPTION_LENGTH);
            }
            options.add(optionValue);
        }

        byte optionsSize = (byte) options.size();
        if (options.size() == 0) {
            return new CreateTransactionRequestData(INCORRECT_ZEROOPTIONS);
        }

        int currentHeight = Apl.getBlockchain().getHeight();
        int finishHeight = ParameterParser.getInt(req, "finishHeight",
                currentHeight + 2,
                currentHeight + Constants.MAX_POLL_DURATION + 1, true);

        byte votingModel = ParameterParser.getByte(req, "votingModel", (byte) 0, (byte) 3, true);

        byte minNumberOfOptions = ParameterParser.getByte(req, "minNumberOfOptions", (byte) 1, optionsSize, true);
        byte maxNumberOfOptions = ParameterParser.getByte(req, "maxNumberOfOptions", minNumberOfOptions, optionsSize, true);

        byte minRangeValue = ParameterParser.getByte(req, "minRangeValue", Constants.MIN_VOTE_VALUE, Constants.MAX_VOTE_VALUE, true);
        byte maxRangeValue = ParameterParser.getByte(req, "maxRangeValue", minRangeValue, Constants.MAX_VOTE_VALUE, true);

        PollBuilder builder = new PollBuilder(nameValue.trim(), descriptionValue.trim(),
                options.toArray(new String[options.size()]), finishHeight, votingModel,
                minNumberOfOptions, maxNumberOfOptions, minRangeValue, maxRangeValue);

        long minBalance = ParameterParser.getLong(req, "minBalance", 0, Long.MAX_VALUE, false);

        if (minBalance != 0) {
            byte minBalanceModel = ParameterParser.getByte(req, "minBalanceModel", (byte) 0, (byte) 3, true);
            builder.minBalance(minBalanceModel, minBalance);
        }

        long holdingId = ParameterParser.getUnsignedLong(req, "holding", false);
        if (holdingId != 0) {
            builder.holdingId(holdingId);
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = builder.build();
        return new CreateTransactionRequestData(attachment, account);
    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        String nameValue = Convert.emptyToNull(req.getParameter("name"));
        String descriptionValue = req.getParameter("description");
        int nameLength = ParameterParser.getInt(req, "nameLength", 1, Integer.MAX_VALUE, false);
        int descriptionLength = ParameterParser.getInt(req, "descriptionLength", 0, Integer.MAX_VALUE, false, -1);

        if (nameLength == 0 && (nameValue == null || nameValue.trim().isEmpty())) {
            return new CreateTransactionRequestData(MISSING_NAME);
        } else if (descriptionLength == -1 && descriptionValue == null) {
            return new CreateTransactionRequestData(MISSING_DESCRIPTION);
        }
        if (nameLength == 0) {
            nameLength = nameValue.length();
        }
        if (descriptionLength == 0) {
            descriptionLength = descriptionValue.length();
        }

        if (nameLength > Constants.MAX_POLL_NAME_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_POLL_NAME_LENGTH);
        }

        if (descriptionLength > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_POLL_DESCRIPTION_LENGTH);
        }

        List<String> options;
        byte optionsSize = ParameterParser.getByte(req, "optionsNumber", (byte) 1, Byte.MAX_VALUE, false);
        int totalOptionsLength = ParameterParser.getInt(req, "totalOptionsLength", 1, Integer.MAX_VALUE, false);

        if (optionsSize == 0 || totalOptionsLength == 0 || optionsSize > totalOptionsLength) {
            List<String> lengthOptions = getLengthOptions(req);
            if (lengthOptions.size() == 0) {
                options = new ArrayList<>();
                JSONStreamAware error = getOptions(req, options);
                if (error != null) {
                    return new CreateTransactionRequestData(error);
                }
            } else {
                options = lengthOptions;
            }
        } else {
            List<String> fakeOptions = new ArrayList<>();
            fakeOptions.add(StringUtils.repeat('*', totalOptionsLength - optionsSize + 1));
            for (byte i = 1; i < optionsSize; i++) {
                fakeOptions.add("*");
            }
            options = fakeOptions;
        }
                if (options.size() == 0) {
                    return new CreateTransactionRequestData(INCORRECT_ZEROOPTIONS);
                }


            PollBuilder builder = new PollBuilder(nameValue.trim(), descriptionValue.trim(),
                    options.toArray(new String[0]), 0, (byte) 0,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0);

            Attachment attachment = builder.build();
            return new CreateTransactionRequestData(attachment, null);
        }

    private List<String> getLengthOptions(HttpServletRequest req) throws ParameterException {
        List<String> options = new ArrayList<>();
        while (options.size() < Constants.MAX_POLL_OPTION_COUNT) {
            int i = options.size();
            int optionLength = ParameterParser.getInt(req, "option" + (i < 10 ? "0" + i : i + "Length"), 1, Constants.MAX_POLL_OPTION_LENGTH, false);
            if (optionLength == 0) {
                break;
            }
            options.add(StringUtils.repeat("*", optionLength));
        }
        return options;
    }

    private JSONStreamAware getOptions(HttpServletRequest req, List<String> options) throws ParameterException {
        while (options.size() < Constants.MAX_POLL_OPTION_COUNT) {
            int i = options.size();
            String optionValue = Convert.emptyToNull(req.getParameter("option" + (i < 10 ? "0" + i : i)));
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH || (optionValue = optionValue.trim()).isEmpty()) {
                return INCORRECT_POLL_OPTION_LENGTH;
            }
            options.add(optionValue);
        }
        return null;
    }
}
