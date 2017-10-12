package dao.mongo;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class MongoConnection {
	private static final String HOST = "localhost";
	private static final int PORT = 27017;
	private static final String DATABASE = "ufam";

	private static MongoConnection uniqueInstance;

	private DB db;
	private MongoClient client;

	private MongoConnection() {
	}

	public static synchronized MongoConnection getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new MongoConnection();
		}
		return uniqueInstance;
	}

	public DB getDB() throws UnknownHostException {
		if (client == null) {
			try {
				client = new MongoClient(HOST, PORT);
				db = client.getDB(DATABASE);
			} catch (MongoException e) {
				e.printStackTrace();
			 }
		}
		return db;
	}
}
