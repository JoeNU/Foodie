package yelp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Request;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


public class YelpFusion {
	// parameters
	private static final String API_HOST = "api.yelp.com";
	private static final String DEFAULT_TERM = "dinner";
	private static final int SEARCH_LIMIT = 20;
	private static final String SEARCH_PATH = "/v3/businesses/search";
	
	// passwords
	private static final String API_KEY = "PQWLR-FU6f864LePPS0Kl73z1zXWIjIJtTnA4O6-gUCglha3iHOMbZgIa1OMjZH1uIRSEssA3kDvZuT1ERI5Vi5RyuHmceP5Vl___SxmuN1s-zXbOrSH1T68yF5VW3Yx";
		
 
	/**
	 * Setup the Yelp API OAuth credentials.
	 * So then you are allowed to use Yelp API.
	 */
	public YelpFusion() {

	}
	
	/**
	 * Sends an {@link OAuthRequest} and returns the {@link Response} body.
	 */
	private String sendRequestAndGetResponse(HttpGet request) {
		System.out.println("Querying " + request + " ...");
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;
		try {
			httpclient = HttpClientBuilder.create().build();  // the http-client, that will send the request
			response = httpclient.execute(request); // the client executes the request and gets a response
            int responseCode = response.getStatusLine().getStatusCode();  // check the response code
            if (responseCode == 200) {
            	// everything is ok, and return the response
            	String res = EntityUtils.toString(response.getEntity());  // now you have the response as String, which you can convert to a JSONObject or do other stuff
            	httpclient.close(); // Don't forget to close connection!!!
            	response.close();
            	return res;
            }
        } catch (IOException e) {
            // handle exception
        	System.out.println("Exception!");
        } 
		return null;
	}
	
 
	/**
	 * Creates and sends a request to the Search API by term and location.
	 */
	public String searchForBusinessesByLocation(double lat, double lon) {	
		try {
			URIBuilder builder = new URIBuilder("http://" + API_HOST + SEARCH_PATH);
			builder.setParameter("term", DEFAULT_TERM)
			.setParameter("latitude", String.valueOf(lat))
			.setParameter("longitude", String.valueOf(lon))
			.setParameter("limit", String.valueOf(SEARCH_LIMIT));		
			HttpGet request = new HttpGet(builder.build()); // create an authenticated http request	
			request.addHeader("Authorization", "Bearer " + API_KEY);
			System.out.println(request);
			return sendRequestAndGetResponse(request);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} 
		return null;
	}
 

 
	/**
	 * Queries the Search API based on the command line arguments and takes the
	 * first result to query the Business API.
	 */
	private static void queryAPI(YelpFusion yelpFusion, double lat, double lon) {
		String searchResponseJSON = yelpFusion.searchForBusinessesByLocation(lat,
				lon);
		JSONObject response = null;
		try {
			response = new JSONObject(searchResponseJSON);
			JSONArray businesses = (JSONArray) response.get("businesses");
			for (int i = 0; i < businesses.length(); i++) {
				JSONObject business = (JSONObject) businesses.get(i);
				System.out.println(business);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	/**
	 * Main entry for sample Yelp API requests.
	 */
	public static void main(String[] args) {
		YelpFusion yelpFusion = new YelpFusion();
		queryAPI(yelpFusion, 37.38, -122.08);
		//yelpFusion.sendRequestAndGetResponse();
		//System.out.println(yelpFusion.searchForBusinessesByLocation(37.38, -122.08));
		
	}
}
