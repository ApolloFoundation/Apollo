package com.apollocurrency.aplwallet.apl.core.shard.helper;

public class ResultWrapper {
    public Long limitValue = -1L;
    public Boolean isFinished = Boolean.FALSE;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ResultWrapper{");
        sb.append("limitValue=").append(limitValue);
        sb.append(", isFinished=").append(isFinished);
        sb.append('}');
        return sb.toString();
    }
}
