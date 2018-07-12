package api;

import java.io.BufferedReader;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A utility class（工具类） to handle rpc related parsing logic.
 */


// RpcParser has three methods. All are static.

public class RpcParser {
	// parse request: convert request to JSON object
	public static JSONObject parseInput(HttpServletRequest request) {
		StringBuffer jb = new StringBuffer(); // 
		String line = null;
		try {
			BufferedReader reader = request.getReader(); // reader points to the body part in http request. 就是POST传来的具体的body那部分。
			// 这里和java IO操作一摸一样
			while ((line = reader.readLine()) != null) {
				jb.append(line);
			}
			reader.close();
			return new JSONObject(jb.toString()); // convert to JSON object
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// write JSON object to response
	// Note: we handle errors by try-catch block here in this method. 
	public static void writeOutput(HttpServletResponse response, JSONObject obj) {
		try {
			response.setContentType("application/json");
			response.addHeader("Access-Control-Allow-Origin", "*"); // allow any one to view the page
			PrintWriter out = response.getWriter();
			out.print(obj);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// write JSON array to response
	// Note: we handle errors by try-catch block here in this method. 
	public static void writeOutput(HttpServletResponse response, JSONArray array) {
		try {
			response.setContentType("application/json");
			response.addHeader("Access-Control-Allow-Origin", "*"); // allow any one to view the page
			PrintWriter out = response.getWriter();
			out.print(array);
			out.flush();// send data from server to client
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
