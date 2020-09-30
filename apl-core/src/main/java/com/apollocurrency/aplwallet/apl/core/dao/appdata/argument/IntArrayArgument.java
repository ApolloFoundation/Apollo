package com.apollocurrency.aplwallet.apl.core.dao.appdata.argument;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IntArrayArgument implements Argument {
    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {

    }
}
