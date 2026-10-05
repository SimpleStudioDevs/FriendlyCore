package com.friendlysmp.core.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public final class HideArmorDao {
    private final DataSource ds;

    public HideArmorDao(DataSource ds) {
        this.ds = ds;
    }

    public void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendlycore_hide_armor (
                  uuid TEXT PRIMARY KEY,
                  hidden INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    public boolean load(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT hidden FROM friendlycore_hide_armor WHERE uuid=?
             """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getInt(1) == 1;
            }
        }
    }

    public void upsert(UUID uuid, boolean hidden) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO friendlycore_hide_armor (uuid, hidden)
                 VALUES (?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET hidden=excluded.hidden
             """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, hidden ? 1 : 0);
            ps.executeUpdate();
        }
    }
}
