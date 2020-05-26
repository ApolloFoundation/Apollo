/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.form;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;

import com.apollocurrency.aplwallet.apl.core.rest.parameter.RecipientIdParameter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SendMoneyForm extends CreateTransactionSenderRecipientForm {

    @Parameter(required = true/*, schema = @Schema(implementation = String.class)*/) @Schema(description = "Mandatory recipient account ID.", implementation = String.class)
    @FormParam("recipient") @NotNull RecipientIdParameter recipient;

    @Parameter(required = true/*, schema = @Schema(implementation = Long.class)*/) @Schema(description = "amount ATM", implementation = Long.class)
    @FormParam("amountATM") @NotNull @DefaultValue("1") @Positive Long amountATM = 1L;

}
