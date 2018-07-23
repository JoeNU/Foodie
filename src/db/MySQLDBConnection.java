package db;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Restaurant;

import org.json.JSONArray;
import org.json.JSONObject;



import java.sql.Connection;
import java.sql.PreparedStatement;

import yelp.YelpAPI;
import yelp.YelpFusion;

// This is detailed implementation of DBConnection interface 
// for MySQL data base.
// Note: 1. searchRestaurants()是此class(或者说DBConnection)最核心的method, 因为它把前后端内容串接起来了！
//       2. Connection conn是个instance/object, 本质上是个接口，你能对它进行操作, 跟外界进行联系。

public class MySQLDBConnection implements DBConnection {
	// May ask for implementation of other methods. Just add empty body to them.
	private Connection conn = null; // as a class member, all methods can access conn. Note：this is the typical way of using connection in a Java class.
	private static final int MAX_RECOMMENDED_RESTAURANTS = 10;

	public MySQLDBConnection() {
		this(DBUtil.URL);
	}
	
	// constructor其实起了打开连接的作用。
	public MySQLDBConnection(String url) { // note: this url is the one for database!
		try {
			// Forcing the class representing the MySQL driver to load and
			// initialize.
			// The newInstance() call is a work around for some broken Java
			// implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url); // save the obtained connection as a member of class MySQLDBConnection
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 关闭连接
	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close(); // close the connection
			} catch (Exception e) { /* ignored */
			}
		}
	}

	
	// Note: 以上是关于member fields和construction的函数，所以放在最上面；下面是功能性的函数。
	
	
	// searchRestaurants()是此class(或者说DBConnection)最核心的method, 因为它把前后端内容串接起来了！
	// Note: searchRestaurants() has done two things:
	//       (1) get response from Yelp, then从Yelp的response中获取"businesses"这个field对应的JSON array，因为它的structure相对比较乱且杂，
	//           我们遍历它，把里面的每个JSON object, 依次转化成Restaurant object, 再转化成JSON object, 再根据此object
	//           是否包含搜索关键字term, 来确定是否存到一个List中去。最终，将这个List转成JSON array, 返回给前端。
	//		 (2) store each converted Restaurant object into database
	
	@Override
	public JSONArray searchRestaurants(String userId, double lat, double lon, String term) {
//		try {
//			// Connect to Yelp API
//			YelpAPI api = new YelpAPI();
//			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
//			JSONArray array = (JSONArray) response.get("businesses");
//
//			List<JSONObject> list = new ArrayList<>();
//
//			for (int i = 0; i < array.length(); i++) {
//				JSONObject object = array.getJSONObject(i);
//				// Clean and purify： convert Yelp data to restaurant object
//				Restaurant restaurant = new Restaurant(object);
//				// return clean restaurant objects: convert restaurant object to JSON object
//				JSONObject obj = restaurant.toJSONObject();
//				list.add(obj);
//			}
//			return new JSONArray(list);
//		} catch (Exception e) {
//			System.out.println(e.getMessage());
//		}
//		return null;

		try {
			// Fetch data from Yelp
			YelpFusion api = new YelpFusion();
			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");
			
			List<JSONObject> list = new ArrayList<JSONObject>();
			Set<String> visited = getVisitedRestaurants(userId);// get visited history for this user
			
			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				//System.out.println(object);
				// Step 1: create restaurant object: convert Yelp data to restaurant object. Note: this step also purifies data.
				Restaurant restaurant = new Restaurant(object);
				// Step 2: read data from restaurant object
				String businessId = restaurant.getBusinessId();
				String name = restaurant.getName();
				String categories = restaurant.getCategories();
				String city = restaurant.getCity();
				String state = restaurant.getState();
				String fullAddress = restaurant.getFullAddress();
				double stars = restaurant.getStars();
				double latitude = restaurant.getLatitude();
				double longitude = restaurant.getLongitude();
				String imageUrl = restaurant.getImageUrl();
				String url = restaurant.getUrl();
				// Step 3: convert restaurant object to JSON object
				JSONObject obj = restaurant.toJSONObject();
				// update "is_visited" field based on visited history
				// if (visited.contains(businessId)) {
				//    obj.put("is_visited", true);
				// } else {
				//    obj.put("is_visited", false);
				// }
				
				// Step 4: Put data into database
				// Note: 1. 这里很重要的一个作用是避免了SQL injection，因为PreparedStatement会帮你在string内加双引号，e.g., "1111 OR 1=1" ----> "\"1111 OR 1=1\""
				//       2. 你可以通过setString()去replace ? with specific field values.
				//       3. IGNORE的意思是，如果你想要插入的某条record对应的primary key已经存在，那么让SQL不报错，而是
				//          选择不插入这条，继续执行接下来的命令。
				//       4. 分成很多行写代码，方便你debug.
				String sql = "INSERT IGNORE INTO restaurants VALUES (?,?,?,?,?,?,?,?,?,?,?)";  
				PreparedStatement statement = (PreparedStatement) conn.prepareStatement(sql);
				statement.setString(1, businessId); // replace first question mark with businessId
				statement.setString(2, name);
				statement.setString(3, categories);
				statement.setString(4, city);
				statement.setString(5, state);
				statement.setDouble(6, stars);
				statement.setString(7, fullAddress);
				statement.setDouble(8, latitude);
				statement.setDouble(9, longitude);
				statement.setString(10, imageUrl);
				statement.setString(11, url);
				statement.execute();
				// Perform filtering if term is specified. Note：this part involves index table.
				if (term == null || term.isEmpty()) {
					list.add(obj);
				} else {// if term is specified
					if (categories.contains(term) || fullAddress.contains(term) || name.contains(term)) {
						list.add(obj);
					}
				}
			}
			// this JSONArray will be present to front end.
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	
	// Note: setVisitedRestaurants()与unsetVisitedRestaurants()使得我们可以对history table做操作，但是同时
	//       我们在API层次也得相应连接起来，即对api package里的visitHistory.java里的doPost()和doDelete()里分别
	//		 使用setVisitedRestaurants()与unsetVisitedRestaurants()。
	//		 Note1: 这里return type是boolean, 当你插入的business_id不valid时，可以给你提醒, 是否成功，因为
	//		        visitHistory.java里的doPost()会用到这个method, 这个返回的boolean会影响到返回给前端什么信息。
	//       Note2: 这里我们只考虑了SQL exception, 但现实中可能有各种exception, 那么可以用enum作为返回类型，然后
	//				在visitHistory.java里的doPost()里会根据返回的不同的enum值去给前端返回相应的信息。之所以用enum而不用
	//				integer是因为，enum中的mapping(什么exception对应什么integer)值非常清晰，但若返回类型为integer，integer值
	//              本身在后端传来传去概念上不是很清楚。而且直接读代码的时候，出现一行，比如，return 1, 那么就比较confusing, 1
	//				代表什么.	若还想处理未知的exception, 那么可以在enum中定义一个default value, 比如0，来对应unknown exception, 
	//              这种exception只能由后端去处理。
	@Override
	public boolean setVisitedRestaurants(String userId, List<String> businessIds) { // Note: this is used in http doPost method in visitHistory.java
		String query = "INSERT INTO history (user_id, business_id) VALUES (?, ?)"; // Note：第一个问号对应user_id， 第二个问号对应business_id
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String businessId : businessIds) {
				statement.setString(1, userId); // Note：userId is the field in Java; user_id is the field in database.
				statement.setString(2, businessId);
				statement.execute();
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	// Note: 这里PreparedStatement作用(即避免SQL injection)就显示出来了："1111 OR 1=1" ----> "\"1111 OR 1=1\""
	//		 它可以使得 DELETE FROM history WHERE user_id = 1111 OR 1=1
	//       =======> DELETE FROM history WHERE user_id = "1111 OR 1=1" 
	//       
	@Override
	public void unsetVisitedRestaurants(String userId, List<String> businessIds) { // Note: this is used in http doDelete method in visitHistory.java
		// 这个query会删掉任何符合这两个given id的entry； 同一个用户可能访问一个餐馆多次，每次有不同的history id, 但这些entries都会被删掉！
		// Note：这个都删掉的结果，符合用户的心理，因为用户已经不喜欢这个餐馆了，那么相应的记录都应删了。
		String query = "DELETE FROM history WHERE user_id = ? and business_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String businessId : businessIds) {
				statement.setString(1, userId);
				statement.setString(2, businessId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	
	// return the set of visited restaurants for a given user id 
	// Note: this is the helper function for step 1 (of engineering design) in recommendRestaurants(String userId).
	@Override
	public Set<String> getVisitedRestaurants(String userId) {
		Set<String> visitedRestaurants = new HashSet<String>();
		try {
			String sql = "SELECT business_id from history WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String visitedRestaurant = rs.getString("business_id");
				visitedRestaurants.add(visitedRestaurant);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return visitedRestaurants;
	}

	// Note: recommendRestaurants() 是根据教案里的engineering design的四步来做的。
	@Override
	public JSONArray recommendRestaurants(String userId) {
		try {
			if (conn == null) {
				return null;
			}
			
			// step 1: given user id, fetch his visited restaurants 
			Set<String> visitedRestaurants = getVisitedRestaurants(userId); 
			
			// step 2: given restaurant ids, find their categories.  note: use hashSet to avoid duplicate category; this hashset is only stored in memory not in database 
			Set<String> allCategories = new HashSet<>();  

			for (String restaurant : visitedRestaurants) { // Note: restaurant is actually restaurant ID
				allCategories.addAll(getCategories(restaurant));
			}
			
			// step 3: find the restaurants corresponding to the given categories
			// Note: we use set because a restaurant can have multiple categories, but we just wanna
			//       display the same restaurant once.
			Set<String> allRestaurants = new HashSet<>(); 

			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allRestaurants.addAll(set);// addAll all elements in set to allRestaurants if these elements are not present in allRestaurants
			}
			
			// step 4 (filtering): get the final recommended restaurants note: diff stores the restaurant objects because we will present the actual objects to front end instead of just restaurant ids
			Set<JSONObject> diff = new HashSet<>(); // diff is the result of filtering out visitedRestaurants from allRestaurants

			int count = 0;
			for (String businessId : allRestaurants) {
				// Perform filtering
				if (!visitedRestaurants.contains(businessId)) {
					diff.add(getRestaurantsById(businessId, false));
					count++;
					if (count >= MAX_RECOMMENDED_RESTAURANTS) { // note: only recommend a limited number of restaurants because the front end is waiting for the recommendation, you don't want loading time is too long.  
						break;
					}
				}
			}
			return new JSONArray(diff);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	
	// Note: this is the helper function for step 2 (of engineering design) in recommendRestaurants(String userId).
	@Override
	public Set<String> getCategories(String businessId) { // note: one businessId only have one categories string which may contain multiple categories
		try {
			String sql = "SELECT categories from restaurants WHERE business_id = ? "; // sql query string

			PreparedStatement statement = conn.prepareStatement(sql); // statement is to replace ? in sql query string

			statement.setString(1, businessId); // 1 means the first ?
			ResultSet rs = statement.executeQuery(); // executeQuery: execute and read data from DB (要读数据时用executeQuery)
													 // executeUpdate: just execute, say, writing to DB;
													 // ResultSet is an iterator
			// Note: 因为category这个field顶多只可能有单个String值，所以用if loop.
			if (rs.next()) { // next() checks if rs has next item (like hasNext()): if yes, point to it (like next()) and return true; if not, just return false. Note: 这里的ResultSet这个iterator, 相当于合并了普通Java iterator的hasNext()和next(). rs.next()背后是Java的iterator.
				Set<String> set = new HashSet<>();
				String[] categories = rs.getString("categories").split(","); // An example of category entry in categories table is "Japanese, Korean, Spanish"
				for (String category : categories) {
					set.add(category.trim()); // trim() removes empty space in string: e.g., ' Japanese ' -> 'Japanese'
				}
				return set;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return new HashSet<String>();
	}
	
	
	// Given a category, fetch the corresponding business ids.
	// Note: this is the helper function for step 3 (of engineering design) in recommendRestaurants(String userId).
	@Override
	public Set<String> getBusinessId(String category) {
		Set<String> set = new HashSet<>();
		try {
			// e.g., if category = Chinese, categories = Japanese, Chinese, Korean, then it's a match			 
			String sql = "SELECT business_id from restaurants WHERE categories LIKE ?"; // LIKE means contain    note: 这个涉及到regular expression.
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, "%" + category + "%"); // this syntax corresponds to LIKE. Note: MySQL不直接支持regular repression, 所以才会这么写。
			ResultSet rs = statement.executeQuery(); // ResultSet是SQL特有的iterator. 
			while (rs.next()) { // a category can have multiple BusinessId, so we use while loop.
				String businessId = rs.getString("business_id");
				set.add(businessId);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return set;
	}

	
	// Note: this is the helper function for step 4 (of engineering design) in recommendRestaurants(String userId).
	@Override
	public JSONObject getRestaurantsById(String businessId, boolean isVisited) { // note: isVisited是前端传来的，它知道有没访问过此餐馆；我们会将此信息放入restaurant object中。
		try {
			String sql = "SELECT * from restaurants where business_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, businessId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				Restaurant restaurant = new Restaurant(
						rs.getString("business_id"), rs.getString("name"),
						rs.getString("categories"), rs.getString("city"),
						rs.getString("state"), rs.getFloat("stars"), // the field value corresponding to "stars" is float type, so we use getFloat()
						rs.getString("full_address"), rs.getFloat("latitude"),
						rs.getFloat("longitude"), rs.getString("image_url"),
						rs.getString("url"));
				JSONObject obj = restaurant.toJSONObject(); // convert Java object to JSONObject
				obj.put("is_visited", isVisited);
				return obj; // return obj to front end
			}
		} catch (Exception e) { /* report an error */
			System.out.println(e.getMessage());
		}
		return null;
	}

	// Verify userId matches password
	@Override
	public Boolean verifyLogin(String userId, String password) {
		try {
			if (conn == null) {
				return false;
			}
			
//			// note: 这段代码会导致SQL injection.
//			String sql = "SELECT user_id from users WHERE user_id='" + userId
//					+ "' and password='" + password + "'";
//			System.out.println(sql);
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);

			String sql = "SELECT user_id from users WHERE user_id = ? and password = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			// note: PreparedStatement会compare语句, 去确认userId就是userId, password就是password.
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	@Override
	public String getFirstLastName(String userId) {
		String name = "";
		try {
			if (conn != null) {
				String sql = "SELECT first_name, last_name from users WHERE user_id = ?";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					name += rs.getString("first_name") + " " + rs.getString("last_name");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return name;
	}
}
