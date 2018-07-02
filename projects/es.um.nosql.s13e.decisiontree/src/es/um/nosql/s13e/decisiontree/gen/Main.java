package es.um.nosql.s13e.decisiontree.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import es.um.nosql.s13e.NoSQLSchema.Entity;
import es.um.nosql.s13e.NoSQLSchema.EntityVariation;
import es.um.nosql.s13e.NoSQLSchema.NoSQLSchemaPackage;
import es.um.nosql.s13e.DecisionTree.DecisionTreeForEntity;
import es.um.nosql.s13e.DecisionTree.DecisionTreeNode;
import es.um.nosql.s13e.DecisionTree.DecisionTrees;
import es.um.nosql.s13e.DecisionTree.DecisionTreeFactory;
import es.um.nosql.s13e.DecisionTree.DecisionTreePackage;
import es.um.nosql.s13e.DecisionTree.IntermediateNode;
import es.um.nosql.s13e.DecisionTree.LeafNode;
import es.um.nosql.s13e.DecisionTree.PropertySpec2;
import es.um.nosql.s13e.decisiontree.utils.ModelNode;
import es.um.nosql.s13e.decisiontree.utils.OpenJ48;
import es.um.nosql.s13e.EntityDifferentiation.EntityDiffSpec;
import es.um.nosql.s13e.EntityDifferentiation.EntityDifferentiation;
import es.um.nosql.s13e.EntityDifferentiation.EntityVariationProp;
import es.um.nosql.s13e.EntityDifferentiation.EntityDifferentiationPackage;
import es.um.nosql.s13e.EntityDifferentiation.PropertySpec;
import es.um.nosql.s13e.util.emf.ModelLoader;
import es.um.nosql.s13e.util.emf.Serializer;
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

	static Pattern pattern = Pattern.compile("\\[(.*?) .*$");
	
	private ModelNode getModelTree(ClassifierTree tree, Map<String, EntityVariation> entityVersions, Map<String, PropertySpec> properties) throws Exception
	{
		if (tree.isLeaf())
		{
			String tag = tree.prefix();
			Matcher matcher = pattern.matcher(tag);

			if (matcher.find())
			{
				EntityVariation ev = entityVersions.get(matcher.group(1));
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

	private void printModelTree(Entity e, ModelNode tree){
		printModelTree(e, tree, 0);
	}

	private void printModelTree(Entity e, ModelNode tree, int level)
	{
		String indent = String.join("", Collections.nCopies(level, "  "));

		if (tree.is_leaf())
			System.out.println(indent+"Entity: "+e.getName()+", Version: "+tree.getEv().getVariationId());
		else
		{
			Function<Boolean,String> present = v -> v ? " is present." : " is NOT present."; 
			
			System.out.println(indent+tree.getProperty().getProperty().getName()+ present.apply(!tree.isCheckNot()));
			printModelTree(e, tree.getNodePresent(), level+1);
			
			System.out.println(indent+tree.getProperty().getProperty().getName()+ present.apply(tree.isCheckNot()));
			printModelTree(e, tree.getNodeAbsent(), level+1);
		}
	}

	public static void main(String[] args)
    {
		(new Main()).run(args);
	}

	private void run(String[] args)
	{
		if (args.length != 2)
		{
			System.err.println("Usage: decisiontree infile outfile");
			System.exit(1);
		}
		
		ModelLoader loader = new ModelLoader(NoSQLSchemaPackage.eINSTANCE);
		loader.registerPackages(EntityDifferentiationPackage.eINSTANCE,DecisionTreePackage.eINSTANCE);
		EntityDifferentiation diff = loader.load(new File(args[0]),
				EntityDifferentiation.class);

		// Create a DecisionTree Model to be written
		DecisionTrees dt = DecisionTreeFactory.eINSTANCE.createDecisionTrees();
		dt.setName(diff.getName());
		
		diff.getEntityDiffSpecs().stream().filter(ed -> ed.getEntityVariationProps().size() > 1)
			.forEach(eds -> {
				ModelNode root = generateTreeForEntity(eds);
				
				DecisionTreeForEntity dte = 
					DecisionTreeFactory.eINSTANCE.createDecisionTreeForEntity();
				dte.setEntity(eds.getEntity());
				// fill dte
				dte.setRoot(decisionTreeForEntity(root));
				
				dt.getTrees().add(dte);
			});
		
		// Write model to disk.
		File outResource = new File(args[1]);
		Resource outputRes = loader.getResourceSet().createResource(URI.createFileURI(args[1]));
		outputRes.getContents().add(dt);
		try {
			OutputStream os = new FileOutputStream(outResource);
			outputRes.save(os, null/*, options*/);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private DecisionTreeNode decisionTreeForEntity(ModelNode root)
	{
		if (root.is_leaf())
		{
			LeafNode ln =  DecisionTreeFactory.eINSTANCE.createLeafNode();
			ln.setIdentifiedVariation(root.getEv());
			return ln;
		}
		else
		{
			IntermediateNode in = 
					DecisionTreeFactory.eINSTANCE.createIntermediateNode();

			// Exchange branches when the test is negative.
			if (root.isCheckNot())
			{
				in.setNoBranch(decisionTreeForEntity(root.getNodePresent()));
				in.setYesBranch(decisionTreeForEntity(root.getNodeAbsent()));
			} else
			{
				in.setYesBranch(decisionTreeForEntity(root.getNodePresent()));
				in.setNoBranch(decisionTreeForEntity(root.getNodeAbsent()));				
			}
			
			// Create PropertySpec2 from PropertySpec
			PropertySpec2 ps2 = DecisionTreeFactory.eINSTANCE.createPropertySpec2();
			ps2.setProperty(root.getProperty().getProperty());
			ps2.setNeedsTypeCheck(root.getProperty().isNeedsTypeCheck());
			
			in.setCheckedProperty(ps2);
			
			return in;
		}
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

	private ModelNode generateTreeForEntity(EntityDiffSpec eds)
	{
		Map<EntityVariationProp, List<Pair<String, PropertySpec>>> propsByEv =
			eds.getEntityVariationProps().stream()
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
		Map<String,EntityVariationProp> classNameToEvp = eds.getEntityVariationProps().stream()
				.map(evp -> Pair.of(String.format("%1$s_%2$d", entityName, evp.getEntityVariation().getVariationId()),evp))
				.collect(toMap(Pair::getKey,Pair::getValue));
		Map<EntityVariationProp,String> evpToClassName = classNameToEvp.entrySet().stream()
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
		
		Map<EntityVariationProp, Instance> featuresByEv =
			propsByEv.entrySet().stream()
			.collect(toMap(Map.Entry::getKey,
					e -> {
						Instance inst = new DenseInstance(1.0,defaultValues);
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

			System.out.println(tree);
			for (int i = 0; i < dataset.numInstances(); i++){
				System.out.println(dataset.get(i).stringValue(tag)+": "
						+ tree.classifyInstance(dataset.get(i)));
			}

			System.out.println(dataset);
			
			ModelNode modelTree =
					getModelTree(root,
							classNameToEvp.entrySet().stream()
								.collect(toMap(Map.Entry::getKey,
											   v -> v.getValue().getEntityVariation())),
							features);
			
			printModelTree(eds.getEntity(),modelTree);
			
			return modelTree;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Error
		return null;
	}

}