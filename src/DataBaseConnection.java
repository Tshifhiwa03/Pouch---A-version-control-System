import java.sql.Connection;
import java.sql.DriverManager;

public class DataBaseConnection {
    private Connection databaselink;

    public Connection getConnection() {
        String databaseName = "softwareprogramming";
        String databaseUser = "root"; // your MySQL username
        String dataPassword = "GhRyawbU@6"; // your MySQL password
        String url = "jdbc:mysql://localhost:3306/" + databaseName + "?useSSL=false";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            databaselink = DriverManager.getConnection(url, databaseUser, dataPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return databaselink;
    }
}
