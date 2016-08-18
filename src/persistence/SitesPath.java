package persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

public class SitesPath {

	private static String SERVER_URL = "http://localhost:10035";
	private static String CATALOG_ID = "system";
	private static String REPOSITORY_ID = "data";
	private static String USERNAME = "adely";
	private static String PASSWORD = "adely";
	
	private static AGRepository repository;
	private static AGAbstractRepository combinedRepo;
	private static AGRepositoryConnection conn;
	private static AGGraphMaker maker;	
	private static ArrayList<String>  settledNodes; // S
	private static ArrayList<String>  unSettledNodes; //Q
	private static Map<String, String> predecessors; //
	private static Map<String, Double> distance; //w{u,v}
	private static Resource source ;
	private static Resource destination ;
	private static Model model; //V
	
	
	private static AGGraph ConnectRepository()
			throws Exception {					
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);	
		AGCatalog catalog = server.getCatalog(CATALOG_ID);					
		repository = catalog.openRepository(REPOSITORY_ID);			
		conn = repository.getConnection();
		maker = new AGGraphMaker(conn);
		AGGraph graph = maker.getGraph();
		return graph;			
	}
	private static void ConnectCombinedRepository() throws Exception {	
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);	
		AGRepository ontologiesRespository = server.getCatalog(CATALOG_ID).openRepository("ontologies");
		AGRepository dataRespository = server.getCatalog(CATALOG_ID).openRepository("data");
	    combinedRepo = server.federate(ontologiesRespository, dataRespository);		    
	    combinedRepo.initialize();
	    conn = combinedRepo.getConnection();
	}
	
	public static JsonObject getShortestPath(String site, String sourceid, String targetid) throws Exception{
		AGGraph graph = ConnectRepository();
		AGModel agmodel = new AGModel(graph);			
		unSettledNodes = new ArrayList<String>();		
		settledNodes = new ArrayList<String>();
		source = agmodel.getResource(sourceid);
		destination = agmodel.getResource(targetid);
		model = ModelFactory.createDefaultModel();	
		model.add(agmodel.listStatements());
		
		/*get nodes*/
		StmtIterator iterator = model.listStatements(model.getResource(site),model.getProperty("http://purl.org/dc/terms/hasPart"), (RDFNode)null);
		graph.close();
		conn.close();
		repository.close();
		ConnectCombinedRepository();
		orderNodes(source);		
		execute();	
		JsonObject path = getPath(destination);
		conn.close();
		combinedRepo.close();
		return path;
	}
	
	private static void execute() throws Exception{
		distance = new HashMap<String, Double>();
	    predecessors = new HashMap<String, String>();
	    distance.put(source.toString(), 0.0);	   
	    while (unSettledNodes.size() > 0) {
	      RDFNode node = extractMin();	      
	      ArrayList<RDFNode> adjacentNodes = getNeighbors(node);
		    for (int i = 0; i < adjacentNodes.size(); i++) {		    	 
		    	relax(node,adjacentNodes.get(i));
			} 
		    
	    }
	}
	private static RDFNode extractMin() {		
		String next = unSettledNodes.get(0);
		unSettledNodes.remove(0);
		if(!settledNodes.contains(next)){
			return model.getResource(next);
		}
		else
			return extractMin();
	  		
	}
		 
	 private static void relax(RDFNode node, RDFNode target) throws Exception {	
		 Double dist = getDistance(node, target);
		 Double targetShortDist = getShortestDistance(target);
		 Double nodeShortDist = getShortestDistance(node);
		      if (targetShortDist > nodeShortDist
		          + dist) {		    	  
		        distance.put(target.toString(), nodeShortDist + dist);		        
		        predecessors.put(target.toString(), node.toString());		        
		      }    	      
	}

	 private static Double getShortestDistance(RDFNode destination) {	    
		 if (distance.containsKey(destination.toString())) {
		    return distance.get(destination.toString());
		 } else {
		    return Double.MAX_VALUE;
		 }
	}
	 
	 private static Double getDistance(RDFNode node, RDFNode target) throws Exception {		 
		 
			maker = new AGGraphMaker(conn);		
			AGModel model = new AGModel(maker.getGraph());			
			String queryString = "prefix dcterms:<http://purl.org/dc/terms/>"
					+ " prefix guiar:<http://www.iff.edu.br/ontologies/guiar#>"
					+ " Select ?distance"						
					+ " WHERE { ?node guiar:link ?adjacency. filter(?node = <"+ node.toString()+">) "
					+ " ?adjacency guiar:link ?target. filter(?target = <"+ target.toString()+">)"
					+ " ?adjacency guiar:distance ?distance.}";	
				
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			   tupleQuery.setIncludeInferred(true);
			   TupleQueryResult result = tupleQuery.evaluate();
			StmtIterator iterator = model.listStatements();
			
			while (result.hasNext()) {        	
	        	BindingSet solution= result.next();	        	
	        	String distance = solution.getValue("distance").stringValue();
	        	return Double.valueOf(distance);
			}		   
		    throw new RuntimeException("Should not happen");
		  }
/*Vai Mudar*/
	private static ArrayList<RDFNode> getNeighbors(RDFNode node) throws Exception {
		ArrayList<RDFNode> neighbors = new ArrayList<RDFNode>();
		
		
		maker = new AGGraphMaker(conn);		
		AGModel model = new AGModel(maker.getGraph());		
		String queryString = "prefix dcterms:<http://purl.org/dc/terms/>"
				+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "prefix guiar: <http://www.iff.edu.br/ontologies/guiar#>"
				+ "Select ?adjacentLocation"						
				+ " WHERE { ?s guiar:link ?o. filter(?s = <"+ node.toString()+"> && isBlank(?o)) "
				+ "?o guiar:link ?adjacentLocation.}";	
		
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		   tupleQuery.setIncludeInferred(true);
		   TupleQueryResult result = tupleQuery.evaluate();
		StmtIterator iterator = model.listStatements();
		
		while (result.hasNext()) {        	
        	BindingSet solution= result.next();        	
        	String adjacentLocation = solution.getValue("adjacentLocation").stringValue();
        	if(!adjacentLocation.equals(node.toString()))
        		neighbors.add(model.getResource(adjacentLocation));
		}
		
		return neighbors;
	}
		
	 private static JsonObject getPath(RDFNode target) throws QueryEvaluationException {
		    LinkedList<String> path = new LinkedList<String>();
		    String step = target.toString();
		    // check if a path exists
		    if (predecessors.get(step) == null) {
		      return null;
		    }
		    path.add(step.toString());
		    while (predecessors.get(step) != null) {
		    	
		      step = predecessors.get(step);
		      path.add(step.toString());
		    }
		    // Put it into the correct order
		    Collections.reverse(path);
		    JsonBuilderFactory factory = Json.createBuilderFactory(null);
		    JsonObjectBuilder pathOB = factory.createObjectBuilder();
		   // JsonObjectBuilder directionOB = factory.createObjectBuilder();
		    for (int i = 0; i < path.size(); i++) {
		    	JsonObjectBuilder directionOB = factory.createObjectBuilder();
		    	if(i == path.size()-1){
		    		pathOB.add(String.valueOf(i+1), directionOB.add("location", path.get(i))
							.add("direction", "end"));
		    	}
		    	else{
				pathOB.add(String.valueOf(i+1), directionOB.add("location", path.get(i))
						.add("direction", getDirection(path.get(i), path.get(i+1))));
		    	}
			}		    
		    return pathOB.build();
		  }
	 private static String getDirection(String node, String target) throws QueryEvaluationException{
		 String queryString = "prefix dcterms:<http://purl.org/dc/terms/>"
					+ " prefix guiar:<http://www.iff.edu.br/ontologies/guiar#>"
					+ " Select ?linkDirection"						
					+ " WHERE { ?node guiar:link ?adjacency. filter(?node = <"+ node+">) "
					+ " ?adjacency ?linkDirection ?target. filter(?target = <"+ target+"> &&"
							+ "?linkDirection != guiar:link)}";	
			
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			   tupleQuery.setIncludeInferred(true);
			   TupleQueryResult result = tupleQuery.evaluate();
			StmtIterator iterator = model.listStatements();			
			String direction = null;
			while (result.hasNext()) {        	
	        	BindingSet solution= result.next();
	        	direction = solution.getValue("linkDirection").stringValue();
			}
			if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#southLink"))
				return "south";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#northLink"))
				return "north";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#eastLink"))
				return "east";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#westLink"))
				return "west";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#northwestLink"))
				return "northwest";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#northeastLink"))
				return "northeast";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#southwestLink"))
				return "southwest";
			else if(direction.equalsIgnoreCase("http://www.iff.edu.br/ontologies/guiar#southeastLink"))
				return "southeast";
			return null;
	 }
	 private static void orderNodes(RDFNode node) throws Exception{
		 ArrayList<RDFNode> adjcents = getNeighbors(node);	
		 unSettledNodes.add(node.toString());			 
		Collections.sort(adjcents, new Comparator<RDFNode>(){
			 @Override
			    public int compare(RDFNode n1, RDFNode n2) {
			        try {
			        	Double n1Dist = getDistance(node,n1);
			        	Double n2Dist = getDistance(node,n2);
						if (n1Dist > n2Dist)
						    return 1;
						if (n1Dist < n2Dist)
				            return -1;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        
			        return 0;
			    }
		});		
		 for (int i = 0; i < adjcents.size(); i++) {
			 if(!unSettledNodes.contains(adjcents.get(i).toString()))
				 orderNodes(adjcents.get(i));
		 }	 
			 
		 }	 
}
