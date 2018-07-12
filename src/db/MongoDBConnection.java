package db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Restaurant;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import yelp.YelpAPI;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;


// 这两个import要手工加，eclipse没法自动加，它们是我们要用的static function
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;




/**
 * 当你去implement DBConnection这个interface时，你就要实现这个interface里的methods, 这是从上一个level, 即Business Level/API level
 * 对你的data base实现的要求。
 */

public class MongoDBConnection implements DBConnection {
	private static final int MAX_RECOMMENDED_RESTAURANTS = 10;

	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient(); // 连接到MongoDB
		db = mongoClient.getDatabase(DBUtil.DB_NAME); // 打开db laiproject
	}
	
	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}
	
	
	// update value：set visited restaurants in users collection
	// Note: unlike MySQL, we have history table; in MongoDB, there is no such table. We add 
	//		 the history(i.e., visited restaurants) in users collection.
	@Override
	public boolean setVisitedRestaurants(String userId, List<String> businessIds) {
		// 当"user_id"等于给定的userId时, update相应的值; pushAll就是append； 这里会在"visited"中添加给定的businessIds. Note: $pushAll已经不能用了。
		for (String businessId : businessIds) {
			db.getCollection("users").updateOne(new Document("user_id", userId),
	   			 new Document("$push", new Document("visited", businessId))); // note: 这里的new不是新加一个document于collection的意思， 而是新建一个document object的意思，因为这里函数updateOne要求你传进去object 
		}
		return true;
	}
	
	// update value：unset visited restaurants in users collection
	@Override
	public void unsetVisitedRestaurants(String userId, List<String> businessIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("visited", businessIds)));
	}
	
	// get the visited restaurants
	@Override
	public Set<String> getVisitedRestaurants(String userId) {
		Set<String> set = new HashSet<>();
		// FindIterable是个可以iterate的东西，比如，list, set之类的；Document类似于JSON object 
		// note: command line: db.users.find({user_id:userId})
		//       其实new Document("user_id", userId)就是产生了{user_id:userId}
		//       这里传入了query
		FindIterable<Document> iterable = db.getCollection("users").find(new Document("user_id", userId));
		
		// .first()只返回一个entry;.containsKey("visited")查看是否包括visited这个field.
		if (iterable.first().containsKey("visited")) {
			// note: iterable.first().get("visited")是JSON array, 但是我们可以cast成List<String>
			List<String> list = (List<String>) iterable.first().get("visited");
			set.addAll(list); // add the list of string to the hashset
		}
		return set;
	}
	
	
	@Override
	public JSONObject getRestaurantsById(String businessId, boolean isVisited) {
		// eq() is a static function; eq("business_id", businessId) has the same effect as new Document("business_id", businessId)
		FindIterable<Document> iterable = db.getCollection("restaurants").find(eq("business_id", businessId)); // find the restaurant whose "business_id" value is equal to the given businessId
		try {
			return new JSONObject(iterable.first().toJson()); // convert the first object to JSON format
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	// note1: compared with the recommendRestaurants() in MySQLDBConnection.java, we have exactly the same function here,
	//       because this function is based on the same recommendation algorithm, which is independent of data base.
	//		 Actually, if our recommendation algorithm is very 
	// 		 complicated, we would put it in 单独的 layer. Since our algorithm is simple now and it's closely
	//       related to data base, we put it here.
	// note2: since we have duplicate codes in two different java file, this design is not good. We should use an
	//		  abstract class to implement DBConnection interface, and let MongoDBConnection and MySQLDBConnection
	//		  extend this abstract class. We can put recommendRestaurants() in it.
	@Override
	public JSONArray recommendRestaurants(String userId) {
		try {
			Set<String> visitedRestaurants = getVisitedRestaurants(userId);
			Set<String> allCategories = new HashSet<>();// why hashSet?
			for (String restaurant : visitedRestaurants) {
				allCategories.addAll(getCategories(restaurant));
			}
			Set<String> allRestaurants = new HashSet<>();
			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allRestaurants.addAll(set);
			}
			Set<JSONObject> diff = new HashSet<>();
			int count = 0;
			for (String businessId : allRestaurants) {
				// Perform filtering
				if (!visitedRestaurants.contains(businessId)) {
					diff.add(getRestaurantsById(businessId, false));
					count++;
					if (count >= MAX_RECOMMENDED_RESTAURANTS) {
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
	
	
	// return restaurants based on your geolocation
	// Note: 这个function和MySQLDBConnection.java中这个function基本是一样的，difference在于插入数据的具体语法不同，因为毕竟
	//       是不同的data base.
	@Override
	public JSONArray searchRestaurants(String userId, double lat, double lon, String term) {
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");

			List<JSONObject> list = new ArrayList<JSONObject>();
			Set<String> visited = getVisitedRestaurants(userId);

			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				Restaurant restaurant = new Restaurant(object);
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
				JSONObject obj = restaurant.toJSONObject();
				if (visited.contains(businessId)) {
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				// Question: why using upsert instead of insert directly?
				// Answer: 因为会出现duplicate business ids, 所以用upsert，它可以做到，如果已有这个id, update; 如果没有，insert.
				//         http://stackoverflow.com/questions/17319307/how-to-upsert-with-mongodb-java-driver
				UpdateOptions options = new UpdateOptions().upsert(true); // upsert means update or insert

				db.getCollection("restaurants").updateOne(new Document().append("business_id", businessId),
						new Document("$set", new Document().append("business_id", businessId).append("name", name)
								.append("categories", categories).append("city", city).append("state", state)
								.append("full_address", fullAddress).append("stars", stars).append("latitude", latitude)
								.append("longitude", longitude).append("image_url", imageUrl).append("url", url)),
						options);
				list.add(obj);
			}
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	
	@Override
	public Set<String> getCategories(String businessId) {
		Set<String> set = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("restaurants").find(eq("business_id", businessId));

		if (iterable.first().containsKey("categories")) {
			String[] categories = iterable.first().getString("categories").split(",");
			for (String category : categories) {
				set.add(category.trim()); // trim() is used to remove empty space
			}
		}
		return set;
	}

	
	@Override
	public Set<String> getBusinessId(String category) {
		Set<String> set = new HashSet<>();
		// note: regex("categories", category) is similar to LIKE %category% in MySQL. 
		//       regex("categories", category) means 如果"categories"对应的值里面包含给定的category，那么就是match上了。
		//       因为regex()返回的结果可能不止一个，所以用FindIterable.
		FindIterable<Document> iterable = db.getCollection("restaurants").find(regex("categories", category)); // regex means regular expression.
		// note 1: Block is an interface, which is implemented by an anonymous class, which overrides the method apply() in Block interface.
		// note 2: interface Block has a method named apply.  
		// note 3: Java的Comparator的用法跟这里是一样的。
		// note 4: 之所以用anonymous class，是为了让代码简洁，同时，你也只用它一次；当然，你也另外写一个class，但是不简洁。
		iterable.forEach(new Block<Document>() { // for each document in iterable, use block.apply() on it. Here block represents the instance of the anonymous class  
			@Override
			public void apply(final Document document) {
				set.add(document.getString("business_id"));
			}
		});
		return set;
	}
	
	
	@Override
	public Boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(new Document("user_id", userId));
		Document document = iterable.first();
		return document.getString("password").equals(password);
	}

	
	@Override
	public String getFirstLastName(String userId) {
		FindIterable<Document> iterable = db.getCollection("users").find(new Document("user_id", userId));
		Document document = iterable.first();
		String firstName = document.getString("first_name");
		String lastName = document.getString("last_name");
		return firstName + " " + lastName;
	}

}
