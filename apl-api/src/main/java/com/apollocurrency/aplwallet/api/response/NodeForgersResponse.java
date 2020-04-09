/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Response that contains node forgers
 *
 * @author alukin@gmail.com
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeForgersResponse extends ResponseBase {
    public List<GeneratorInfo> generators = new ArrayList<>();
}
