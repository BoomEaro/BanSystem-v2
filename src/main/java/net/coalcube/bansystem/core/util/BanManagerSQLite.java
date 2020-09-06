package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManagerSQLite implements BanManager {

    private final SQLite sqlite;

    public BanManagerSQLite(SQLite sqlite) {
        this.sqlite = sqlite;
    }

    public void log(String action, String creator, String target, String note) throws SQLException {
        sqlite.update("INSERT INTO `logs` (`action`, `target`, `creator`, `note`, `creationdate`) " +
                "VALUES ('" + action + "', '" + target + "','" + creator + "', '" + note + "', CURRENT_TIMESTAMP);");
    }

    public void kick(UUID player, String creator) throws SQLException {
        kick(player, creator, "");
    }

    public void kick(UUID player, UUID creator) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public void kick(UUID player, String creator, String reason) throws SQLException {
        sqlite.update("INSERT INTO `kicks` (`player`, `creator`, `reason`, `creationdate`) " +
                "VALUES ('" + player + "', '" + creator + "', '" + reason + "', CURRENT_TIMESTAMP);");
    }

    public void kick(UUID player, UUID creator, String reason) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason, v4adress);
    }

    public void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason);
    }

    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        sqlite.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', CURRENT_TIMESTAMP, '" + creator + "', '" + reason + "', '" + v4adress.getHostName() + "', '" + type + "');");

        sqlite.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `ip`, `type`, `creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', " +
                "'" + v4adress.getHostName() + "', '" + type + "', CURRENT_TIMESTAMP);");
    }

    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException, SQLException {
        sqlite.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', CURRENT_TIMESTAMP," +
                " '" + creator + "', '" + reason + "', '','" + type + "');");



        sqlite.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `type`, `ip`,`creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + type + "', '', CURRENT_TIMESTAMP);");
    }

    public void unBan(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unBan(player, unBanner.toString(), reason);
    }

    public void unBan(UUID player, String unBanner, String reason) throws IOException, SQLException {
        sqlite.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.NETWORK + "'");
        sqlite.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', CURRENT_TIMESTAMP, '" + reason + "','" + Type.NETWORK +"');");
    }

    public void unBan(UUID player, UUID unBanner) throws IOException, SQLException {
        unBan(player, unBanner);
    }

    public void unBan(UUID player, String unBanner) throws IOException, SQLException {
        sqlite.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.NETWORK + "'");
        sqlite.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', CURRENT_TIMESTAMP, '" + Type.NETWORK +"');");

    }

    public void unMute(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unMute(player, unBanner.toString(), reason);
    }

    public void unMute(UUID player, String unBanner, String reason) throws IOException, SQLException {
        sqlite.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.CHAT + "'");
        sqlite.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', CURRENT_TIMESTAMP, '" + reason + "','" + Type.CHAT +"');");
    }

    public void unMute(UUID player, UUID unBanner) throws IOException, SQLException {
        unMute(player, unBanner.toString());
    }

    public void unMute(UUID player, String unBanner) throws IOException, SQLException {
        sqlite.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.CHAT + "'");
        sqlite.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', CURRENT_TIMESTAMP,'" + Type.CHAT +"');");

    }

    public void deleteHistory(UUID player) throws SQLException {
        sqlite.update("DELETE FROM `banhistories` WHERE player = '" + player + "';");

    }

    public void setIP(UUID player, InetAddress address) throws SQLException {
        sqlite.update("UPDATE `bans` SET ip='" + address.getHostName() + "' WHERE ip IS NULL;");
    }

    public String getBanReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public Long getEnd(UUID player, Type type) throws SQLException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT duration FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            Long duration = resultSet.getLong("duration");

            return (duration == -1) ? duration : getCreationDate(player, type) + duration ;
        }
        return null;
    }

    public String getBanner(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT creator FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("creator");
        }
        return null;
    }

    public Long getRemainingTime(UUID player, Type type) throws SQLException, ParseException {
        return (getEnd(player, type) == -1) ? -1 : getEnd(player, type) - System.currentTimeMillis();
    }

    public String getReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public int getLevel(UUID player, String reason) throws UnknownHostException, SQLException {
        int lvl = 1;
        if(hasHistory(player, reason)) {
            ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "' AND reason = '" + reason + "';");
            while (resultSet.next()) {
                lvl ++;
            }
        }
        return lvl;
    }

    public Long getCreationDate(UUID player, Type type) throws SQLException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT creationdate FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        while (resultSet.next()) {
            return df.parse(resultSet.getString("creationdate")).getTime();
        }
        return null;
    }

    public List<History> getHistory(UUID player) throws UnknownHostException, SQLException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        List<History> list = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        while (resultSet.next()) {
            list.add(new History(UUID.fromString(
                    resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    df.parse(resultSet.getString("creationdate")).getTime(),
                    resultSet.getLong("duration"),
                    Type.valueOf(resultSet.getString("type")),
                    InetAddress.getByName(resultSet.getString("ip"))));
        }
        return list;
    }

    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE ip = '" + address.getHostName() + "';");
        List<UUID> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(UUID.fromString(resultSet.getString("player")));
        }
        return list;
    }

    public boolean hasHistory(UUID player) throws UnknownHostException, SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean hasHistory(UUID player, String reason) throws UnknownHostException, SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player='" + player + "' AND reason='" + reason + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isBanned(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' and type = '" + type.toString() + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isSetIP(UUID player) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT ip FROM `bans` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            if(!resultSet.getString("ip").isEmpty())
                return true;
        }
        return false;
    }
}
