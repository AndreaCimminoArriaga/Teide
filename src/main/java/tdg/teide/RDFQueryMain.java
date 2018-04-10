package tdg.teide;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDBFactory;

public class RDFQueryMain {

	public static void main(String[] args) {
		Integer old = GlobalCounter;
		String queryString = "#Prefixes\n" + 
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"PREFIX agg:<java:tdg.link_discovery.connector.sparql.evaluator.arq.linker.aggregates.>\n" + 
				"PREFIX str:<java:tdg.link_discovery.connector.sparql.evaluator.arq.linker.string_similarities.>\n" + 
				"PREFIX trn:<java:tdg.link_discovery.connector.sparql.evaluator.arq.linker.transformations.>\n" + 
				"#Query\n" + 
				"SELECT  ?L040 {\n" + 
				"	<http://newcastle.rkbexplorer.com/id/publications/article/215> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/RaeNewcastleTarget> .\n" + 
				"	<http://newcastle.rkbexplorer.com/id/publications/article/215> <http://www.aktors.org/ontology/portal#has-author> ?v04 .\n" + 
				"	?v04 rdf:type <http://www.aktors.org/ontology/portal#Lecturer-In-Academia> .\n" + 
				"	?v04 <http://www.aktors.org/ontology/portal#family-name> ?ux64 .\n" + 
				" 	#Bind and Filter\n" + 
				"	BIND ( str:JaroWinklerTFIDFSimilarity(\"Koutny\",?ux64,0.5) AS ?L040 ) .\n" + 
				"	FILTER ( ?L040> 0 ) \n"+
				"}";
				 
		//System.out.println("------");
		long startTime = System.nanoTime();
		execQuery(queryString, "./tdb-data/newcastle");
		long stopTime = System.nanoTime();
	    long elapsedTime = (stopTime - startTime)/1000000;

	}
	

	public static Integer GlobalCounter = 0;
	public static Set<String> execQuery(String queryString, String repositoryName) {
		Set<String> finalResults = new HashSet<String>();
		Dataset dataset = TDBFactory.createDataset(repositoryName);
		dataset.begin(ReadWrite.READ);
	
		int counter = 0;
		Set<String> types = new HashSet<String>();
		try {
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
			ResultSet results = qexec.execSelect();
				while(results.hasNext()){
					if(results!=null){
						QuerySolution soln = results.nextSolution();
						System.out.println(">>>>"+soln);
						if(soln != null){
							GlobalCounter++;
						/*String iri =soln.get("?s").toString();
						if(getIrisFromGold().contains(iri))
							 counter++;
						*/}
					}
				}
				qexec.close();			
				
		}catch(Exception e){
				System.out.println("Failed executing in "+repositoryName+" the query:\n"+queryString);
				e.printStackTrace();			
		} 
		dataset.end();
		
		
		for(String type:types){
			System.out.println(type);
		}
		System.out.println(counter);
		System.out.println(GlobalCounter);
		return finalResults;
	}
	
	public static Set<String> getIrisFromGold(){
		Set<String> iris = Sets.newHashSet();
//		iris.add("http://www.imdb_movies.nt#work/id=174936");
//		iris.add("http://www.imdb_movies.nt#work/id=184783");
//		iris.add("http://www.imdb_movies.nt#work/id=263856");
//		iris.add("http://www.imdb_movies.nt#work/id=281463");
//		iris.add("http://www.imdb_movies.nt#work/id=281668");
//		iris.add("http://www.imdb_movies.nt#work/id=327415");
//		iris.add("http://www.imdb_movies.nt#work/id=344787");
//		iris.add("http://www.imdb_movies.nt#work/id=365188");
//		iris.add("http://www.imdb_movies.nt#work/id=450726");
//		iris.add("http://www.imdb_movies.nt#work/id=718800");
//		iris.add("http://www.imdb_movies.nt#work/id=1381665");
//		iris.add("http://www.imdb_movies.nt#work/id=1490285");
//		iris.add("http://www.imdb_movies.nt#work/id=1594693");
//		iris.add("http://www.imdb_movies.nt#work/id=1785944");
//		iris.add("http://www.imdb_movies.nt#work/id=1786094");
//		iris.add("http://www.imdb_movies.nt#work/id=1849476");
//		iris.add("http://www.imdb_movies.nt#work/id=1945093");
//		iris.add("http://www.imdb_movies.nt#work/id=2011495");
//		iris.add("http://www.imdb_movies.nt#work/id=2228243");
//		iris.add("http://www.imdb_movies.nt#work/id=2547566");
//		iris.add("http://www.imdb_movies.nt#work/id=2574073");
//		iris.add("http://www.imdb_movies.nt#work/id=2580315");
//		iris.add("http://www.imdb_movies.nt#work/id=2593902");
//		iris.add("http://www.imdb_movies.nt#work/id=2593903");
//		iris.add("http://www.imdb_movies.nt#work/id=2593904");
//		iris.add("http://www.imdb_movies.nt#work/id=2598003");
//		iris.add("http://www.imdb_movies.nt#work/id=2607166");
//		iris.add("http://www.imdb_movies.nt#work/id=2607167");
//		iris.add("http://www.imdb_movies.nt#work/id=2607169");
//		iris.add("http://www.imdb_movies.nt#work/id=2607170");
//		iris.add("http://www.imdb_movies.nt#work/id=2607171");
//		iris.add("http://www.imdb_movies.nt#work/id=2630179");
//		iris.add("http://www.imdb_movies.nt#work/id=2654014");
//		iris.add("http://www.imdb_movies.nt#work/id=2662103");
//		iris.add("http://www.imdb_movies.nt#work/id=2673514");
//		iris.add("http://www.imdb_movies.nt#work/id=2681716");
//		iris.add("http://www.imdb_movies.nt#work/id=2681718");
//		iris.add("http://www.imdb_movies.nt#work/id=2681719");
//		iris.add("http://www.imdb_movies.nt#work/id=2681724");
//		iris.add("http://www.imdb_movies.nt#work/id=2687553");
//		iris.add("http://www.imdb_movies.nt#work/id=2687557");
//		iris.add("http://www.imdb_movies.nt#work/id=2688003");
//		iris.add("http://www.imdb_movies.nt#work/id=2688005");
//		iris.add("http://www.imdb_movies.nt#work/id=2688007");
//		iris.add("http://www.imdb_movies.nt#work/id=2688009");
//		iris.add("http://www.imdb_movies.nt#work/id=2697544");
//		iris.add("http://www.imdb_movies.nt#work/id=2697545");
//		iris.add("http://www.imdb_movies.nt#work/id=2703511");
//		iris.add("http://www.imdb_movies.nt#work/id=2703512");
//		iris.add("http://www.imdb_movies.nt#work/id=2739342");
//		iris.add("http://www.imdb_movies.nt#work/id=2739344");
//		iris.add("http://www.imdb_movies.nt#work/id=2739345");
//		iris.add("http://www.imdb_movies.nt#work/id=2763723");
//		iris.add("http://www.imdb_movies.nt#work/id=2816697");
//		iris.add("http://www.imdb_movies.nt#work/id=2835061");
//		iris.add("http://www.imdb_movies.nt#work/id=2880924");
//		iris.add("http://www.imdb_movies.nt#work/id=2880925");
//		iris.add("http://www.imdb_movies.nt#work/id=3031438");
//		iris.add("http://www.imdb_movies.nt#work/id=3049209");
//		iris.add("http://www.imdb_movies.nt#work/id=3049212");
//		iris.add("http://www.imdb_movies.nt#work/id=3049217");
//		iris.add("http://www.imdb_movies.nt#work/id=3049225");
//		iris.add("http://www.imdb_movies.nt#work/id=3049252");
//		iris.add("http://www.imdb_movies.nt#work/id=3168341");
//		iris.add("http://www.imdb_movies.nt#work/id=3168342");
//		iris.add("http://www.imdb_movies.nt#work/id=3168343");
//		iris.add("http://www.imdb_movies.nt#work/id=3168344");
//		iris.add("http://www.imdb_movies.nt#work/id=3168349");
//		iris.add("http://www.imdb_movies.nt#work/id=3301114");
//		iris.add("http://www.imdb_movies.nt#work/id=3301115");
//		iris.add("http://www.imdb_movies.nt#work/id=3318274");
//		iris.add("http://www.imdb_movies.nt#work/id=3373499");
//		iris.add("http://www.imdb_movies.nt#work/id=3373500");
//		iris.add("http://www.imdb_movies.nt#work/id=3422757");
//		iris.add("http://www.imdb_movies.nt#work/id=3449143");
//		iris.add("http://www.imdb_movies.nt#work/id=3449144");
//		iris.add("http://www.imdb_movies.nt#work/id=3449145");
//		iris.add("http://www.imdb_movies.nt#work/id=3494081");
//		iris.add("http://www.imdb_movies.nt#work/id=3494085");
//		iris.add("http://www.imdb_movies.nt#work/id=3494092");
//		iris.add("http://www.imdb_movies.nt#work/id=3507742");
//		iris.add("http://www.imdb_movies.nt#work/id=3507743");
//		iris.add("http://www.imdb_movies.nt#work/id=3507745");
//		iris.add("http://www.imdb_movies.nt#work/id=3507794");
//		iris.add("http://www.imdb_movies.nt#work/id=3537166");
//		iris.add("http://www.imdb_movies.nt#work/id=3537171");
//		iris.add("http://www.imdb_movies.nt#work/id=3566268");
//		iris.add("http://www.imdb_movies.nt#work/id=3571518");
//		iris.add("http://www.imdb_movies.nt#work/id=3571519");
//		iris.add("http://www.imdb_movies.nt#work/id=3641147");
//		iris.add("http://www.imdb_movies.nt#work/id=3642781");
//		iris.add("http://www.imdb_movies.nt#work/id=3671070");
//		iris.add("http://www.imdb_movies.nt#work/id=3671074");
//		iris.add("http://www.imdb_movies.nt#work/id=3671076");
//		iris.add("http://www.imdb_movies.nt#work/id=3671149");
//		iris.add("http://www.imdb_movies.nt#work/id=3671150");
//		iris.add("http://www.imdb_movies.nt#work/id=3671153");
//		iris.add("http://www.imdb_movies.nt#work/id=3671154");
//		iris.add("http://www.imdb_movies.nt#work/id=3696923");
//		iris.add("http://www.imdb_movies.nt#work/id=3723111");
//		iris.add("http://www.imdb_movies.nt#work/id=3754518");
		return iris;
	}

}
