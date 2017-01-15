/**
 *
 */
package es.um.nosql.schemainference.nosqlimport.couchdb;

import java.util.ArrayList;
import java.util.List;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.lightcouch.DesignDocument.MapReduce;

import com.google.gson.JsonObject;

import es.um.nosql.schemainference.nosqlimport.util.MapReduceSources;

/**
 * @author dsevilla
 *
 */
public class CouchDBSchemaInference
{
	public static void main(String[] args)
	{
/*		if (args.length < 2)
		{
			System.err.println("USAGE: inference dbName viewDir (a directory where the map.js and reduce.js files live.)");
			return;
		}
*/
		String dbName = "test";//args[0];
		String dirName = "mapreduce/couchdb/v1";//args[1];

		try {
			MapReduceSources mrs = MapReduceSources.fromDir(dirName);

			CouchDbProperties properties =
					new CouchDbProperties(dbName, true, "http", "155.54.190.237", 5984,
							null,null);
			CouchDbClient dbClient = new CouchDbClient(properties);

			//		List<JsonObject> allDocs = dbClient.view("_all_docs").query(JsonObject.class);
			//
			//		for (JsonObject o : allDocs)
			//			System.out.println(o.toString());

			MapReduce mapRedObj = new MapReduce();
			mapRedObj.setMap(mrs.getMapJSCode());
			mapRedObj.setReduce(mrs.getReduceJSCode());

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

		} catch (Exception e) {
			System.err.println("Cannot access map.js and/or reduce.js files.");
			e.printStackTrace();
		}
	}
}
