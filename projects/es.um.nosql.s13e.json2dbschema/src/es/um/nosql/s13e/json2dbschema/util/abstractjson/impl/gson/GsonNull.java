package es.um.nosql.s13e.json2dbschema.util.abstractjson.impl.gson;

import com.google.gson.JsonElement;
import es.um.nosql.s13e.json2dbschema.util.abstractjson.IAJNull;

/**
 * @author dsevilla
 *
 */
public class GsonNull extends GsonElement implements IAJNull
{
	public GsonNull(JsonElement val) {
		super(val);
	}
}
