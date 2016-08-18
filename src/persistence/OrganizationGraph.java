package persistence;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGVirtualRepository;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class OrganizationGraph {

	
	private static String SERVER_URL = "http://localhost:10035";
	private static String CATALOG_ID = "system";
	private static String REPOSITORY_ID = "data";
	private static String USERNAME = "adely";
	private static String PASSWORD = "adely";	
	private static AGRepository repository;
	private static AGRepositoryConnection conn;
	private static AGGraphMaker maker;
	private static AGAbstractRepository combinedRepo;

	 
	
	private static AGGraph ConnectSingleRepository() throws Exception {			
		
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
	public static String getGraph(String resourceURI, String extensao) throws Exception{			
		ConnectCombinedRepository();
		Model fakeModel = ModelFactory.createDefaultModel();
		fakeModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		fakeModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
		fakeModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		fakeModel.setNsPrefix("guiar", "http://www.iff.edu.br/ontologies/guiar#");
		fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");		
		boolean exist = false;		
		
		String queryString = "Describe <"+resourceURI+"> ?s ?p ?o ";		  
		GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
		describeQuery.setIncludeInferred(true);
		GraphQueryResult  result = describeQuery.evaluate();
		 
		 
		 while(result.hasNext()){
			 org.openrdf.model.Statement solution = result.next();						
				Resource r ;
				Resource o;
				Property p = fakeModel.createProperty(solution.getPredicate().stringValue());				
				if(solution.getSubject().toString().contains("_:")){
						AnonId id = new AnonId(solution.getSubject().toString());
						 r = fakeModel.createResource(id);
						 fakeModel.add(r, p, solution.getObject().toString());
				}
				else{	
					r = fakeModel.createResource(solution.getSubject().stringValue());
					if(solution.getObject().toString().contains("_:")){
						AnonId id = new AnonId(solution.getSubject().toString());
						 o = fakeModel.createResource(id);							 
						 fakeModel.add(r, p, o);
					}
					else
						 fakeModel.add(r, p, solution.getObject().stringValue());					 
				}
				exist = true;			 
		 }		 
		if(exist){
			OutputStream stream = new ByteArrayOutputStream() ;					
			if(extensao.equals("rdf"))						
				fakeModel.write(stream, "RDF/XML-ABBREV");				
			if(extensao.equals("ttl")){					
				fakeModel.write(stream, "TURTLE");
			}				
			return stream.toString();
		}
		else
			return "False";
	}
	
	public static String getAll(String format) throws Exception{
		 ConnectCombinedRepository();
			Model fakeModel = ModelFactory.createDefaultModel();
			fakeModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
			fakeModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
			fakeModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
			fakeModel.setNsPrefix("guiar", "http://www.iff.edu.br/guiar/ontologies/sites#");
			fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
			boolean exist = false;			
			String queryString = "prefix foaf:<http://xmlns.com/foaf/0.1/>"
					+  "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "Select ?s ?p ?o "						
					+ " WHERE { ?s rdf:type foaf:Organization. "
					+ "?s ?p ?o.filter regex(str(?s),\"RestWebService/organizations/\",\"i\")}";
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			   tupleQuery.setIncludeInferred(true);
			   TupleQueryResult result = tupleQuery.evaluate();
			   
			while (result.hasNext()) {   	
				exist = true;
				BindingSet solution= result.next();	        	
				Resource r = fakeModel.createResource(solution.getValue("s").toString());
				Property p = fakeModel.createProperty(solution.getValue("p").toString());
				String o = solution.getValue("o").toString();
				fakeModel.add(r,p,o);
				if(o.contains("_:")){
					AnonId id = new AnonId(o);
					queryString =  "Select ?p ?o "						
   					+ " WHERE { "+o+" ?p ?o.}";
					TupleQuery anonQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
					anonQuery.setIncludeInferred(true);
					TupleQueryResult anonresult = anonQuery.evaluate();
					while (anonresult.hasNext()) {			        	
						BindingSet triples= anonresult.next();		        	
						Resource anonResource = fakeModel.createResource(id);
						Property property = fakeModel.createProperty(triples.getValue("p").toString());
						String object = triples.getValue("o").toString();
						fakeModel.add(anonResource, property, object);
					}
       		
				}
       	
       }
			conn.close();
			combinedRepo.close();
			if(!exist)
				return null;
			OutputStream stream = new ByteArrayOutputStream() ;					
			if(format.equals("rdf"))						
				fakeModel.write(stream, "RDF/XML-ABBREV");				
			if(format.equals("ttl"))				
				fakeModel.write(stream, "TURTLE");								
			return stream.toString();
			
		
	}
	
	public static String setOrganizationGraph(JsonObject json) throws Exception{			
		AGGraph graph = ConnectSingleRepository();						
		AGModel model = new AGModel(graph);	
		Iterator subjects= model.listSubjects();
		int newid = 0;
		int newanonid= 0;			
		while(subjects.hasNext()){								
			Resource r = (Resource) subjects.next();			
			if((r.toString().contains("http://localhost:8080/RestWebService/organizations/"))&& !r.toString().contains("groups")){/*Verify if it's the ontology statement*/
				int lastid = Integer.parseInt(r.toString().substring(51));					
				if(lastid>newid)
						newid = lastid;
			}				
		}
		
		Resource resource =	model.createResource("http://localhost:8080/RestWebService/organizations/" + (newid+1));
		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceOrganization =	model.createResource("http://xmlns.com/foaf/0.1/Organization" );
		model.add(resource, type, resourceOrganization);
		Iterator iterator = json.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();
			String value = entry.getValue().toString();			
			if(!value.equals(""))						
				model.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));
					
		}				
		maker.close();
		conn.close();
		repository.shutDown();
		return resource.getURI();
	} 
	
	public static void updateOrganizationGraph(String uri, JsonObject json) throws Exception{			
		AGGraph graph = ConnectSingleRepository();			
		AGModel model = new AGModel(graph);			
		Model addModel = ModelFactory.createDefaultModel();						
		Resource resource =	addModel.createResource(uri);			
		StmtIterator result = model.listStatements();					
		while (result.hasNext()) {				
			Statement st = result.next();	
			if(!st.getSubject().isAnon()){
				if(st.getSubject().getURI().equals(uri) && !st.getPredicate().toString().contains("member")){					
					if(st.getObject().isAnon())							
						model.removeAll((Resource)st.getObject(), (Property)null, (RDFNode)null);
					model.remove(st);
				}
			}
		}			
		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceOrganization =	model.createResource("http://xmlns.com/foaf/0.1/Organization" );
		addModel.add(resource, type, resourceOrganization);
		Iterator iterator = json.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();											
			String value = entry.getValue().toString();
			if(!value.equals(null))															 
				addModel.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));							
		}				
					
		model.add(addModel);
		maker.close();
		conn.close();
		repository.shutDown();						
	}		
		
	public static boolean deleteOrganizationGraph(String resourceURI) throws Exception{		
		AGGraph graph = ConnectSingleRepository();			
		AGModel model = new AGModel(graph);
		Resource resource = model.getResource(resourceURI);		
		StmtIterator iterator = model.listStatements();	
		ArrayList<String> anon = new ArrayList<String>();
		boolean exist = false; 		
		while(iterator.hasNext()){
			Statement st = iterator.next();					
			if(st.getSubject().toString().equals(resourceURI)){					
				if(st.getPredicate().toString().equals("http://xmlns.com/foaf/0.1/member")){					
					return false;
				}
			}
		}
		iterator = model.listStatements();
		while(iterator.hasNext()){
			Statement st = iterator.next();					
			if(st.getSubject().toString().equals(resourceURI) || st.getObject().toString().equals(resourceURI)){					
				model.remove(st);
				exist = true;
				}
			
		}
		/*Remove members*/
		 iterator = model.listStatements();
		while(iterator.hasNext()){					
			Statement st = iterator.next();						
			if(anon.contains(st.getSubject().toString()))								
				model.remove(st);
		
		}
		maker.close();
		conn.close();
		repository.shutDown();
		return exist;
	}
	public static String setGroupGraph(String organizationUri, JsonObject groupsJson) throws Exception{
		AGGraph graph = ConnectSingleRepository();			
		AGModel model = new AGModel(graph);		
		Iterator subjects= model.listSubjects();
		int newid = 0;
		int newanonid= 0;			
		while(subjects.hasNext()){								
			Resource r = (Resource) subjects.next();			
			if(r.toString().contains("groups")){				
				int lastid = Integer.parseInt(r.toString().substring(r.toString().lastIndexOf("/")+1));					
				if(lastid>newid)
						newid = lastid;
			}				
		}
		Resource organization = model.getResource(organizationUri);
		Property memberProperty = model.getProperty("http://xmlns.com/foaf/0.1/member");
		Resource group = model.createResource(organizationUri+"/groups/"+(newid+1));
		model.add(organization, memberProperty, group);
		Iterator iterator = groupsJson.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();			
			if(entry.getKey().contains("member")){
				JsonArray array= (JsonArray)entry.getValue();						
				Iterator arrayValue = array.iterator();
				while(arrayValue.hasNext()){
					String member = arrayValue.next().toString();					
					model.add(group, model.getProperty(entry.getKey()), member.substring(1, member.length()-1));
									
				}
			}
			else{
				String value = entry.getValue().toString();
				if(!value.equals(null))						
					model.add(group, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));
			}
					
		}
		maker.close();
		conn.close();
		repository.shutDown();
		return group.getURI().toString();
	}
	public static void updateGroupGraph(String orguri, String groupuri, JsonObject groupsJson) throws Exception{
		AGGraph graph = ConnectSingleRepository();			
		AGModel model = new AGModel(graph);	
		StmtIterator deteleiterator = model.listStatements();
		while(deteleiterator.hasNext()){
			Statement st = deteleiterator.next();					
			if(st.getSubject().toString().equals(groupuri) || st.getObject().toString().equals(groupuri)){								
				model.remove(st);			
				}			
		}
		deleteGroupGraph(groupuri);
		
		Resource organization = model.getResource(orguri);
		Property memberProperty = model.getProperty("http://xmlns.com/foaf/0.1/member");
		Resource group = model.getResource(groupuri);
		model.add(organization, memberProperty, group);
		Iterator iterator = groupsJson.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();
			if(entry.getKey().contains("member")){
				JsonArray array= (JsonArray)entry.getValue();						
				Iterator arrayValue = array.iterator();
				while(arrayValue.hasNext()){
					String memberValue = arrayValue.next().toString();
					model.add(group, model.getProperty(entry.getKey()), memberValue.substring(1,memberValue.length()-1));				
									
				}
			}
			else{
				String value = entry.getValue().toString();
				if(!value.equals(null))						
					model.add(group, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));
			}
					
		}
		maker.close();
		conn.close();
		repository.shutDown();
		
	}
	
	public static boolean deleteGroupGraph(String resourceURI) throws Exception{		
		AGGraph graph = ConnectSingleRepository();			
		AGModel model = new AGModel(graph);
		Resource resource = model.getResource(resourceURI);		
		StmtIterator iterator = model.listStatements();	
		ArrayList<String> anon = new ArrayList<String>();
		boolean exist = false;		
		while(iterator.hasNext()){
			Statement st = iterator.next();					
			if(st.getSubject().toString().equals(resourceURI) || st.getObject().toString().equals(resourceURI)){					
								
				model.remove(st);
				exist = true;
				}			
		}
		
		maker.close();
		conn.close();
		repository.shutDown();
		return exist;
	}
	
}
