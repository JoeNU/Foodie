package db;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import java.text.ParseException;


// This class is similar to DBYelpImport; it's just a Mongo data base version. 
// We create two tables: users, restaurants. We also insert user information into users collection/table.
// Key point: we don't have history table here, but MySQL database has that. Why? Because history is the relation table,
//			  we don't use ER model in NoSQL. The relation can be put in users collection/table using another field.
public class MongoDBImport {
	// note: 这里用了main函数，所以可以作为单独的程序运行，通常我们只运行一次。
	public static void main(String[] args) throws ParseException {
		MongoClient mongoClient = new MongoClient(); // 获得一个跟本机上MongDB server程序的连接接口
		MongoDatabase db = mongoClient.getDatabase(DBUtil.DB_NAME); // get the database named laiproject
		// 在"users"这个collection(即table)里，insert一个新的document(very similar to JSON object), the field values of this user are inserted by append 
		db.getCollection("users").insertOne(new Document().append("first_name", "John").append("last_name", "Smith")
				.append("password", "3229c1097c00d497a0fd282d586be050").append("user_id", "1111"));
		// make sure user_id is unique.
		IndexOptions indexOptions = new IndexOptions().unique(true);

		// use 1 for ascending index , -1 for descending index
		db.getCollection("users").createIndex(new Document("user_id", 1), indexOptions);

		// make sure business_id is unique.
		db.getCollection("restaurants").createIndex(new Document("business_id", 1), indexOptions);

		// use a compound text index of name, full_address and categories for
		// search.
		db.getCollection("restaurants").createIndex(
				new Document().append("categories", "text").append("full_address", "text").append("name", "text"));
		mongoClient.close();
	}
}
