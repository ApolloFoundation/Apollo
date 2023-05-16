package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.ShufflingParticipant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ShufflingParticipantsResponse extends ResponseBase {
    private List<ShufflingParticipant> participants;
}
