package db;

// This class has all the database related utility stuff, so we can reuse them in our project.
// Note: in our project, whenever you wanna access MySQL instance, you can refer to URL in this class.
//       we have all configuration here and make it consistent.

public class DBUtil {
	private static final String HOSTNAME = "localhost";
	//private static final String PORT_NUM = "3308"; // change it to your mysql port number if it's not 3306. My local MySQL uses port number 3308.
	private static final String PORT_NUM = "3306"; // on AWS, the default port number is 3306, so we need to set it be 3306 before exporting war file.
	public static final String DB_NAME = "laiproject"; // 我们在MAMP自己create了laiproject database
	private static final String USERNAME = "root";
	private static final String PASSWORD = "root"; 
	// Note: 1. jdbc是协议名字；mysql是其中一种实现；autoreconnect是指若连接断掉了，是否自动重新连接
	//       2. 这个URL组合了上面所有参数。
	//		 3. This URL for MySQL is very similar to http url.
	public static final String URL = "jdbc:mysql://" + HOSTNAME + ":" + PORT_NUM + "/" + DB_NAME + "?user=" + USERNAME
			                         + "&password=" + PASSWORD + "&autoreconnect=true";
}
