package offline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoDatabase;

import db.DBUtil;


// Use collaborative-filtering algorithm to do recommendation, so we need to calculate the similarity score;
// use MapReduce model to compute the similarity

public class Prediction {
	// note: 经过各种尝试发现，对这两个user(AWLVQ1NSU3LDS 和 A1GO6VJZN0UDLN)的推荐, 效果比较好，其它user的数据比较sparse, 效果不好
	private static final String USER_ID = "AWLVQ1NSU3LDS"; // 	target user
	private static final String COLLECTION_NAME = "ratings"; // ratings table里有三个column: user, item, rating
	private static final String USER_COLUMN = "user";
	private static final String ITEM_COLUMN = "item";
	private static final String RATING_COLUMN = "rating";

	public static void main(String[] args) {
		// Init: 打开connection
		MongoClient mongoClient = new MongoClient();  
		MongoDatabase db = mongoClient.getDatabase(DBUtil.DB_NAME);

		// Get USER_ID's purchase records
		List<String> previousItems = new ArrayList<>(); // target user's previous items
		List<Double> previousRatings = new ArrayList<>(); // target user's previous ratings
			// new Document(USER_COLUMN, USER_ID) 选择USER_COLUMN中对应的USER_ID那一行
		FindIterable<Document> iterable = db.getCollection(COLLECTION_NAME).find(new Document(USER_COLUMN, USER_ID));
		
		iterable.forEach(new Block<Document>() { // anonymous class. Note: block is an interface, which has a method named apply()
			@Override
			public void apply(final Document document) {
				previousItems.add(document.getString(ITEM_COLUMN)); // 存入list
				previousRatings.add(document.getDouble(RATING_COLUMN)); // 存入list
			}
		});
		
		
		/**
		 * note: 我们的mapper函数就是如下的样子
		 * var map = function() {
		 *   if (this.item == "0634029363" && this.rating == 2) {
		 *  	emit(this.user, 1); 
		 *   } 
		 *   if (this.item == "B000TZTPQ6" && this.rating == 5) {
		 *      emit(this.user, 1); 
		 *   } 
		 *   if (this.item == "B001QE994M" && this.rating == 4) {
		 *  	emit(this.user, 1); 
		 *   } 
		 *   if (this.item == “B001QE997E" && this.rating == 5) { 
		 *   	emit(this.user, 1);
		 * 	 }
		 * }
		 */

		// Construct mapper function
		// Note: MongoDB没有支持能在Java里写代码的api, 所以我们得把mapper function写在String里. 同样地，reducer function也得写在String里.
		StringBuilder sb = new StringBuilder();
		sb.append("function() {");

		for (int i = 0; i < previousItems.size(); i++) {
			String item = previousItems.get(i);
			Double rating = previousRatings.get(i);
			sb.append("if (this.item == \"");
			sb.append(item);
			sb.append("\" && this.rating == ");
			sb.append(rating);
			sb.append(" ){ emit(this.user, 1); }");
		}
		sb.append("}");
		String map = sb.toString();
		// Construct a reducer function
		String reduce = "function(key, values) {return Array.sum(values)} ";

		// MapReduce
		MapReduceIterable<Document> results = db.getCollection(COLLECTION_NAME).mapReduce(map, reduce);
		// Need a sorting here
		List<User> similarUsers = new ArrayList<>();
		results.forEach(new Block<Document>() { // anonymous class
			@Override
			public void apply(final Document document) {
				String id = document.getString("_id"); // user_id
				Double value = document.getDouble("value"); // value就是similarity score, 即此user和target user对同一产品有相同的rating的次数
				if (!id.equals(USER_ID)) { // note: 因为不需要计算target user和自身的相似度，所以跳过它
					similarUsers.add(new User(id, value));
				}
			}
		});
		// printList(similarUsers);
		// System.out.println("\n\n\n");
		Collections.sort(similarUsers); // 按照similarity score, 即value大小进行排序
		printList(similarUsers);

		// Get similar users' previous records order by similarity，so then we can recommend items to target user
		Set<String> products = new HashSet<>(); // 要推荐给target user的产品
		for (User user : similarUsers) {
			String id = user.getId();
			iterable = db.getCollection(COLLECTION_NAME).find(new Document(USER_COLUMN, id)); //  a user may have rated multiple items

			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					String item = document.getString(ITEM_COLUMN);
					if (!previousItems.contains(item)) { // filter out target user已经喜欢过的items, 只推荐新的产品
						products.add(document.getString(ITEM_COLUMN));
					}
				}
			});
			
			if (products.size() > 5) {
				break;
			}
		}

		for (String product : products) {
			System.out.println("Recommended product: " + product);
		}

		mongoClient.close();
	}

	
	// printList() is a helper function to print out recommendation result.
	private static void printList(List<User> similarUsers) {
		for (User user : similarUsers) {
			System.out.println(user.getId() + "," + user.getValue());
		}
	}
}
