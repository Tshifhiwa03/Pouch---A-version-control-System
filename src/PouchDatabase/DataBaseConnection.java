package PouchDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles database connections for any MySQL database.
 * You can specify database name, username, and password at runtime.
 */
public class DataBaseConnection {

    // MySQL JDBC driver
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public DataBaseConnection() {
        try {
            Class.forName(DRIVER); // Load JDBC driver
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    /**
     * Returns a Connection object to the specified database.
     *
     * @param databaseName The name of the database to connect to
     * @param user         The database username
     * @param password     The database password
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public Connection getConnection(String databaseName, String user, String password) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/" + databaseName +
                     "?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Convenience method for connecting to the 'users' database.
     */
    public Connection getUsersDatabaseConnection() throws SQLException {
        return getConnection("users", "root", "DrTnet@170621");
    }

    /**
     * Convenience method for connecting to the 'softwareprogramming' database.
     */
    public Connection getSoftwareProgrammingDatabaseConnection() throws SQLException {
        return getConnection("softwareprogramming", "root", "GhRyawbU@6");
    } 
    
    public Connection getLoginsDatabaseConnection() throws SQLException {
        return getConnection("logins", "root", "Leandra@mysql24");
    } 
}
