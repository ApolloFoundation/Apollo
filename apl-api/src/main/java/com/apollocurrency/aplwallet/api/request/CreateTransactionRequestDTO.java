package com.apollocurrency.aplwallet.api.request;

public class CreateTransactionRequestDTO {
    public String deadline;
    public String referencedTransactionFullHash;
    public String secretPhrase;
    public String publicKey;
    public String recipientPublicKey;
    public boolean broadcast;
    public boolean encryptedMessageIsPrunable;
    public boolean messageIsPrunable;
    public boolean phased;
    public boolean messageToEncryptIsText;
    public boolean compressMessageToEncrypt;
    public EncryptedMessage encryptedMessage;
    public String messageToEncrypt;

    public class EncryptedMessage {
        private byte[] messageToEncrypt;
        private byte[] recipientPublicKey;

        @Override
        public String toString() {
            return "EncryptedMessage{" +
                    "messageToEncrypt=[" + (messageToEncrypt != null ? messageToEncrypt.length : -1) + "]" +
                    ", recipientPublicKey=[" + (recipientPublicKey != null ? recipientPublicKey.length : -1) +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CreateTransactionRequestDTO{" +
                "deadline='" + deadline + '\'' +
                ", referencedTransactionFullHash='" + referencedTransactionFullHash + '\'' +
                ", secretPhrase='" + secretPhrase + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", recipientPublicKey='" + recipientPublicKey + '\'' +
                ", broadcast=" + broadcast +
                ", encryptedMessageIsPrunable=" + encryptedMessageIsPrunable +
                ", messageIsPrunable=" + messageIsPrunable +
                ", phased=" + phased +
                ", messageToEncryptIsText=" + messageToEncryptIsText +
                ", compressMessageToEncrypt=" + compressMessageToEncrypt +
                ", encryptedMessage=" + (encryptedMessage != null ? encryptedMessage.toString() : "empty") +
                ", messageToEncrypt='" + messageToEncrypt + '\'' +
                '}';
    }
}
