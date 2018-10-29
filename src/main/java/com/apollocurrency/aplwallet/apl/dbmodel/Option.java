/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.dbmodel;

import com.apollocurrency.aplwallet.apl.Db;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.slf4j.LoggerFactory.getLogger;

public class Option {
    private static final Logger LOG = getLogger(Option.class);

    public static String get(String optionName) 
    {
        try (Connection con = Db.db.getConnection();
                Statement stmt = con.createStatement()) {
            String query = String.format("SELECT * FROM option WHERE name = '%s'", optionName);
            try (ResultSet rs = stmt.executeQuery(query)) {
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
            try (Connection con = Db.db.getConnection();
                    Statement stmt = con.createStatement()) {
                String query = String.format("INSERT INTO option (name, value) VALUES ('%s', '%s')", optionName, optionValue);
                stmt.execute(query);
            }
            catch (SQLException e)
            {
                LOG.error(e.getMessage());
            }
        }
        else
        {
            try (Connection con = Db.db.getConnection();
                    Statement stmt = con.createStatement()) {
                String query = String.format("UPDATE option set value = '%s' WHERE name = '%s'", optionValue, optionName);
                stmt.execute(query);
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
            try (Connection con = Db.db.getConnection();
                    Statement stmt = con.createStatement()) {
                String query = String.format("DELETE FROM option WHERE name = '%s'", optionName);
                stmt.execute(query);
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
