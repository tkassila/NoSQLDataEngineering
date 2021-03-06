package es.um.nosql.streaminginference.json2dbschema.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.ArraySC;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.BooleanSC;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.NullSC;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.NumberSC;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.ObjectSC;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.SchemaComponent;
import es.um.nosql.streaminginference.json2dbschema.intermediate.raw.StringSC;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJArray;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJBoolean;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJElement;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJNull;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJNumber;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJObject;
import es.um.nosql.streaminginference.json2dbschema.util.abstractjson.IAJTextual;
import es.um.nosql.streaminginference.json2dbschema.util.inflector.Inflector;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author dsevilla
 *
 */
public class SchemaInference implements Serializable
{
	private static final long serialVersionUID = 2885206624442781144L;

	private Map<String, List<SchemaComponent>> rawEntities;
	
	// These properties correspond to an intermediate state
	// thus we will not serialize them
	private transient IAJArray theArray;
	private Set<String> innerSchemaNames;

	private static final boolean ROOT_OBJECT = true;
	private static final boolean NON_ROOT_OBJECT = false;

	public SchemaInference() 
	{
		rawEntities = new HashMap<String, List<SchemaComponent>>();
		innerSchemaNames = new HashSet<String>();
	}
	
	public SchemaInference(IAJArray rows)
	{
		this();
		this.theArray = rows;
	}
	
	public SchemaInference(SchemaInference copy) 
	{
		rawEntities = new HashMap<String, List<SchemaComponent>>();
		copy.rawEntities.forEach((entity, versions) -> {
			List<SchemaComponent> myVersions = new ArrayList<SchemaComponent>(versions);
			rawEntities.put(entity, myVersions);
		});
		
		innerSchemaNames = new HashSet<String>(copy.innerSchemaNames);
	}
	
	public Map<String, List<SchemaComponent>> getEntities() 
	{
		return rawEntities;
	}
	
	public Set<String> getInnerSchemaNames() 
	{
		return innerSchemaNames;
	}
	
	public boolean equals(SchemaInference other) 
	{
		if (this == other)
			return true;
		
		if (!this.rawEntities.keySet().equals(other.getEntities().keySet()))
			return false;
		
		for(Map.Entry<String, List<SchemaComponent>> entry : other.getEntities().entrySet()) 
		{
			if(rawEntities
				.get(entry.getKey()).stream().anyMatch(version -> 
					entry
						.getValue()
						.stream()
						.noneMatch(otherVersion -> version.equals(otherVersion))
				)) {
				return false;
			}
		}
		
		return true;
		
	}

	/**
	 * Gets a new SchemaInference object containing entities and versions from this and other
	 * @param other SchemaInference object to merge
	 * @return SchemaInference merged object
	 */
	public SchemaInference merge(SchemaInference cp) 
	{
		// IMPORTANT!! cp object will be modified so we need to work on a deep copy
		// to avoid future problems
		SchemaInference other = (SchemaInference) org.apache.commons.lang3.SerializationUtils.clone(cp);
		SchemaInference merged = (SchemaInference) org.apache.commons.lang3.SerializationUtils.clone(this);
		Map<String, List<SchemaComponent>> mergedEntities = merged.getEntities();
		Map<String, List<SchemaComponent>> otherEntities = other.getEntities();
		Map<String, List<SchemaComponent>> toAppend = new HashMap<String, List<SchemaComponent>>();
		List<Pair<SchemaComponent,SchemaComponent>> versionsToReplace = new ArrayList<Pair<SchemaComponent,SchemaComponent>>(10);
		otherEntities.forEach((entity, versions) -> 
		{
			if (!merged.rawEntities.containsKey(entity)) 
				toAppend.put(entity, versions);
			else 
			{
				versions.forEach(version -> 
				{
					// Check the existance of an equal version
					Optional<SchemaComponent> foundVersion = merged
															.rawEntities
															.get(entity)
															.stream()
															.filter(myVersion -> myVersion == version || myVersion.equals(version))
															.findAny();
					
					if (foundVersion.isPresent())
						versionsToReplace.add(Pair.of(version, foundVersion.get()));
					else
					{
						if (toAppend.get(entity) == null)
							toAppend.put(entity, new ArrayList<SchemaComponent>());
						toAppend.get(entity).add(version);
					}
				});
			}
		});

		merged.innerSchemaNames.addAll(other.innerSchemaNames);
		toAppend.forEach((entity, versions) -> {
			if (mergedEntities.get(entity) == null)
				mergedEntities.put(entity, new ArrayList<SchemaComponent>());
			mergedEntities.get(entity).addAll(versions);
		});
		
		merged.joinAggregatedEntities();
		// Update references of merged entity versions
		versionsToReplace.forEach(pair -> 
		{
			toAppend.forEach((entity, versions) -> {
				versions.forEach(version -> {
					merged.updateReferences(pair.getLeft(), pair.getRight(), version);
				});
			});
		});
				
		return merged;
	}

	public Map<String, List<SchemaComponent>> infer()
	{
		theArray.forEach(n -> infer(n, Optional.<String>empty(), ROOT_OBJECT));

		joinAggregatedEntities();
		
		mergeEquivalentEVs();

		// Print entities and entity versions
//		rawEntities.forEach((en, evl) ->
//		{
//			System.out.println("Entity: " + en);
//
//			evl.forEach(ev ->
//			{
//				System.out.print("* ");
//				System.out.println(SchemaPrinter.schemaString(ev));
//			});
//		});

		return rawEntities;
	}

	static List<String> AggregateHintWords = 
		Arrays.asList(new String[]{
				"has","with", "set", "list",
				"setof", "listof", "array", "arrayof",
				"collection","collectionof"});
	
	private void joinAggregatedEntities() 
	{
		Set<String> joinedEntities = new HashSet<String>();
		
		innerSchemaNames.forEach(name -> {
			rawEntities.keySet().stream()
				.filter(e -> 
					AggregateHintWords.stream().anyMatch(w ->
			    		(w + e).equalsIgnoreCase(name)
			    		|| (e + w).equalsIgnoreCase(name)))
				.findFirst()
				.ifPresent(v -> {
					// Change the name of the Entity Name for the
					// new entities that are in turn old entities with
					// slightly different name
					rawEntities.get(name).forEach(sc ->
						((ObjectSC)sc).entityName = v);
						
					// And all them at the end of the found entity
					rawEntities.get(v).addAll(rawEntities.get(name));
					rawEntities.remove(name);
					// Add joined entity name
					joinedEntities.add(name);
				});
			});
		
		// Remove previously joined entities from schema names
		innerSchemaNames.removeAll(joinedEntities);
	}

	private void mergeEquivalentEVs()
	{
		rawEntities.values().stream().forEach(leSC -> {
			boolean listModified;

			// While a modification in the list is requested, continue looking for equivalences
			do {
				listModified = false;

				// Repeat through all the elements of the list if the list is not modified
				Iterator<SchemaComponent> it = leSC.iterator();
				do {
					final SchemaComponent toConsider = it.next();
					for (SchemaComponent sc : leSC)
					{
						if (sc != toConsider && mergeEquivalentEVs(toConsider, sc))
						{
							// Update references to the old SchemaComponent
							updateReferences(toConsider, sc);
							
							// remove toConsider
							it.remove();

							// Start from the beginning, as the list has been modified
							listModified = true;
							break;
						}
					}
				} while (!listModified && it.hasNext());
			} while (listModified);
		});
	}

	private void updateReferences(SchemaComponent old, SchemaComponent neew)
	{
		rawEntities.forEach((n,l) ->
			l.forEach(sc -> updateReferences(old,neew, sc)));
	}

	private void updateReferences(SchemaComponent old, SchemaComponent neew, SchemaComponent sc) 
	{
		if (sc instanceof ObjectSC)
			updateReferences(old, neew, (ObjectSC)sc);

		if (sc instanceof ArraySC)
			updateReferences(old, neew, (ArraySC)sc);
	}

	private void updateReferences(SchemaComponent old, SchemaComponent neew, ObjectSC sc) 
	{
		sc.getInners().forEach(p -> {
			if (p.getValue() == old)
				p.setValue(neew);
			else
				updateReferences(old,neew,p.getValue());
		});
	}

	private void updateReferences(SchemaComponent old, SchemaComponent neew, ArraySC sc) 
	{
		sc.getInners().replaceAll(_sc -> _sc == old ? neew : _sc);
		sc.getInners().forEach(_sc -> {
			if (_sc != neew) // Already changed?
				updateReferences(old, neew, _sc);
		});
	}
	
	// Check & merge both schema components into the first.
	private boolean mergeEquivalentEVs(SchemaComponent toConsider, SchemaComponent sc)
	{
		return walkAndMerge(null, toConsider, sc);
	}

	private boolean walkAndMerge(String id, SchemaComponent toConsider, SchemaComponent sc)
	{
		// Are both equal classes?
		if (!toConsider.getClass().equals(sc.getClass()))
			return false;

		if (toConsider instanceof ObjectSC)
			return walkAndMerge(id, (ObjectSC)toConsider, (ObjectSC)sc);

		if (toConsider instanceof ArraySC)
			return walkAndMerge(id, (ArraySC)toConsider, (ArraySC)sc);

		return toConsider.equals(sc);
	}

	private boolean walkAndMerge(String id, ObjectSC toConsider, ObjectSC sc)
	{
		if (toConsider.size() != sc.size())
			return false;

		Iterator<Pair<String, SchemaComponent>> toConsiderIt = toConsider.getInners().iterator();
		Iterator<Pair<String, SchemaComponent>> scIt = sc.getInners().iterator();

		while (toConsiderIt.hasNext())
		{
			Pair<String, SchemaComponent> toCP = toConsiderIt.next();
			Pair<String, SchemaComponent> scP = scIt.next();

			if (!toCP.getLeft().equals(scP.getLeft()) ||
					!walkAndMerge(toCP.getLeft(), toCP.getRight(), scP.getRight()))
				return false;
		}

		return true;
	}

	private boolean walkAndMerge(String id, ArraySC toConsider, ArraySC sc)
	{
		if (toConsider.isHomogeneous() != sc.isHomogeneous())
			return false;

		// Special case. Homogeneous arrays?
		if (toConsider.isHomogeneous() && sc.isHomogeneous())
			return homogeneousArraysMerge(id, toConsider, sc);
		else
		{
			// Normal case. Similar to ObjectSC, but without element names
			if (toConsider.size() != sc.size())
				return false;

			Iterator<SchemaComponent> toConsiderIt = toConsider.getInners().iterator();
			Iterator<SchemaComponent> scIt = sc.getInners().iterator();

			while (toConsiderIt.hasNext())
			{
				if (!walkAndMerge(id, toConsiderIt.next(), scIt.next()))
					return false;
			}

			return true;
		}
	}

	private boolean homogeneousArraysMerge(String id, ArraySC toConsider, ArraySC sc)
	{
		// Homogeneous arrays have either zero or one element
		// Both of them can be empty
		if (toConsider.size() == 0 && sc.size() == 0)
			return true;
		else if (toConsider.size() == 0 || sc.size() == 0
				|| toConsider.getInners().get(0).equals(sc.getInners().get(0)))
		{
			int lowerBounds = Math.min(toConsider.getLowerBounds(), sc.getLowerBounds());

			// If this is the empty array, then it won't be empty
			if (sc.size() == 0)
				sc.add(toConsider.getInners().get(0));

			// Finally merge!
			sc.setLowerBounds(lowerBounds);
			int upperBounds = Math.max(toConsider.getUpperBounds(), sc.getUpperBounds());
			sc.setUpperBounds(upperBounds);

			return true;
		}

		return false;
	}

	private SchemaComponent infer(IAJElement n, Optional<String> elementName, boolean isRoot)
	{
		if (n.isObject())
			return infer(n.asObject(), elementName, isRoot);

		if (n.isArray())
			return infer(n.asArray(), elementName.get());

		if (n.isBoolean())
			return infer(n.asBoolean(), elementName.get());

		if(n.isNumber())
			return infer(n.asNumber(), elementName.get());

		if (n.isNull())
			return infer(n.asNull(), elementName.get());

		if (n.isTextual())
			return infer(n.asTextual(), elementName.get());

		assert(false);

		return null;
	}

	private SchemaComponent infer(IAJObject n, Optional<String> elementName, boolean isRoot)
	{
		// Entity names are by convention capitalized
		Optional<String> typeName = Optional.empty();
		if (isRoot)
			typeName = Optional.ofNullable(n.get("_type"))
							.map(_n -> Inflector.getInstance().capitalize(_n.asString()));

		ObjectSC schema = new ObjectSC();
		schema.isRoot = isRoot;

		schema.entityName = typeName.orElse(
				Inflector.getInstance().capitalize(elementName.orElse("")));

		// It is important this is a sorted set
		SortedSet<String> fields = new TreeSet<String>();
		n.getFieldNames().forEachRemaining(fields::add);

		// Tuple has to be mutable in order to update references
		for(String f : fields)
			schema.add(new MutablePair<String, SchemaComponent>(f, infer(n.get(f), Optional.of(f), NON_ROOT_OBJECT)));

		// Now that we have the complete schema, try to compare it with any of the versions in the map
		List<SchemaComponent> entityVersions = rawEntities.get(schema.entityName);
		SchemaComponent retSchema = schema;

		if (entityVersions != null)
		{
			Optional<SchemaComponent> foundSchema =
					entityVersions.stream().filter(sc -> schema.equals(sc)).findFirst();
			if (foundSchema.isPresent())
				retSchema = foundSchema.get();
			else
				entityVersions.add(schema);
		}
		else
		{
			List<SchemaComponent> ll = new ArrayList<SchemaComponent>(10);
			ll.add(schema);
			rawEntities.put(schema.entityName, ll);
			
			// Add the name of this entity to the list of afterward checking for already existing entities 
			if (!isRoot)
				innerSchemaNames.add(schema.entityName);
		}

		return retSchema;
	}

	private SchemaComponent infer(IAJArray n, String elementName)
	{
		ArraySC schema = new ArraySC();

		// TODO: At this point we should use the ReferenceMatcher to test verbs such as has or Id.
		// If the name for this array can be made singular, do it.
		String singularName = Inflector.getInstance().singularize(elementName);

		final Optional<String> name = Optional.of(singularName);

		n.forEach(e -> schema.add(infer(e, name, NON_ROOT_OBJECT)));

		return schema;
	}

	private SchemaComponent infer(IAJBoolean n, String elementName)
	{
		BooleanSC schema = new BooleanSC();
		return schema;
	}

	private SchemaComponent infer(IAJNumber n, String elementName)
	{
		NumberSC schema = new NumberSC();
		return schema;
	}

	private SchemaComponent infer(IAJNull n, String elementName)
	{
		NullSC schema = new NullSC();
		return schema;
	}

	private SchemaComponent infer(IAJTextual n, String elementName) {
		StringSC schema = new StringSC();
		return schema;
	}
}
