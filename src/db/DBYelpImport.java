package db;

// 这里我们import都是java.sql下的library
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class helps drop, create database tables, and insert rows in MySQL.
 * Note: basically, you use this class to initialize and set up the MySQL database.  
 *       In addition, this class is independent of the web server project, so
 *       you can directly run it as Java application. 如果你只run server的话，它自己是不会run的。
 *       在这个单独的程序(i.e., DBYelpImport)里，我们对于connection的使用，就是new一个connection, 一直使用就可以了，因为
 *       只有main函数会使用它。
 *       此外，这里并没有和前端的http method, 即post, delete,和get连系起来，它只是独自地drop, create database tables, 
 *       and insert rows in MySQL，但是它的影响是定义了MySQL database里的table schema.
 * Note: 其实你可以将此class命名为MySQLDBImport      
 */

public class DBYelpImport {
	// Note: Since we have main function here, we can right click and run as Java application.
	// 所以, 我们可以把它当作普通的Java程序执行，而不是作为web server一部分去执行。因为create/update database是很危险的操作，
	// 我们不应该将此功能开放给前端的用户，而是由后端的engineers去手工执行，所以我们把它写成main函数。Web server不会运行它，即server每次启动时，不会重新run它。
	// 如果运行一段时间后，database出错了，那么我就可以重新对这个main函数run as Java application。
	// 其实这个class不是web application/server的一部分，但是它利用了DBUtil.java的内容，这样我们不用再输入一遍密码。
	public static void main(String[] args) {
		try {
			// Ensure the driver is imported.
			Class.forName("com.mysql.jdbc.Driver").newInstance(); // initialize jdbc driver. Note: 在路径里找到一个叫com.mysql.jdbc.Driver的java class, 然后new一个instance, 
			                                                      // 不返回任何动作。若能new成功，说明你的library import进来了。这其实是class loader的一个写法。																		
			Connection conn = null; // 打开一个database的connection. Note: 当你打开一个connection, 相当于一个管道，连接了你和MySQL, 

			try {
				System.out.println("Connecting to \n" + DBUtil.URL);
				// DBUtil.URL has all info like username and password, etc., we use it to connect to MySQL and get
				// a connection object，若成功，代表你能够initialize它。
				// Note：得到和MySQL的连接后，通过它对MySQL进行操作。
				conn = DriverManager.getConnection(DBUtil.URL); // 通过DriverManager来实现，getConnection是个静态的method.
			} catch (SQLException e) {
				// 常见错误：端口错了，密码错了，hostname错了，MAMP没有运行
				System.out.println("SQLException " + e.getMessage());
				System.out.println("SQLState " + e.getSQLState());
				System.out.println("VendorError " + e.getErrorCode());
			}
			
			if (conn == null) {// connection连接不成功
				return; // 一旦连接不成功，直接return, 否则后面全部出错。
			}
			
			// Note: 下面三个是关于怎么对database进行操作。
			
			// Step 1：Drop/Delete tables in case they exist.
			Statement stmt = conn.createStatement(); // statement is the object used for executing a static SQL statement and returning the results it produces.
			
			// Note: 理论上你可以把所有sql语句写在一个双引号里，用分号隔开；但是这样要是出错了的话，很难debug.
			String sql = "DROP TABLE IF EXISTS history"; // 每次重启server后，如果存在老的history table, delete it. Note: 必须加IF EXISTS, 否则table不存在的话，会报错。  
			stmt.executeUpdate(sql);

			sql = "DROP TABLE IF EXISTS restaurants";
			stmt.executeUpdate(sql);

			sql = "DROP TABLE IF EXISTS users";
			stmt.executeUpdate(sql);

			// Step 2: Create tables. Note: 也可以通过load schema去create table，但因为我们是Java project, 我们就在Java里create.
			
			// create table, whose name is restaurants
			// Note: field name data type, e.g., for String type, we use VARCHAR, 255个字节，NOT NULL means its value cannot be null
			// 		 这里我们禁止business_id为null, 因为它是primary key;其它fields的值可以没有；SQL里的FLOAT类似于Java中的floot；这里的field值来源于Yelp API的数据，我们选用存储了对我们有用的部分。
			//       primary key在整个table里面必须是unique的。其它的fields可以有duplicate values. primary key相当于一个独立的指针，它指向表中特定的某一行。
			sql = "CREATE TABLE restaurants " + "(business_id VARCHAR(255) NOT NULL, " + " name VARCHAR(255), "
					+ "categories VARCHAR(255), " + "city VARCHAR(255), " + "state VARCHAR(255), " + "stars FLOAT,"
					+ "full_address VARCHAR(255), " + "latitude FLOAT, " + " longitude FLOAT, "
					+ "image_url VARCHAR(255)," + "url VARCHAR(255)," + " PRIMARY KEY ( business_id ))";
			stmt.executeUpdate(sql);
			
			// create table, whose name is users
			sql = "CREATE TABLE users " + "(user_id VARCHAR(255) NOT NULL, " + " password VARCHAR(255) NOT NULL, "
					+ " first_name VARCHAR(255), last_name VARCHAR(255), " + " PRIMARY KEY ( user_id ))";
			stmt.executeUpdate(sql);
			
			// create table, whose name is history
			// Note: why we need visit_history_id? Because multiple users can visit the same restaurant, also
			//		 a user can visit multiple restaurants, thus user_id is not unique, business_id is not unique,
			//       user_id plus business_id is not unique. visit_history_id is the key we manually create, actually
			//       we don't care about its values, it basically count how many entries we have; AUTO_INCREMENT increases
			//		 previous field value by 1 each time you insert a new entry. For a database, it's ok to not have key,
			//		 but having key is good for potential extension later. 这里time stamp很有用，如果做extension, 那么此人是
			//       最近一段时间访问这个餐馆，还是很久以前访问过，就可以考虑进去了。还有它访问了同一个餐馆多少次，也让这个餐馆的重要程度不一样。
			//       FOREIGN KEY (business_id) 是指history table里的user_id，restaurants(business_id)指的是restaurants table
			//       里的business_id，REFERENCES意思是像指针一样，history table里的user_id指向restaurants table里的business_id，那么
			//       history table里的user_id就必须是restaurants table里的business_id范围内的值，若出现了范围外的值，则不能让你将此数据
			//		 写进table, 保证了系统的干净性，因为之后我们会根据访问历史去推荐餐馆，倘若一个被访问过的餐馆未曾出现在restaurants table中过，
			//       那么我们很难对此做出推荐。此外，history table的创建必须在最后，因为它reference了restaurants和users table,
			//		 否则会出error, 不执行。
			sql = "CREATE TABLE history " + "(visit_history_id bigint(20) unsigned NOT NULL AUTO_INCREMENT, "
					+ " user_id VARCHAR(255) NOT NULL , " + " business_id VARCHAR(255) NOT NULL, "
					+ " last_visited_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ " PRIMARY KEY (visit_history_id),"
					+ "FOREIGN KEY (business_id) REFERENCES restaurants(business_id),"
					+ "FOREIGN KEY (user_id) REFERENCES users(user_id))";
			stmt.executeUpdate(sql);

			// Step 3: Insert data
			// Note: Here we create a fake user. Here we insert data offline.
			sql = "INSERT INTO users " + "VALUES (\"1111\", \"3229c1097c00d497a0fd282d586be050\", \"Joe\", \"Jiang\")";

			System.out.println("\nDBYelpImport executing query:\n" + sql);
			stmt.executeUpdate(sql);

			System.out.println("DBYelpImport: import is done successfully.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
