package com.friendlysmp.core.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public final class PlayerFlagDao {
    private final DataSource ds;
    private final String table;
    private final String column;

    public PlayerFlagDao(DataSource ds, String table, String column) {
        this.ds = ds;
        this.table = table;
        this.column = column;
    }

    public void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + " uuid TEXT PRIMARY KEY,"
                    + " " + column + " INTEGER NOT NULL DEFAULT 0"
                    + ")");
        }
    }

    public boolean load(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + column + " FROM " + table + " WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getInt(1) == 1;
            }
        }
    }

    public void upsert(UUID uuid, boolean value) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO " + table + " (uuid, " + column + ") VALUES (?, ?)"
                             + " ON CONFLICT(uuid) DO UPDATE SET " + column + "=excluded." + column)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, value ? 1 : 0);
            ps.executeUpdate();
        }
    }
}
