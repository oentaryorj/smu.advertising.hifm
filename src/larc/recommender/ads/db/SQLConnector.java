package larc.recommender.ads.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConnector {
	public static Connection connect(String schemaName) throws Exception{
		if (schemaName == null || schemaName.length() == 0) {
			throw new IllegalArgumentException(schemaName + " cannot be found.");
		}
		//String jdbc = "jdbc:mysql://10.0.106.40:3306/" + schemaName + "?zeroDateTimeBehavior=convertToNull";
		String jdbc = "jdbc:mysql://10.0.106.61:3306/" + schemaName + "?zeroDateTimeBehavior=convertToNull";
		String user = "roentaryo";
		String pass = "roentaryo123";
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection(jdbc, user, pass);
	}
	
	public static ResultSet query(Connection conn, String query) throws SQLException {
		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(Integer.MIN_VALUE);
		return stmt.executeQuery(query);
	}
}