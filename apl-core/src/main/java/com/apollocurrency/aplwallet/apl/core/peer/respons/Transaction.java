/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import com.apollocurrency.aplwallet.api.p2p.respons.BaseP2PResponse;
import com.cedarsoftware.util.io.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction extends BaseP2PResponse {

    public byte type;
    public byte subtype;
    public int timestamp;
    public short deadline;
    public String senderPublicKey;

    public Long amountATM;
    public Long feeATM;

    public String referencedTransactionFullHash;
    public long version;

    public JsonObject signature;
    public JsonObject attachment;

    public int ecBlockHeight;
    public String ecBlockId;

//    public MessageAppendix messageAppendix;
//    public EncryptedMessageAppendix encryptedMessageAppendix;
//    public PublicKeyAnnouncementAppendix publicKeyAnnouncementAppendix;
//    public EncryptToSelfMessageAppendix encryptToSelfMessageAppendix;
//    public PhasingAppendixFactory phasingAppendixFactory;
//    public PrunablePlainMessageAppendix prunablePlainMessageAppendix;
//    public PrunableEncryptedMessageAppendix prunableEncryptedMessageAppendix;


}
