/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dbmodel;

import com.apollocurrency.aplwallet.apl.Db;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import static org.slf4j.LoggerFactory.getLogger;

//TODO: Refactor as ORM

public class Option {
    private static final Logger LOG = getLogger(Option.class);

    public static String get(String optionName) 
    {
        try (Connection con = Db.db.getConnection()) 
        {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM option WHERE name = ?");
            stmt.setString(1, optionName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        catch (SQLException e)
        {
            LOG.error(e.getMessage());
        }
        return null;
    }
    
    public static boolean set(String optionName, String optionValue)
    {
        if (get(optionName) == null)
        {
            try (Connection con = Db.db.getConnection())
            {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO option (name, value) VALUES (?, ?)");
                stmt.setString(1, optionName);
                stmt.setString(2, optionValue);
                stmt.execute();
            }
            catch (SQLException e)
            {
                LOG.error(e.getMessage());
            }
        }
        else
        {
            try (Connection con = Db.db.getConnection())
            {
                PreparedStatement stmt = con.prepareStatement("UPDATE option set value = ? WHERE name = ?");
                stmt.setString(1, optionValue);
                stmt.setString(2, optionName);
                stmt.execute();
            }
            catch (SQLException e)
            {
                LOG.error(e.getMessage());
            }
        }
        return true;
    }
    
    public static boolean delete(String optionName)
    {
        if (get(optionName) == null)
        {
            return false;
        }
        else
        {
            try (Connection con = Db.db.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM option WHERE name = ?");
                stmt.setString(1, optionName);
            }
            catch (SQLException e)
            {
                LOG.error(e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
}
