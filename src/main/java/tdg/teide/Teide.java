package tdg.teide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.collect.Sets;

import tdf.pathfinder.model.Path;
import tdg.link_discovery.connector.sparql.evaluator.arq.linker.factory.SPARQLFactory;
import tdg.link_discovery.framework.algorithm.individual.ISpecification;
import tdg.link_discovery.middleware.framework.configuration.FrameworkConfiguration;
import tdg.link_discovery.middleware.objects.Tuple;
import tdg.pathfinder.PathFinder;
import tdg.pathfinder.parameters.PathFinderParameters;
import tdg.teide.parameters.TeideParameters;
import tdg.teide.translator.TranslatorStringRuleToSparql;

public class Teide  {

	private TeideParameters parameters;
	private PathFinderParameters  searchParameters;
	private LinkerK linker;
	
	
	public Teide(TeideParameters parameters, PathFinderParameters  searchParameters) {
		if(parameters == null)
			throw new IllegalArgumentException("TeideParameters passed as argument cannot be null");
		
		this.parameters = parameters;
		this.searchParameters = searchParameters;
		
		// initialize the linker
		linker = new LinkerK();
		linker.setDatasetSource(this.parameters.getSourceDataset());
		linker.setDatasetTarget(this.parameters.getTargetDataset());
	}
	
	
	
	@SuppressWarnings("unchecked")
	public Set<Tuple<String, String>> execute() {
		Set<Path> sourcePaths = null;
		Set<Path> targetPaths = null;
		Set<Tuple<String,String>> filteredLinks = null;
		
		// Apply main rule
		filteredLinks = Sets.newHashSet();
		FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "Applying main link rule");
		applyMainLinkRule();
		FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "Links generated: "+this.linker.getInstancesLinked().size());
		FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "Pruning links relying on supporting rules");
		// Prune previous links relying on the supporting rules
		for(ISpecification<String> supporting : parameters.getSupportingRules()) {
			FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\tRule: "+supporting.toString());
			FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\tSource classes: "+supporting.getSourceRestrictions());
			FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\tTarget classes: "+supporting.getTargetRestrictions());
			// Retrieve paths that connect mainRule with supporting
			FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\t\tRetrieving source paths");
			sourcePaths = retrieveConnectingPaths(parameters.getSourceDataset(), parameters.getMainRule().getSourceRestrictions(), supporting.getSourceRestrictions());
			FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\t\tRetrieving target paths");
			targetPaths = retrieveConnectingPaths(parameters.getTargetDataset(), parameters.getMainRule().getTargetRestrictions(), supporting.getTargetRestrictions());
			
			// If paths were found
			if(!sourcePaths.isEmpty() || !targetPaths.isEmpty()) {
				System.out.println("######@@@@@@#######");
				System.out.println(sourcePaths);
				System.out.println(targetPaths);
				// Combine every possible path
				Set<List<Path>> navigablePaths = combinePathsCartesianProduct(sourcePaths, targetPaths);
				for(List<Path> paths : navigablePaths) {
					System.out.println("-"+paths.get(0));
					System.out.println("-"+paths.get(1));
					FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\tExploring paths: ");
					FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\t Path source: "+paths.get(0));
					FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\t Path target: "+paths.get(1));
					Set<Tuple<String,String>> filteredLinksTMP = filterLink(supporting, paths.get(0), paths.get(1));
					FrameworkConfiguration.traceLog.addLogLine(Teide.class.getCanonicalName(), "\t\t Links generated: "+filteredLinksTMP.size());
					filteredLinks.addAll(filteredLinksTMP);	
				}
			}
		}
		
		//Store results
		if(parameters.getFilteredLinksFile()!=null && !parameters.getFilteredLinksFile().isEmpty())
			writeResults(filteredLinks, parameters.getFilteredLinksFile());
		
		return filteredLinks;
	}
	

	@SuppressWarnings("unchecked")
	private Set<List<Path>> combinePathsCartesianProduct(Set<Path> sourcePaths, Set<Path> targetPaths){
		Set<List<Path>> cartesianProduct = Sets.newHashSet();
		
		if(sourcePaths.isEmpty()) {
			targetPaths.forEach(tPath -> cartesianProduct.add(emptyPathTuple(null, tPath)));
		}else if(targetPaths.isEmpty()) {
			sourcePaths.forEach(sPath -> cartesianProduct.add(emptyPathTuple(sPath, null)));
		}else {
			cartesianProduct.addAll(Sets.cartesianProduct(sourcePaths,targetPaths));
		}
			
		return cartesianProduct;
			
	}
	
	private List<Path> emptyPathTuple(Path sourcePath, Path targetPath){
		List<Path> result = new ArrayList<Path>();
		result.add(sourcePath);
		result.add(targetPath);
		return result;
	}
	
	
	/** 
	 *  Applies the main rule and stores its links if required
	 */
	private void applyMainLinkRule() {
		TranslatorStringRuleToSparql translator = null;
		Tuple<String,String> sparqlQueries = null;
		
		translator = new TranslatorStringRuleToSparql();
		sparqlQueries = translator.translate(parameters.getMainRule());
		linker.linkDatasets(sparqlQueries, ""); // second parameter is empty to avoid Linker to create an output file 
		if(parameters.getOutputMainRuleLinksFile()!=null && !parameters.getOutputMainRuleLinksFile().isEmpty())
			writeResults(linker.getInstancesLinked(), parameters.getOutputMainRuleLinksFile());
	}
	
	/**
	 * Write filtered links into file
	 */
	private void writeResults(Collection<Tuple<String,String>> links, String outputFileStr) {
		File outputFile = null;
		Collection<String> linksToWrite = null;
		
		linksToWrite = links.parallelStream().map(tuple -> fromTupleToLink(tuple)).collect(Collectors.toSet());
		outputFile = new File(outputFileStr);
		
		try {
			// overrides previous files
			FileUtils.writeLines(outputFile, linksToWrite);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String fromTupleToLink(Tuple<String,String> tupleLink) {
		StringBuffer link = null;
		
		link = new StringBuffer();
		link.append("<").append(tupleLink.getFirstElement()).append("> ");
		link.append("<http://www.w3.org/2002/07/owl#sameAs> ");
		link.append("<").append(tupleLink.getSecondElement()).append("> .");
		
		return link.toString();
	}
	
	
	/**
	 * Finds the set of shortest paths that connect the intialClasses with the targetClasses within the input dataset
	 */
	private Set<Path> retrieveConnectingPaths(String dataset, Collection<String> intialClasses, Collection<String> targetClasses){
		PathFinder pathFinder = null;
		Set<Path> paths = null;
		

		pathFinder = new PathFinder(searchParameters);
		pathFinder.searchPaths(dataset, intialClasses, targetClasses, searchParameters.getMaxSearchDepth());
		paths = pathFinder.getShortesPaths();
		
		return paths;
	}
	
	/**
	 * Applies the specification to the set of instances linked by the main rule
	 */
	private Set<Tuple<String,String>> filterLink(ISpecification<String> rule, Path sourcePath, Path targetPath){
		Tuple<String,String> sparqlQueries = null;
		Set<Tuple<String,String>> irisLinked =  null;
		
		irisLinked = Sets.newHashSet();
		// Format link rule as queries
		sparqlQueries = translateToQueryWithPath(rule, sourcePath, targetPath);
		
		// Link instances
		LinkerK auxiliarLinker = new LinkerK();
		auxiliarLinker.setDatasetSource(this.parameters.getSourceDataset());
		auxiliarLinker.setDatasetTarget(this.parameters.getTargetDataset());
		auxiliarLinker.setInstances(linker.getInstancesLinked());
		auxiliarLinker.linkInstances(sparqlQueries);
		irisLinked = auxiliarLinker.getInstancesLinked();
		
		return irisLinked;
	}
	
	private Tuple<String,String> translateToQueryWithPath(ISpecification<String> rule, Path sourcePath, Path targetPath) {
		TranslatorStringRuleToSparql translator = null;
		Tuple<String, String> ruleQueries = null;
		String sourceQuery = null;
		String targetQuery = null;
		Tuple<String,String> queries = null;
		
		// Retrieve link rule queries
		queries = new Tuple<String,String>();
		translator = new TranslatorStringRuleToSparql();
		ruleQueries = translator.translate(rule);

		// Integrate the path into the query matching variables in both
		sourceQuery = integratePathInLinkRule(ruleQueries.getFirstElement(), sourcePath);
		targetQuery = integratePathInLinkRule(ruleQueries.getSecondElement(), targetPath);
		queries = new Tuple<String,String>(sourceQuery,targetQuery);
		
		return queries;
	}
	
	private String integratePathInLinkRule(String query, Path path) {
		String[] mainVariable = null;
		String lastPathVariable = null;
		String initialPathVariable = null;
		String pathSparql = null;
		String newVar = null;
		if(path!=null) {
			// Retrieve varaibles to replace in both path and query
			mainVariable = SPARQLFactory.getMainVariable(query);
			lastPathVariable = path.getLastVariable();
			initialPathVariable = path.getInitialVariable();
			// Replace variables in path, i.e., connect path with query variables
			pathSparql = path.toSPARQL();
			pathSparql = pathSparql.replace(lastPathVariable, mainVariable[0]);
			// Append path to query
			newVar = SPARQLFactory.generateFreshVar();
			query = query.replace("{", "{\n"+pathSparql)
						 .replace(initialPathVariable, newVar)
						 .replace("DISTINCT "+mainVariable[0], "DISTINCT "+newVar );
		}
		return query;
	}
	
	

}
