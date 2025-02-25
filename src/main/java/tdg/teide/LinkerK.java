package tdg.teide;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import tdg.link_discovery.connector.sparql.evaluator.arq.linker.factory.SPARQLFactory;
import tdg.link_discovery.framework.engine.evaluator.linker.ILinker;
import tdg.link_discovery.middleware.objects.Tuple;
import tdg.link_discovery.middleware.utils.StreamUtils;
import tdg.link_discovery.middleware.utils.Utils;


public class LinkerK implements ILinker {
	// TODO: PARALLELIZE WITH FUTURES
	protected String datasetSourceName, datasetTargetName;
	protected String sourceQuery, targetQuery;
	
	protected static Set<Tuple<String,String>> sampleInstances;
	
	protected Boolean linkindDatasets;
	protected String outputFile;
	protected Integer chunk;
	
	//private Dataset datasetSource, datasetTarget;
	
	//protected Multimap<Tuple<String,String>,Double> instancesLinked;
	private Set<Tuple<String,String>> instancesLinkedSet;
	public static int DEFAULT_THREAD_POOL_SIZE = 200;
	
	public LinkerK(){
		super();
		datasetSourceName="";
		datasetTargetName ="";
		sourceQuery="";
		targetQuery="";
		sampleInstances = new CopyOnWriteArraySet<Tuple<String,String>>();
		//instancesLinked = ArrayListMultimap.create();
		linkindDatasets = false;
		outputFile = "";
		chunk = SPARQLFactory.LINKER_RESULTS_SAVING_CHUNK;
		
	}
	
	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#getInstancesLinked()
	 */
	@Override
	public Set<Tuple<String,String>> getInstancesLinked(){
		return this.instancesLinkedSet;
	}
	
	/*
	 * Link two datasets
	 */
	
	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#linkDatasets(tdg.link_discovery.middleware.objects.Tuple, java.lang.String)
	 */
	@Override
	public void linkDatasets(Tuple<String, String> queries, String outputFile) {
		linkindDatasets = true;
		this.outputFile = outputFile;
		
		instancesLinkedSet = new CopyOnWriteArraySet<Tuple<String,String>>();
		if(nonEmptyQueries(queries)){
			linkInstances(null, null, queries);
		}
		saveResults();
		//System.out.println("Found " + instancesLinkedSet.size()+ " between datasets");
	}
	
	private Boolean nonEmptyQuery(String query){
		Pattern pattern = Pattern.compile("\\{\\s*\\}");
        Matcher matcher = pattern.matcher(query);
		return  !matcher.find();
	}
	
	private Boolean nonEmptyQueries(Tuple<String,String> queries){
		return nonEmptyQuery(queries.getFirstElement()) && nonEmptyQuery(queries.getSecondElement());
	}
	/*
	 * Link only instances
	 */
	
	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#linkInstances(tdg.link_discovery.middleware.objects.Tuple)
	 */
	@Override
	public void linkInstances(Tuple<String,String> queries){
		linkindDatasets = false;
		outputFile = "";
		//instancesLinked = ArrayListMultimap.create();
		instancesLinkedSet = new CopyOnWriteArraySet<Tuple<String,String>>();
		if(nonEmptyQueries(queries)){
			sampleInstances.stream().parallel().forEach(pair-> linkPairOfInstances(pair,queries));
		}
	}
	
	private boolean linkPairOfInstances(Tuple<String,String> instancesToLink, Tuple<String,String> queries){
		//System.out.println("Thread: "+Thread.currentThread().getId());
		String sourceInstance = instancesToLink.getFirstElement();
		String targetInstance = instancesToLink.getSecondElement();
		linkInstances(sourceInstance, targetInstance, queries);
		return true;
	}
	
	
	private void linkInstances(String sourceInstance, String targetInstance, Tuple<String,String> queries) {
		Dataset datasetSource = TDBFactory.createDataset(datasetSourceName);
		datasetSource.begin(ReadWrite.READ);

		// Retrieving variables
		Tuple<String, List<String>> retrievedVariables = retrieveVariablesFromSourceQuery(queries.getFirstElement());
		String mainVariable = retrievedVariables.getFirstElement();
		List<String> variables = retrievedVariables.getSecondElement(); // Does not contain the main var
		
		// Prepare query to be executed
		String queryString = formatSourceQuery(queries.getFirstElement(), mainVariable, sourceInstance);

		System.out.println(queryString);

		ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
		List<Callable<Set<Tuple<String,String>>>> tasks = Lists.newArrayList();
		try {			
			// Execute query
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, datasetSource);
			ResultSet results = qexec.execSelect();
			
			while(results.hasNext()) {
				QuerySolution querySolution = results.next();
				Callable<Set<Tuple<String,String>>> task = () -> {
					return handleSourceResultSet(querySolution, sourceInstance, targetInstance, queries, mainVariable, variables);
				};
				tasks.add(task);
			}
			

			List<Future<Set<Tuple<String,String>>>> futures = executor.invokeAll(tasks);
			for(Future<Set<Tuple<String,String>>> future: futures) {
				try {
					
					Set<Tuple<String,String>> links = future.get();
					if(links!=null)
						this.instancesLinkedSet.addAll(links);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
					// TODO
					System.exit(-1);
				}
			}
			
			
			
			
			// Retrieve and process results in parallel
			qexec.close();
			
		} catch (Exception e) {
			System.out.println("Error with: "+queryString);
			e.printStackTrace();
		}
		datasetSource.end();
		// Shutdown executor
		executor.shutdown();
	}
	
	private Tuple<String, List<String>> retrieveVariablesFromSourceQuery(String query){
		String [] vars = SPARQLFactory.getMainVariable(query);
		List<String> variables = Lists.newArrayList();
		String mainVariable = vars[0];
		variables.addAll(Arrays.asList(vars));
		variables.remove(mainVariable);
		return new Tuple<String,List<String>>(mainVariable, variables);
	}

	private String	formatSourceQuery(String query, String mainVariable, String sourceInstance){
		String queryString = query;
		// if sourceInstance !=null means we are linking examples, hece, embed the example iri in the query
		if(sourceInstance != null){
			queryString = queryString.replace("DISTINCT "+mainVariable, "DISTINCT ");
			queryString = queryString.replace(mainVariable, SPARQLFactory.fixIRIS(sourceInstance));
		}// Other wise don't embed anything.
		queryString = queryString.replace("DISTINCT", ""); // Not removing the DISTINCT throws error in some datasets due to the data structure and incompleteness
		return queryString;
	}
	
	

	private Set<Tuple<String,String>> handleSourceResultSet(QuerySolution soln, String sourceInstance, String targetInstance, Tuple<String, String> queries, String mainVariable, List<String> variables) {
		Set<Tuple<String,String>> links = Sets.newHashSet();
		if (soln != null) {
			// In case sourceInstance is null, hence we are linking the
			// datasets. Retrieve the current instance iri
			if (sourceInstance == null && soln.contains(mainVariable))
				sourceInstance = soln.getResource(mainVariable).toString();
			// Retrieving literal values
			Multimap<String, String> literals = retrieveSourceLiteralsFromQuerySolution(soln, mainVariable, variables);
			System.out.println(literals);
			// Query second dataset
			if (sourceInstance != null) {
				links = queryDatasetTarget(sourceInstance, targetInstance, literals,queries);
			}
			sourceInstance = null; // this line is mandatory to correctly
									// retrieve the next source iri
		}
		System.out.println("-------------------------------------");
		return links;
	}
	
	private Multimap<String, String> retrieveSourceLiteralsFromQuerySolution(QuerySolution soln, String mainVariable, List<String> variables){
		Multimap<String, String> literals = ArrayListMultimap.create();

		
		variables.stream().forEach(variable ->{
			if (soln.contains(variable) && soln.getLiteral(variable)!=null) {
				String literal = soln.getLiteral(variable).getString();
				if (!literals.containsEntry(variable, literal))
					literals.put(variable, literal);
			}
		});
		
		return literals;
	}
	

	private Set<Tuple<String,String>> queryDatasetTarget(String sourceInstance, String targetInstance, Multimap<String, String> literals, Tuple<String, String> queries) {
		Dataset datasetTarget = TDBFactory.createDataset(datasetTargetName);
		datasetTarget.begin(ReadWrite.READ);
		Set<Tuple<String,String>> links = Sets.newHashSet();
		String queryString = replaceVariablesWithLiterals(queries.getSecondElement(), literals);
		// Obtaining relevant variables from query: main var and score var
		String scoreVariable = getTargetScoreVariable(queryString);
		String mainVar = SPARQLFactory.getMainVariable(queryString)[0];
		// Formatting query
		queryString = formatTargetQuery(queryString, mainVar,scoreVariable, targetInstance);
		
		try {
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, datasetTarget);
			ResultSet results = qexec.execSelect();
			StreamUtils.asStream(results).parallel().forEach(solution -> links.add(handleTargetQuerySolution(solution, sourceInstance, targetInstance, mainVar, scoreVariable)));
			qexec.close();
		} catch (Exception e) {
			System.out.println("Failed executing target query in " + this.datasetTargetName + " the query:\n" + queryString+" \n\n ORIGINAL QUERY"+queries.getSecondElement());
			e.printStackTrace();
		}
		datasetTarget.end();
		
		return links;
	}
	
	private String getTargetScoreVariable(String targetQuery){
		String scoreVariable = targetQuery.substring(targetQuery.indexOf("FILTER")+6, targetQuery.lastIndexOf("0"));
		scoreVariable = scoreVariable.substring(scoreVariable.indexOf("?"), scoreVariable.lastIndexOf(">")).trim();
		return scoreVariable;
	}
	
	private String formatTargetQuery(String targetQuery, String mainVar, String scoreVariable, String targetInstance){
		String queryString = targetQuery;
	
		if(targetInstance!=null){
			// Adding score variable to SELECT statement & replacing mainVar for the instance iri
			// The regex that follows the main var is in case we havae as main var ?hc and then a var ?hcX. Withouth the regex an error would occur
			// because the non-main var would be partialy replaced with the iri of the main var rather than a literañ
			queryString = queryString.replaceFirst("\\"+mainVar+"[^\\,0-9a-zA-Z]", scoreVariable);  
			queryString = queryString.replaceAll("\\"+mainVar+"[^\\,0-9a-zA-Z]", SPARQLFactory.fixIRIS(targetInstance)+" ");
			
		}else{
			// We are linking the datasets, hence we don't replace the main var for the score 
			StringBuilder str = new StringBuilder();
			str.append("\\").append(mainVar);
			queryString = queryString.replaceFirst(str.toString()+"[^\\,]", str.append(" ").append(scoreVariable).toString());
		}
		
		queryString = queryString.replace("DISTINCT", ""); // Not removing the DISTINCT throws error in some datasets due to the data structure and incompleteness
		
		return queryString;
	}
	
	private Tuple<String, String> handleTargetQuerySolution(QuerySolution soln, String sourceInstance, String targetInstance, String mainVariable, String scoreVariable) {
		Tuple<String, String> instancesToLink =  null;
		if (soln.contains(scoreVariable)) {
			
			Double score = soln.get(scoreVariable).asLiteral().getDouble();
			if (targetInstance == null && soln.contains(mainVariable))
				targetInstance = soln.getResource(mainVariable).toString();
			if (score != null && targetInstance != null) { // TODO: After this line an error ocurr since source literals retrieved are empty
				instancesToLink = new Tuple<String, String>( sourceInstance, targetInstance);
				try{
				
					instancesLinkedSet.add(instancesToLink);
				}catch(Exception e){
					e.printStackTrace();
					System.out.println("Error block");
					System.out.println(instancesToLink);
					System.out.println("End block");
				}
				targetInstance = null; // Set this to null in order to update the targetInstance variable in the next iteration
				// instancesLinked.put(instancesToLink, score);
			}
		}
		return instancesToLink;
	}

	private String replaceVariablesWithLiterals(String queryString, Multimap<String, String> literals) {
		String query = queryString;
		for(String var:literals.keySet()){
			for(String literal:literals.get(var)){
				String literalFixed = SPARQLFactory.fixLiterals(literal);
				StringBuffer literalToReplace = new StringBuffer();
				literalToReplace.append("\"").append(literalFixed).append("\"");
				query = query.replace(var,literalToReplace.toString());
			
			}
		}
		return query;
	}
	

	
	private void saveResults() {
		File outputFileF = new File(outputFile);
		outputFileF.delete();
		instancesLinkedSet.stream().parallel().forEach(tuple -> Utils.appendLineInCSV(outputFile, generateOutputFileLine(tuple)));
	}
	
	private String generateOutputFileLine(Tuple<String,String> tuple){
		StringBuilder str = new StringBuilder();
		str.append(SPARQLFactory.fixIRIS(tuple.getFirstElement()));
		str.append(" <http://www.w3.org/2002/07/owl#sameAs> ");
		str.append(SPARQLFactory.fixIRIS(tuple.getSecondElement())).append(" .\n");
		return str.toString();
	}

	

	/*
	 * Getters & Setters
	 */
	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#setDatasetSource(java.lang.String)
	 */
	@Override
	public void setDatasetSource(String datasetSource) {
		this.datasetSourceName = datasetSource;
	}

	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#setDatasetTarget(java.lang.String)
	 */
	@Override
	public void setDatasetTarget(String datasetTarget) {
		this.datasetTargetName = datasetTarget;
	}

	/* (non-Javadoc)
	 * @see tdg.link_discovery.connector.sparql.evaluator.arq.linker.ILinker#setInstances(java.util.Set)
	 */
	@Override
	public void setInstances(Set<Tuple<String, String>> instances) {
		LinkerK.sampleInstances = instances;
	}

	@Override
	public void setDatasetSource(Model datasetSource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDatasetTarget(Model datasetTarget) {
		// TODO Auto-generated method stub
		
	}


	
}
