/**
 *
 */
package es.um.nosql.schemainference.nosqlimport.couchdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.lightcouch.DesignDocument.MapReduce;

import com.google.gson.JsonObject;

/**
 * @author dsevilla
 *
 */
public class CouchDBSchemaInference
{
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.err.println("USAGE: inference dbName viewDir (a directory where the map.js and reduce.js files live.)");
			return;
		}

		String dbName = args[0];
		String dirName = args[1];
		Path dir = new File(dirName).toPath();

		Path mapFile = dir.resolve("map.js");
		Path reduceFile = dir.resolve("reduce.js");

		try {
			String mapFunc = new String(Files.readAllBytes(mapFile));
			String reduceFunc = new String(Files.readAllBytes(reduceFile));

			CouchDbProperties properties =
					new CouchDbProperties(dbName, true, "http", "127.0.0.1", 5984,
							null,null);
			CouchDbClient dbClient = new CouchDbClient(properties);

			//		List<JsonObject> allDocs = dbClient.view("_all_docs").query(JsonObject.class);
			//
			//		for (JsonObject o : allDocs)
			//			System.out.println(o.toString());

			MapReduce mapRedObj = new MapReduce();
			mapRedObj.setMap(mapFunc);
			mapRedObj.setReduce(reduceFunc);

			List<JsonObject> list = dbClient.view("_temp_view")
					.tempView(mapRedObj)
					.group(true)
					.includeDocs(false)
					.reduce(true)
					.query(JsonObject.class);

			for (JsonObject o : list)
				System.out.println(o.toString());

			// Produce all the actual objects from the query. Couchdb won't allow include_docs to be specified
			// for a reduce view, and if I include the document itself it causes a view overflow. So we have
			// to take all the value objects and obtain them from the database directly
			List<JsonObject> result = new ArrayList<JsonObject>(list.size());
			for (JsonObject o : list)
			{
				String doc_id = o.get("value").getAsString();
				JsonObject obj = dbClient.find(JsonObject.class, doc_id);
				result.add(obj);
			}

		} catch (IOException e) {
			System.err.println("Cannot access map.js and/or reduce.js files.");
			e.printStackTrace();
		}
	}
}