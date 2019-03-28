/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PhasingPollResult {

    private final long id;
    private final long result;
    private final boolean approved;
    private final int height;

    public PhasingPollResult(PhasingPoll poll, long result, int height) {
        this.id = poll.getId();
        this.result = result;
        this.approved = result >= poll.getQuorum();
        this.height = height;
    }

    public PhasingPollResult(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.result = rs.getLong("result");
        this.approved = rs.getBoolean("approved");
        this.height = rs.getInt("height");
    }

    public PhasingPollResult(long id, long result, boolean approved, int height) {
        this.id = id;
        this.result = result;
        this.approved = approved;
        this.height = height;
    }

    public void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_result (id, "
                + "result, approved, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, id);
            pstmt.setLong(++i, result);
            pstmt.setBoolean(++i, approved);
            pstmt.setInt(++i, height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getResult() {
        return result;
    }

    public boolean isApproved() {
        return approved;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPollResult)) return false;
        PhasingPollResult result1 = (PhasingPollResult) o;
        return id == result1.id &&
                result == result1.result &&
                approved == result1.approved &&
                height == result1.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, result, approved, height);
    }
}
