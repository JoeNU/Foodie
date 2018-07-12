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

import java.io.PrintWriter;

/**
 * Servlet implementation class SearchRestaurants
 */
@WebServlet("/restaurants") // This is from url mapping.
public class SearchRestaurants extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SearchRestaurants() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	// Note: if you receive a request having GET method, then doGet() will be called.
	// HttpServletRequest request is the detailed request sent by client.
	// HttpServletResponse response is a pointer pointing to your response. It is open, 
	// so you can write/flush more than one time in response. 
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists   note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
		JSONArray array = null;
		DBConnection connection = new MySQLDBConnection();
//		if (request.getParameterMap().containsKey("user_id") && request.getParameterMap().containsKey("lat")
//				&& request.getParameterMap().containsKey("lon")) {
		if (request.getParameterMap().containsKey("lat") && request.getParameterMap().containsKey("lon")) {				
			//String userId = request.getParameter("user_id");
			String userId = (String) session.getAttribute("user"); // get user id from session
			double lat = Double.parseDouble(request.getParameter("lat"));
			double lon = Double.parseDouble(request.getParameter("lon"));
			// term is null or empty by default
			String term = request.getParameter("term");
			array = connection.searchRestaurants(userId, lat, lon, term);
		}
		RpcParser.writeOutput(response, array);
		

//      // note: previous temporary codes		
//		JSONArray array = new JSONArray();
//		DBConnection connection = new MySQLDBConnection();
//		//DBConnection connection = new MongoDBConnection();
//		if (request.getParameterMap().containsKey("lat") && request.getParameterMap().containsKey("lon")) {
//			// term is null or empty by default
//			String term = request.getParameter("term");
//			// String userId = (String) session.getAttribute("user");
//			String userId = "1111";
//			double lat = Double.parseDouble(request.getParameter("lat"));
//			double lon = Double.parseDouble(request.getParameter("lon"));
//			// pass all the above parameters into searchRestaurants()
//			array = connection.searchRestaurants(userId, lat, lon, term);
//		}
//		// this line of code presents data to front end
//		RpcParser.writeOutput(response, array); // write JSON array to response
		
		
		
//		/// Sample test Code 1: test if browser can receive response
//		response.setContentType("application/json"); // server will return a response in a format of JSON
//		response.addHeader("Access-Control-Allow-Origin", "*"); // 
//		String username = "";
//		PrintWriter out = response.getWriter();// PrintWriter is getWriter's IO buffer.
//		if (request.getParameter("username") != null) {
//			username = request.getParameter("username");
//			out.print("Hello " + username);// out.print() is essentially same as out.append()
//										   // Note: out.append() 一直往后面拼到一块儿； out.print()当时就解决了，当时就直接填进去了
//		}
//		out.flush(); // send data to the client side
//		out.close(); // close the response 
		
		
//		/// Sample test Code 2: write html codes to response
//		response.setContentType("text/html");
//		PrintWriter out = response.getWriter(); // PrintWriter helps you do the IO buffering.
//		out.println("<html><body>");// 往buffer里填内容
//		out.println("<h1>This is a HTML page</h1>");
//		out.println("</body></html>"); // 这几行codes都是先将内容写到本机memory里面
//		out.flush(); // 将memory里的东西推送到client端
//		out.close();
		
		
//		/// Sample test Code 3: send data by JSON
//		response.setContentType("application/json");
//		response.addHeader("Access-Control-Allow-Origin", "*");
//		String username = "";
//		String age = "";
//		if (request.getParameter("username") != null) {
//			username = request.getParameter("username");
//		}
//		if (request.getParameter("age") != null) {
//			age = request.getParameter("age");
//		}
//		JSONObject obj = new JSONObject();
//		try {
//			obj.put("username", username);
//			obj.put("age", age);
//		} catch (JSONException e) {// 预计可能出现的exception: 转成JSON的过程中出现问题
//			e.printStackTrace();
//		}
//		PrintWriter out = response.getWriter();
//		out.print(obj);
//		out.flush();
//		out.close();
		
//		/// Sample test Code 4: 
//		JSONArray array = new JSONArray();
//		try {
//			if (request.getParameterMap().containsKey("user_id")
//					&& request.getParameterMap().containsKey("lat")
//					&& request.getParameterMap().containsKey("lon")) {
//				String userId = request.getParameter("user_id");
//				double lat = Double.parseDouble(request.getParameter("lat"));
//				double lon = Double.parseDouble(request.getParameter("lon"));
//				// return some fake restaurants
//				array.put(new JSONObject().put("name", "Panda Express")); // create a JSON object and put it in JSON array
//				array.put(new JSONObject().put("name", "Hong Kong Express")); // create another JSON object and put it in JSON array
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		RpcParser.writeOutput(response, array); // write JSON array to response
	}
	
	// Note: getWriter() : Returns a PrintWriter object that can send character text to the client. 
	// 		 In this case, the servlet binded with the url-pattern (previously set) is called. 
	//       The method being called depends on the kind of request (doGet, doPost, doPut).
	
	
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	// Note: if you receive a request having POST method, then doPost() will be called.
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists    note: this is for security issues
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}

		doGet(request, response);
	}

}
