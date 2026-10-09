package com.friendlysmp.core.features.homes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public final class HomesDao {
    private final DataSource ds;

    public HomesDao(DataSource ds) {
        this.ds = ds;
    }

    public void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendlycore_bonus_homes (
                  uuid TEXT PRIMARY KEY,
                  bonus_homes INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    public int getBonusHomes(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT bonus_homes FROM friendlycore_bonus_homes WHERE uuid=?
             """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt(1);
            }
        }
    }

    public void setBonusHomes(UUID uuid, int amount) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO friendlycore_bonus_homes (uuid, bonus_homes)
                 VALUES (?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET bonus_homes = excluded.bonus_homes
             """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, Math.max(0, amount));
            ps.executeUpdate();
        }
    }
}
