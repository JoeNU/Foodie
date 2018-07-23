package model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// This class represents restaurant object, which can interact with front end
// and data base (in back end) in our web project.
// Note: we parse Yelp data by this class.

public class Restaurant {
	// the parameters we will present to the front end
	// note: (1) their values are parsed from Yelp response, which is a JSON object containing these parameters.
	//       (2) since we have private fields here, we need setter and getter functions to set and get values. 
	//           Eclipse can help you generate setter and getter functions 
	private String businessId;
	private String name;
	private String categories;
	private String city;
	private String state;
	private String fullAddress;
	private double stars;
	private double latitude;
	private double longitude;
	private String imageUrl;
	private String url;

	
	// initialize Restaurant object by a JSON object
	// Note: (1) basically, we convert a JSON object from front end to an object in Java by parsing.
	//       (2) by using this constructor, we can parse an ugly JSON object and convert it to a Restaurant object,
	//           so we can clean and purify not so well-organized JSON object (from Yelp data) in this way.
	//       (3) parse Yelp data and only keep some of the data which you need for front end rendering
	public Restaurant(JSONObject object) {
		try {
			if (object != null) {
				this.businessId = object.getString("id");
				// the value for "categories" is a JSON array of JSON objects
				JSONArray jsonArray = (JSONArray) object.get("categories");
				
				List<String> list = new ArrayList<>();
				// iterate over each JSON array in the outer JSON array
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject catObj = jsonArray.getJSONObject(i); // each category object has two fields: "alias", "title"
					// get the value corresponding to "alias", which is a trimmed "title"
					list.add(catObj.getString("alias"));
					//System.out.println(catObj.getString("alias"));
				}
				// convert list of String to a single String where each element in this String is separated by a comma
				this.categories = String.join(",", list);
				this.name = object.getString("name");
				this.imageUrl = object.getString("image_url");
				this.stars = object.getDouble("rating");
				// the value for "location" is a JSON object
				JSONObject location = (JSONObject) object.get("location"); // the return type for .get() is object, so you need to cast it to JSONObject, then you can assign it to location of JSONObject type.
				JSONObject coordinate = (JSONObject) object.get("coordinates");
				this.latitude = coordinate.getDouble("latitude");
				this.longitude = coordinate.getDouble("longitude");
				this.city = location.getString("city");
				this.state = location.getString("state");
				this.fullAddress = jsonArrayToString((JSONArray) location.get("display_address"));
				this.url = object.getString("url");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	// this constructor is for our later test use
	// Note: （1） if you use Generate Constructor using Fields in Source in Eclipse,
	//        the order of the parameters is not guaranteed.
	//       （2） initialize object by directly giving them the parameter values instead of using JSON array
	//	      to initialize it. This is good for test later.
	public Restaurant(String businessId, String name, String categories, String city, String state, double stars,
			String fullAddress, double latitude, double longitude, String imageUrl, String url) {
		this.businessId = businessId;
		this.categories = categories;
		this.name = name;
		this.city = city;
		this.state = state;
		this.stars = stars;
		this.fullAddress = fullAddress;
		this.latitude = latitude;
		this.longitude = longitude;
		this.imageUrl = imageUrl;
		this.url = url;
	}

	
	// convert Java object to JSON object, which can be present to front end
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("business_id", businessId);
			obj.put("name", name);
			obj.put("stars", stars);
			obj.put("latitude", latitude);
			obj.put("longitude", longitude);
			obj.put("full_address", fullAddress);
			obj.put("city", city);
			obj.put("state", state);
			obj.put("categories", stringToJSONArray(categories));
			obj.put("image_url", imageUrl);
			obj.put("url", url);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	


	// Note: parseString(), jsonArrayToString(), stringToJSONArray() are three helper functions.

	//  Perform data cleanup and purify data from Yelp API.
	//  Note: e.g., if you have a string, like "abc"edf"
	//              Java will recognize it as "abc" 
	//              because Java recognize the quotation mark as a special character.
	// 				我需要做特殊字符处理：将"abc"edf"变成"abc\"edf"
	//                                那么Java就认为中间的双引号是普通字符， 那么它就识别为 abc"edf
	//                                这里斜杠就是转义字符，它indicate它后面的字符是个普通字符，不是功能意义上的字符。
	public static String parseString(String str) {
		// replace " with \", then replace / with or
		return str.replace("\"", "\\\"").replace("/", " or ");
	}

	
	// Convert JSON array to String
	// e.g., [Korean, Chinese, Japanese] ---->  "Korean, Chinese, Japanese"
	// Note: （1） Java only recognizes string. It does not know JSON array.
	//       （2） SQL cannot store data in JSON format. 
	//        (3) 在string上做文本查找时，match character很容易
	public static String jsonArrayToString(JSONArray array) {
		// use a StringBuilder to do the conversion
		StringBuilder sb = new StringBuilder();
		try {
			for (int i = 0; i < array.length(); i++) {
				String obj = (String) array.get(i);
				sb.append(obj);
				if (i != array.length() - 1) {
					sb.append(",");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	// Convert String to JSON array
	// Note: 存在database里的data要转化成JSON array，前端才能理解。
	public static JSONArray stringToJSONArray(String str) {
		try {
			// 就是加一对钟括号
			return new JSONArray("[" + parseString(str) + "]");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	
	// Note: the following getter and setter functions for each field are generated 
	//       by Generate Getters and Setters in Source menu in Eclipse.
	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress;
	}

	public double getStars() {
		return stars;
	}

	public void setStars(double stars) {
		this.stars = stars;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
