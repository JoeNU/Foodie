package api;

import java.io.IOException;
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

// recommend restaurants based on user's favorite restaurants' categories

/**
 * Servlet implementation class RecommendRestaurants
 */
@WebServlet("/recommendation")
public class RecommendRestaurants extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static DBConnection connection = new MySQLDBConnection();
    //private static DBConnection connection = new MongoDBConnection(); // note: actually you can use a configuration file to change the implementation class in different java files in api package.
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RecommendRestaurants() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */


	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
		
		JSONArray array = null;
		//if (request.getParameterMap().containsKey("user_id")) {
			//String userId = request.getParameter("user_id");
		String userId = (String) session.getAttribute("user"); // get user id from session
		array = connection.recommendRestaurants(userId); // connect to recommendRestaurants method in MySQLDBConnection.java   note: 在api层次， 把代码给连接起来，
		//}

		RpcParser.writeOutput(response, array);
		
//		// Sample Code 1
//		JSONArray array = new JSONArray();
//		try {
//			if (request.getParameterMap().containsKey("user_id")) {
//				String userid = request.getParameter("user_id");
//				array.put(new JSONObject()
//						 .put("name", "panda express")
//                         .put("location", "downtown")
//                         .put("country", "united states"));
//				array.put(new JSONObject()
//						 .put("name", "HK express")
//                        .put("location", "uptown")
//                        .put("country", "united states"));
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		RpcParser.writeOutput(response, array);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
		
		doGet(request, response);
	}

}
