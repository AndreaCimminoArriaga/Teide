package tdg.teide;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;

import tdg.evaluator.LinksEvaluator;
import tdg.evaluator.model.ConfusionMatrix;
import tdg.evaluator.parameters.LinksEvaluatorParameters;
import tdg.evaluator.parameters.LinksEvaluatorParametersReader;
import tdg.link_discovery.middleware.framework.configuration.FrameworkConfiguration;
import tdg.link_discovery.middleware.log.Logger;
import tdg.pathfinder.parameters.PathFinderParameters;
import tdg.pathfinder.parameters.PathFinderParametersReader;
import tdg.teide.parameters.InputReader;
import tdg.teide.parameters.TeideParameters;

public class TeideLinker {

	private static String parametersFile;

	
	public static void main(String[] args) throws Exception {
		// Disable jena logs and use ours
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
				
		// -- Teide
		TeideParameters parameters = null;
		Teide teide = null; 
		// -- PathFinder
		PathFinderParameters parametersSearch = null;
		// -- Evaluator
		LinksEvaluatorParameters parametersEvaluator = null;
		LinksEvaluator evaluator = null;
		ConfusionMatrix matrix = null;
		// -- results
		List<String> results = null;
		
		// Retrieve configuration file
		parametersFile = "./restaurants-input.json";
		results = Lists.newArrayList();
		
		// Execute Teide and store results
		parameters = InputReader.parseFromJSON(parametersFile);
		parametersSearch = PathFinderParametersReader.parseFromJSON(parametersFile);
		FrameworkConfiguration.traceLog = new Logger(new StringBuffer(parameters.getResultsFolder()).append("/execution-log.txt").toString(), 1);
		teide = new Teide(parameters, parametersSearch);
		FrameworkConfiguration.traceLog.addLogLine(TeideLinker.class.getCanonicalName(), "Executing Teide Algorithm");
		long startTime = System.nanoTime();
		teide.execute();
		long stopTime = System.nanoTime();
	    long elapsedTime = (stopTime - startTime)/1000000;
	    FrameworkConfiguration.traceLog.addLogLine(TeideLinker.class.getCanonicalName(), "Executed in "+elapsedTime+" (ms)");
		
		FrameworkConfiguration.traceLog.writeCurrentCachedLines();
		
	}

}
