package offline;

import java.io.BufferedReader;
import java.io.FileReader;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import db.DBUtil;


// 打开MongoDB connection, 将原始的csv文件处理导入到MongoDB database
// Note: 这个class类似于MongoDBImport in package db
public class Purify {
	public static void main(String[] args) {
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase(DBUtil.DB_NAME);
		// The name of the file to open.
		// Windows is different : C:\\Documents\\ratings_Musical_Instruments.csv
		String fileName = "/Users/Joe/Desktop/LaiOffer/LaiProject/Back_End/ratings_Musical_Instruments.csv";

		String line = null;

		try {
			FileReader fileReader = new FileReader(fileName); // 打开文件

			BufferedReader bufferedReader = new BufferedReader(fileReader); // 用BufferedReader()因为文件比较大
			while ((line = bufferedReader.readLine()) != null) {
				String[] values = line.split(","); // 因为input是csv file, 所以是comma-separated.
				// 在我们新建的"ratings" table里插入数据，分别插入user，item，rating三个column 
				// note: 我们将rating的string格式转换成double.
				db.getCollection("ratings").insertOne(new Document().append("user", values[0]).append("item", values[1])
						.append("rating", Double.parseDouble(values[2])));

			}
			System.out.println("Import Done!");
			bufferedReader.close();
			mongoClient.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
