package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.MongoDBConnection;
import db.MySQLDBConnection;

/**
 * Servlet implementation class VisitHistory
 * Note: 这个servlet可以处理三种query: post, delete, get, 而后端对应的处理是要在data base里进行操作，
 *       这里通过DBConnection来连接前后端，具体的实现是MySQLDBconnection, 所以我们连接的是MySQL data base.
 *       具体doPost(), doDelete(), doGet()得以对数据库里的内容(即history table)进行操作，是分别通过connection的setVisitedRestaurants(),
 *       unsetVisitedRestaurants(), 和getVisitedRestaurants()去实现的，而这些connection的函数的具体实现，则是通过
 *       SQL queries. 此外，这个doPost(), doDelete(), doGet()都是protected, 因为这不是我们自己调用的, 只有Tomcat Apache library才会调用这部分代码，若改成
 *       public, 那么其它package里的代码也可以调用这三个method, 那么就不对了。
 */
@WebServlet("/history")
public class VisitHistory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final DBConnection connection = new MySQLDBConnection(); // Initialize MySQL connection

	//private static final DBConnection connection = new MongoDBConnection();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public VisitHistory() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */


	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
		
		try {
			JSONObject input = RpcParser.parseInput(request);// convert request to JSON object using the static method parseInput in utility class RpcParser 
			// check if the input object has parameter user_id and parameter visited
			if (input.has("user_id") && input.has("visited")) {
				String userId = (String) input.get("user_id");
				JSONArray array = (JSONArray) input.get("visited"); // .get()默认返回的是普通的Java的object, 而普通的Java object没有各种methods, 所以要强制转换成JSONArray。
				List<String> visitedRestaurants = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);
					visitedRestaurants.add(businessId);
				}
				
				// store the visit history in the history table in MySQL/MongoDB database.
				if (connection.setVisitedRestaurants(userId, visitedRestaurants)) { // Note: 通过这行代码就把doPost()和 MySQL/MongoDB database连系起来了
					RpcParser.writeOutput(response, new JSONObject().put("status", "OK")); // 告诉前端在数据库中添加数据成功
				} else {
					RpcParser.writeOutput(response, new JSONObject().put("status", "Error")); // 告诉前端在数据库中添加数据失败
				}
			} else {
				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
		
		try {
			// allow access only if session exists
			/*
			 * if (!RpcParser.sessionValid(request, connection)) {
			 * response.setStatus(403); return; }
			 */
			JSONObject input = RpcParser.parseInput(request);
			if (input.has("user_id") && input.has("visited")) {
				String userId = (String) input.get("user_id");
				JSONArray array = (JSONArray) input.get("visited");
				List<String> visitedRestaurants = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);
					visitedRestaurants.add(businessId);
				}
				// Note: 通过这行代码就把doDelete()和 MySQL database连系起来了
				connection.unsetVisitedRestaurants(userId, visitedRestaurants); // delete the visit history in the history table in MySQL database
				RpcParser.writeOutput(response, new JSONObject().put("status", "OK"));
			} else {
				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	
	// the front end wants to get the visit history
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues. We haven't checked user id.
		HttpSession session = request.getSession();
//		System.out.println(session.getAttribute("user")); // user id corresponding to current session; it's the user id of who has logged in
//		System.out.println(request.getParameter("user_id")); // user id in the incoming http request
		// note: session.getAttribute("user") returns user id
		if (session.getAttribute("user") == null) { // note: we need to use session.getAttribute("user").equals(userId) to compare String objects
			response.setStatus(403);
			return;
		}
		
//		 for (Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements();)
//		       System.out.println(e.nextElement());
//		 
//		 System.out.println(session.getId()); // session id
		
//		try {
			DBConnection connection = new MySQLDBConnection();
			JSONArray array = null;
			// allow access only if session exists
			/*
			 * if (!RpcParser.sessionValid(request, connection)) {
			 * response.setStatus(403); return; }
			 */
			
//			if (request.getParameterMap().containsKey("user_id")) {
//				String userId = request.getParameter("user_id"); // get user id from url
				String userId = (String) session.getAttribute("user"); // get user id from session
				Set<String> visited_business_id = connection.getVisitedRestaurants(userId);  // get the visit history in the history table in MySQL database
				array = new JSONArray();
				for (String id : visited_business_id) {
					array.put(connection.getRestaurantsById(id, true));
				}
				RpcParser.writeOutput(response, array);
//			} else {
//				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
	}

}
