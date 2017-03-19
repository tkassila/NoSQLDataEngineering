package es.um.nosql.schemainference.decisiontree.gen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import es.um.nosql.schemainference.NoSQLSchema.Entity;
import es.um.nosql.schemainference.NoSQLSchema.EntityVersion;
import es.um.nosql.schemainference.NoSQLSchema.NoSQLSchemaPackage;
import es.um.nosql.schemainference.decisiontree.utils.ModelNode;
import es.um.nosql.schemainference.decisiontree.utils.OpenJ48;
import es.um.nosql.schemainference.entitydifferentiation.EntityDiffSpec;
import es.um.nosql.schemainference.entitydifferentiation.EntityDifferentiation;
import es.um.nosql.schemainference.entitydifferentiation.EntityVersionProp;
import es.um.nosql.schemainference.entitydifferentiation.EntitydifferentiationPackage;
import es.um.nosql.schemainference.entitydifferentiation.PropertySpec;
import es.um.nosql.schemainference.util.emf.ModelLoader;
import es.um.nosql.schemainference.util.emf.Serializer;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class Main 
{
	private OpenJ48 generateTree(Instances train) throws Exception
    {
		OpenJ48 classifier = new OpenJ48();
		classifier.setUnpruned(true);
		classifier.setMinNumObj(1);
		classifier.buildClassifier(train);
		return classifier;
    }

	static Pattern pattern = Pattern.compile("\\[(.*?) \\([\\d\\.\\,]+\\)\\]");
	
	private ModelNode getModelTree(ClassifierTree tree, Map<String, EntityVersion> entityVersions, Map<String, PropertySpec> properties) throws Exception
	{
		if (tree.isLeaf())
		{
			String tag = tree.prefix();
			Matcher matcher = pattern.matcher(tag);

			if (matcher.find())
			{
				EntityVersion ev = entityVersions.get(matcher.group(1));
				return new ModelNode(ev);
			}
			else
				throw new Exception("Invalid exp reg for: "+tag);
		}
		else
		{
			//ClassifierSplitModel classifierSplitModel = tree.getLocalModel();
			ClassifierTree[] sons = tree.getSons();
			String left = tree.getLocalModel().leftSide(tree.getTrainingData()).trim();
			
			PropertySpec p = properties.get(left);

			if (p==null) throw new Exception("Unknown Property Name: " + left);

			if (sons.length != 2) throw new Exception("This is not a binary decision tree");

			ModelNode m = new ModelNode(p, left.startsWith("!"));
			for (int i = 0 ; i < sons.length; i++)
			{
				String value = tree.getLocalModel().rightSide(i, tree.getTrainingData()).trim();
				ModelNode node = getModelTree(sons[i], entityVersions, properties);
				if (value.equals("= yes"))
					m.setNodePresent(node);
				else if (value.contentEquals("= no"))
					m.setNodeAbsent(node);
				else
					throw new Exception("Unknown Right side: " + value.trim());
			}

			return m;
		}
	}

	private void runModelTree(Entity e, ModelNode tree){
		runModelTree(e, tree, 0);
	}

	private void runModelTree(Entity e, ModelNode tree, int level)
	{
		String indent = String.join("", Collections.nCopies(level, "  "));

		if (tree.is_leaf())
		{
			System.out.println(indent+"Entity: "+e.getName()+", Version: "+tree.getTag().getVersionId());
		}
		else
		{
			Function<Boolean,String> present = (v) -> v ? " is present." : " is NOT present."; 
			
			System.out.println(indent+tree.getProperty().getProperty().getName()+ present.apply(!tree.isCheckNot()));
			runModelTree(e, tree.getNodePresent(), level+1);
			
			System.out.println(indent+tree.getProperty().getProperty().getName()+ present.apply(tree.isCheckNot()));
			runModelTree(e, tree.getNodeAbsent(), level+1);
		}
	}

	public static void main(String[] args)
    {
		(new Main()).run(args);
	}

	private void run(String[] args)
	{
		ModelLoader loader = new ModelLoader(NoSQLSchemaPackage.eINSTANCE);
		loader.registerPackages(EntitydifferentiationPackage.eINSTANCE);
		EntityDifferentiation diff = loader.load(new File("model/mongoMovies3_Diff.xmi"),
				EntityDifferentiation.class);

		diff.getEntityDiffSpecs().stream().filter(ed -> ed.getEntityVersionProps().size() > 1)
			.forEach(eds -> 
				generateTreeForEntity(eds));
	}

	private String serialize(PropertySpec ps)
	{
		if (ps.isNeedsTypeCheck())
			return Serializer.serialize(ps.getProperty());
		else
			return ps.getProperty().getName();
	}

	private String serializeNot(PropertySpec ps)
	{
		return "!" + ps.getProperty().getName();
	}

	private void generateTreeForEntity(EntityDiffSpec eds)
	{
		Map<EntityVersionProp, List<Pair<String, PropertySpec>>> propsByEv =
			eds.getEntityVersionProps().stream()
			.collect(toMap(Function.identity(),
					evp ->
						Stream.concat(
								evp.getPropertySpecs().stream().map(ps -> Pair.of(serialize(ps), ps)),
								evp.getNotProps().stream().map(ps -> Pair.of(serializeNot(ps), ps)))
							.collect(toList()),
					(u,v) -> u,
					LinkedHashMap::new));
		
		// serializedFeature -> PropertySpec. We do the final reducing to leave just one element on 
		// the list, as there may be features in different entity version that serialize to the same
		// string. We just select one.
		Map<String, PropertySpec> features =
			propsByEv.values().stream().flatMap(l -> l.stream())
				.collect(toMap(Pair::getKey,
							   Pair::getValue,
							   (u,v) -> u,
							   LinkedHashMap::new));

		final List<String> f_values = Arrays.asList(new String[]{"yes","no"});

		// Attributes associated to each feature (propertyspec serialization)
		Map<String, Attribute> attrMap =
			features.keySet().stream()
				.collect(toMap(Function.identity(),
						 s -> new Attribute(s, f_values),
						 (u,v) -> u,
						 LinkedHashMap::new));
		
//		// Generate inverted index for feature serialization to feature vector position
//		final Iterator<Map.Entry<String,PropertySpec>> it = features.entrySet().iterator();
//		Map<String,Integer> arrayPos =
//			IntStream.range(0,features.size())
//				.boxed()
//				.collect(toMap(e -> it.next().getKey(),
//						Function.identity()));
		
		final String entityName = eds.getEntity().getName();
		Map<String,EntityVersionProp> classNameToEvp = eds.getEntityVersionProps().stream()
				.map(evp -> Pair.of(String.format("%1$s_%2$d", entityName, evp.getEntityVersion().getVersionId()),evp))
				.collect(toMap(Pair::getKey,Pair::getValue));
		Map<EntityVersionProp,String> evpToClassName = classNameToEvp.entrySet().stream()
				.collect(toMap(Map.Entry::getValue,
						Map.Entry::getKey));

		// Get classes and count them. We do it in the order that they are obtained
		// from the original list of EntityVersionProps
		List<String> classes = propsByEv.keySet().stream()
				.map(evp -> evpToClassName.get(evp))
				.collect(toCollection(ArrayList::new));
		int num_classes = classes.size();
				
		// Define Weka Instances Model
		// Build Attribute models for weka
		ArrayList<Attribute> atts = new ArrayList<>(attrMap.values());
		Attribute tag = new Attribute("tag", classes);
		atts.add(tag);
		
		// Generate Dataset
		Instances dataset = new Instances("Train", atts, num_classes);
		
		final double[] defaultValues = new double[features.size()+1];
		Arrays.fill(defaultValues,1.0);
		
		Map<EntityVersionProp, Instance> featuresByEv =
			propsByEv.entrySet().stream()
			.collect(toMap(Map.Entry::getKey,
					e -> {
						Instance _inst = new DenseInstance(features.size()+ 1);
						Instance inst = _inst.copy(defaultValues);
						e.getValue().forEach(p -> inst.setValue(attrMap.get(p.getKey()),"yes"));
						inst.setValue(tag, evpToClassName.get(e.getKey()));
						return inst;
					}));
		
		dataset.addAll(featuresByEv.values());
		dataset.setClass(tag);
		
		try {
			// Get Classification Tree
			OpenJ48 tree = generateTree(dataset);
			ClassifierTree root = tree.get_m_root();

			System.out.println(tree.graph());
			for (int i = 0; i < dataset.numInstances(); i++){
				System.out.println(dataset.get(i).stringValue(tag)+": "
						+ tree.classifyInstance(dataset.get(i)));
			}

			ModelNode modelTree =
					getModelTree(root,
							classNameToEvp.entrySet().stream()
								.collect(toMap(Map.Entry::getKey,
											   v -> v.getValue().getEntityVersion())),
							features);
			runModelTree(eds.getEntity(),modelTree);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}