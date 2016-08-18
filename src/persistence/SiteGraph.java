package persistence;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sparql.query.SPARQLQuery;
import org.openrdf.sail.federation.Federation;
//import org.openrdf.model.Statement;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Container;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.URIref;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGVirtualRepository;

public class SiteGraph {	

		private static String SERVER_URL = "http://localhost:10035";
		private static String CATALOG_ID = "system";
		private static String REPOSITORY_ID = "data";
		private static String USERNAME = "adely";
		private static String PASSWORD = "adely";		
		private static AGRepository repository;
		private static AGRepositoryConnection conn;
		private static AGGraphMaker maker;
		private static AGAbstractRepository combinedRepo;
		
		 
		
		private static AGGraph ConnectSingleRepository()
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
		
		public static String getGraph(String siteUri, String extensao) throws Exception{			
			 ConnectCombinedRepository();
			 System.out.println("\nStarting conection().");
			Model fakeModel = ModelFactory.createDefaultModel();
			fakeModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
			fakeModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
			fakeModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
			fakeModel.setNsPrefix("guiar", "http://www.iff.edu.br/ontologies/guiar#");			
			boolean exist = false;			
			String queryString;
			queryString = "Describe <"+siteUri+"> ?s ?p ?o ";			  
			GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
			describeQuery.setIncludeInferred(true);
			GraphQueryResult  result = describeQuery.evaluate();
			
			 while(result.hasNext()){
				 org.openrdf.model.Statement solution = result.next();						
					Resource r ;
					Resource o;
					Property p = fakeModel.createProperty(solution.getPredicate().stringValue());					
					if(solution.getSubject().toString().contains("_:")){
							AnonId id = new AnonId(solution.getSubject().stringValue());
							 r = fakeModel.createResource(id);
							 fakeModel.add(r, p, solution.getObject().stringValue());
					}
					else{	
						r = fakeModel.createResource(solution.getSubject().stringValue());
						if(solution.getObject().toString().contains("_:")){
							AnonId id = new AnonId(solution.getSubject().stringValue());
							 o = fakeModel.createResource(id);							 
							 fakeModel.add(r, p, o);
						}
						else
							 fakeModel.add(r, p, solution.getObject().stringValue());
						 
					}
					exist = true;
				 
			 }						
			conn.close();
			combinedRepo.close();			
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
				fakeModel.setNsPrefix("guiar", "http://www.iff.edu.br/ontologies/guiar#");
				boolean exist = false;				
				String queryString = "prefix dcterms:<http://purl.org/dc/terms/>"
						+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
						+ "Select ?s ?p ?o"						
						+ " WHERE { ?s rdf:type dcterms:Location. "
						+ "?s ?p ?o.filter regex(str(?s),\"RestWebService/sites/\",\"i\")"
						+ "optional {?o ?p2 ?o2. filter(isBlank(?o)) }}";	
				TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				   tupleQuery.setIncludeInferred(true);
				   TupleQueryResult result = tupleQuery.evaluate();
				   
				while (result.hasNext()) {   	
	        	exist = true;
	        	BindingSet solution= result.next();	        	
	        	Resource r = fakeModel.createResource(solution.getValue("s").stringValue());	        
	        	Property p = fakeModel.createProperty(solution.getValue("p").stringValue());
	        	String o = solution.getValue("o").toString();
	        	fakeModel.add(r,p,o);
	        	
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
		
		public static String setGraph(JsonObject json) throws Exception{			
			AGGraph graph = ConnectSingleRepository();						
			AGModel model = new AGModel(graph);				
			Iterator subjects= model.listSubjects();			
			int newid = 0;
			int newanonid= 0;			
			while(subjects.hasNext()){								
				Resource r = (Resource) subjects.next();			
				if((r.toString().contains("http://localhost:8080/RestWebService/sites/"))&& !r.toString().contains("thesaurus")){/*Verify if it's the ontology statement*/
					int lastid = Integer.parseInt(r.toString().substring(43));					
					if(lastid>newid)
							newid = lastid;
				}				
			}
			
			Resource resource =	model.createResource("http://localhost:8080/RestWebService/sites/" + (newid+1));
			Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
			Resource resourceLocation =	model.createResource("http://purl.org/dc/terms/Location" );
			model.add(resource, type, resourceLocation);
			Iterator iterator = json.entrySet().iterator();			
				while (iterator.hasNext()) {
					Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();						
					if(entry.getValue().getValueType().toString().equals("ARRAY")){	
						JsonArray array= (JsonArray)entry.getValue();
						if(array.get(0).getValueType().toString().equals("STRING")){
							for (int i = 0; i < array.size(); i++) {								
								model.add(resource, model.getProperty(entry.getKey()),model.createResource(array.getString(i)));
							}							
						}
						else{							
							Iterator arrayValue = array.iterator();								
							while(arrayValue.hasNext()){
								Resource adjacency = model.createResource();								
								model.add(adjacency, model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), entry.getKey());
								JsonObject objectValue= (JsonObject)arrayValue.next();
								String distance = objectValue.getString("http://www.iff.edu.br/ontologies/guiar#distance");
								Resource target = model.getResource(objectValue.getString("target"));
								Property directionProperty = model.getProperty(objectValue.getString("direction"));
								model.add(adjacency,model.getProperty("http://www.iff.edu.br/ontologies/guiar#distance"),distance);
								model.add(adjacency,directionProperty,target);
								model.add(resource,directionProperty,adjacency);													
							}				
						
						}
					}
					else{						
						String value = entry.getValue().toString();							
						if(!value.equals(null)){
							Property property = model.getProperty(entry.getKey());
							if(entry.getKey().contains("type")){
								Resource typeTesauro = model.getResource(value.substring(1,value.length()-1));
								model.add(resource, model.getProperty(entry.getKey()), typeTesauro);
							}
							else if(entry.getKey().contains("PhysicalResource"))
								model.add(resource,model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") ,entry.getKey());
							else 
								model.add(resource, property, value.substring(1,value.length()-1));
							
						}	
					}
				}				
				maker.close();
				conn.close();
				repository.shutDown();
				return resource.getURI();
		} 
		
		public static void updateGraph(String uri, JsonObject json) throws Exception{			
			AGGraph graph = ConnectSingleRepository();			
			AGModel model = new AGModel(graph);			
			Model addModel = ModelFactory.createDefaultModel();			
			Resource resource =	addModel.createResource(uri);			
			
			StmtIterator result = model.listStatements();	
			boolean hasOrigin = false;
			while (result.hasNext()) {				
				Statement st = result.next();	
				if(!st.getSubject().isAnon()){
					if(st.getSubject().getURI().equals(uri)){						
						if(st.getObject().isAnon())		
							
							model.remove(model.listStatements((Resource)st.getObject(), (Property)null, (RDFNode)null));					
					}
				}else if(st.getSubject().isAnon() && st.getObject().toString().equals(uri)){
					model.remove(model.listStatements((Resource)st.getSubject(), (Property)null, (RDFNode)null));
					model.remove(model.listStatements((Resource)null, (Property)null, (Resource)st.getSubject()));
				}
			}			
			model.removeAll((Resource)resource, (Property)null, (RDFNode)null);				
			Property type = addModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
			Resource resourceLocation =	addModel.createResource("http://purl.org/dc/terms/Location" );
			addModel.add(resource, type, resourceLocation);
			Iterator iterator = json.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();						
				if(entry.getValue().getValueType().toString().equals("ARRAY")){	
					JsonArray array= (JsonArray)entry.getValue();
					if(array.get(0).getValueType().toString().equals("STRING")){
						for (int i = 0; i < array.size(); i++) {							
							model.add(resource, model.getProperty(entry.getKey()),model.createResource(array.getString(i)));
						}	
					}
				
					else{							
						
						Iterator arrayValue = array.iterator();						
						while(arrayValue.hasNext()){	
							Resource adjacency = model.createResource();
								model.add(adjacency, model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), entry.getKey());
								JsonObject objectValue= (JsonObject)arrayValue.next();
								String distance = objectValue.getString("http://www.iff.edu.br/ontologies/guiar#distance");
								Resource target = model.getResource(objectValue.getString("target"));
								Property directionProperty = model.getProperty(objectValue.getString("direction"));
								model.add(adjacency,model.getProperty("http://www.iff.edu.br/ontologies/guiar#distance"),distance);
								model.add(adjacency,directionProperty,target);
								model.add(resource,directionProperty,adjacency);													
							}			
							
						}
					
				}
				else{						
					String value = entry.getValue().toString();	
					if(!value.equals(null)){
						Property property = model.getProperty(entry.getKey());
						if(entry.getKey().contains("type")){
							Resource typeTesauro = model.getResource(value.substring(1,value.length()-1));
							model.add(resource, model.getProperty(entry.getKey()), typeTesauro);
						}
						else if(entry.getKey().contains("PhysicalResource"))
							model.add(resource,model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") ,entry.getKey());
						else 
							model.add(resource, property, value.substring(1,value.length()-1));
						
					}	
				}					
			}
			
			
					
			model.add(addModel);
			maker.close();
			conn.close();
			repository.shutDown();						
		}		
		
		public static boolean deleteResource(String resourceURI) throws Exception{	
			
			 ConnectCombinedRepository();
			 boolean exist = false;
			
			 String queryString = "prefix dcterms:<http://purl.org/dc/terms/>"
			 		+ "prefix guiar:<http://www.iff.edu.br/ontologies/guiar#>"
						+ "SELECT ?blank ?part "						
						+ " WHERE { <"+resourceURI+"> ?p ?part. FILTER( ?p = guiar:hasPartTransitive && regex(str(?part), \"sites\", \"i\"))"
						+ " ?part guiar:link ?blank. FILTER(isBlank(?blank))}";			 
			 
				TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);				
				tupleQuery.setIncludeInferred(true);
				TupleQueryResult result = tupleQuery.evaluate();				
				AGGraph graph = ConnectSingleRepository();			
				AGModel model = new AGModel(graph);
				
				ArrayList<String> subjects = new ArrayList<String>();
				subjects.add(resourceURI);
				while (result.hasNext()) {					
		        	exist =true;
		        	BindingSet solution= result.next();	
		        	Iterator solutionIterator = solution.iterator();
		        	while(solutionIterator.hasNext()){
		        		
		        		String add = solutionIterator.next().toString();
		        		add = add.substring(add.indexOf("=")+1, add.length());
		        		if(add.contains("_:"))
		        			add = add.substring(2);
		        		if(!subjects.contains(add))
		        			subjects.add(add);
		        	}
		        	
		        	}
		        	
				
				Iterator iterator = model.listStatements();
	        	while(iterator.hasNext()){
	        		Statement st = (Statement) iterator.next();		        		
	        			if(subjects.contains(st.getSubject().toString()) || subjects.contains(st.getObject().toString())){
	        					model.remove(st);	        					
	        			}	        		
				}
				return exist;
			
		}
		
		public static String getTesaurus(String uri, String format) throws Exception{
			
			 ConnectCombinedRepository();
				Model tesaurusModel = ModelFactory.createDefaultModel();
				tesaurusModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
				tesaurusModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
				tesaurusModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
				tesaurusModel.setNsPrefix("guiar", "http://www.iff.edu.br/ontologies/guiar#");
				boolean exist = false;				
				String queryString;
				queryString = "Describe <http://localhost:8080/RestWebService/thesaurus/"+uri+"> ?s ?p ?o ";
				GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
				describeQuery.setIncludeInferred(true);
				 GraphQueryResult  result = describeQuery.evaluate();
				 while(result.hasNext()){
					 org.openrdf.model.Statement solution = result.next();				 
						Resource r ;
						Resource o;
						Property p = tesaurusModel.createProperty(solution.getPredicate().stringValue());					
						if(solution.getSubject().toString().contains("_:")){
								AnonId id = new AnonId(solution.getSubject().stringValue());
								 r = tesaurusModel.createResource(id);
								 tesaurusModel.add(r, p, solution.getObject().stringValue());
						}
						else{	
							r = tesaurusModel.createResource(solution.getSubject().stringValue());
							if(solution.getObject().toString().contains("_:")){
								AnonId id = new AnonId(solution.getSubject().stringValue());
								 o = tesaurusModel.createResource(id);							 
								 tesaurusModel.add(r, p, o);
							}
							else
								tesaurusModel.add(r, p, solution.getObject().stringValue());
							 
						}
						exist = true;
					 
				 }	
				 conn.close();
				combinedRepo.shutDown();
				 if(exist){
						OutputStream stream = new ByteArrayOutputStream() ;					
						if(format.equals("rdf"))						
							tesaurusModel.write(stream, "RDF/XML-ABBREV");				
						if(format.equals("ttl")){				
							tesaurusModel.write(stream, "TURTLE");
						}				
						return stream.toString();
					}
					else
						return "False";			
			
			
			
		}	
			
		public static String getAllTesaurus(String format) throws Exception{
			 ConnectCombinedRepository();
				Model fakeModel = ModelFactory.createDefaultModel();
				fakeModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
				fakeModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
				fakeModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
				fakeModel.setNsPrefix("guiar", "http://www.iff.edu.br/ontologies/guiar#");
				boolean exist = false;			
				String queryString = "prefix this:<http://localhost:8080/RestWebService/thesaurus>"
						+ "prefix skos:<http://www.w3.org/2004/02/skos/core#>"
						+ "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
						+ "Select ?s ?p ?o  "						
						+ " WHERE { ?s ?p ?o.filter regex(str(?s), \"tesaurus\", \"i\")}";
				TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				   tupleQuery.setIncludeInferred(true);
				   TupleQueryResult result = tupleQuery.evaluate();
				while (result.hasNext()) {   	
					exist = true;
	        	BindingSet solution= result.next();
	        	Resource r = fakeModel.createResource(solution.getValue("s").stringValue());
	        	Property p = fakeModel.createProperty(solution.getValue("p").stringValue());
	        	String o = solution.getValue("o").stringValue();
	        	fakeModel.add(r,p,o);
	        	
	        	
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
				
		public static String setTesaurus(JsonObject json) throws Exception{

			AGGraph graph = ConnectSingleRepository();						
			AGModel model = new AGModel(graph);			
			
			Iterator listStatment = model.listStatements();
			int newid = 0;
			while(listStatment.hasNext()){				
				Statement st = (Statement)listStatment.next();				
				if(st.getSubject().toString().contains("thesaurus")){					
					int lastid = Integer.parseInt(st.getSubject().toString().substring(47));					
					if(lastid > newid)
						newid = lastid;					
				}				
			}
			Resource resource =	model.createResource("http://localhost:8080/RestWebService/thesaurus/" + (newid+1));
			Property type = model.getProperty(model.getNsPrefixURI("rdf")+"type");
			Resource conceptResource = model.createResource("http://www.w3.org/2004/02/skos/core#Concept");
			model.add(resource, type, conceptResource);
			Iterator iterator = json.entrySet().iterator();							
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();			
				String value = entry.getValue().toString();
					if(!value.equals("null")){
						Property property = model.getProperty(entry.getKey());
						model.add(resource, property, value.substring(1,value.length()-1));
					}					
				}
				maker.close();
				conn.close();
				repository.shutDown();	
				return resource.getURI();
		}
		
		public static void updateTesaurus(String uri, JsonObject json) throws Exception{
			AGGraph marker = ConnectSingleRepository();
			AGModel model = new AGModel(marker);
			Iterator iterator = model.listStatements();			
			while(iterator.hasNext()){
				Statement st = (Statement)iterator.next();
				if(st.getSubject().toString().equals(uri))
					model.remove(st);
			}
			Resource resource =	model.createResource(uri);
			Property type = model.getProperty(model.getNsPrefixURI("rdf")+"type");
			Resource conceptResource = model.createResource("http://www.w3.org/2004/02/skos/core#Concept");
			model.add(resource, type, conceptResource);
			Iterator jsonIterator = json.entrySet().iterator();							
			while(jsonIterator.hasNext()){
				Entry<String, JsonValue> entry= (Entry<String, JsonValue>)jsonIterator.next();				
				String value = entry.getValue().toString();
					if(!value.equals("null")){						
						Property property = model.getProperty(entry.getKey());
						model.add(resource, property, value.substring(1,value.length()-1));
					}					
				}
				maker.close();
				conn.close();
				repository.shutDown();	
		}
		
		public static boolean deleteTesaurus(String resourceURI) throws Exception{		
			AGGraph graph = ConnectSingleRepository();			
			AGModel model = new AGModel(graph);				
			StmtIterator iterator = model.listStatements();				
			boolean exist = false; // verifica se o recurso existe			
			while(iterator.hasNext()){
				Statement st = iterator.next();	
				if(st.getSubject().toString().equals(resourceURI) || st.getObject().toString().equals(resourceURI)){
					exist = true;					
					model.remove(st);					
				}		
			}
			maker.close();
			conn.close();
			repository.shutDown();
			return exist;
		}
		
		public static String getResourceDescription(String uri) throws Exception{		
			AGGraph graph = ConnectSingleRepository();
			AGModel agModel = new AGModel(graph);
			AGReasoner reasoner = new AGReasoner();
			InfModel model = new AGInfModel(reasoner, agModel);
			Resource resource = model.getResource(uri);
			Property propertyDescription = model.getProperty("http://purl.org/dc/terms/description");			
			StmtIterator st = model.listStatements(resource, propertyDescription, (Literal)null);			
			String description="";
			while(st.hasNext()){
				description += 	st.next().getObject().asLiteral().toString();
			}			
			maker.close();
			conn.close();
			repository.shutDown();
			return description;
		}
		
		public static JsonObjectBuilder getCandidatesDescription(JsonObject json) throws Exception{			
			Model model = ModelFactory.createDefaultModel();
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			JsonObject result2 = null;
			JsonObjectBuilder jsonOB = factory.createObjectBuilder();			
			String queryString = null;
			JsonObjectBuilder labelOB = factory.createObjectBuilder();
			boolean hasDescription = false;
			
			ArrayList uris = new ArrayList<String>();			
			if(!json.getJsonObject("annotation").containsKey("surfaceForm")){
				return jsonOB;
			}
			if(json.getJsonObject("annotation").get("surfaceForm").getValueType().toString().equals("OBJECT")){
				queryString = "PREFIX dc:<http://purl.org/dc/elements/1.1/> \n" 
			            +"PREFIX dbpedia:<http://dbpedia.org/resource/> "
			            + "PREFIX dbproperty:<http://dbpedia.org/property/>\n"
			            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT ?s ?description "
			            +" WHERE { "
						+ "\n ?s ?d ?description. "
						+ "filter((?s = dbpedia:"+json.getJsonObject("annotation").getJsonObject("surfaceForm").getJsonObject("resource").getString("@uri")+") "
								+ "&& (?d= dc:abstract || ?d= dbproperty:description) )}  ";
													
				labelOB.add("http://dbpedia.org/resource/"+json.getJsonObject("annotation").getJsonObject("surfaceForm").getJsonObject("resource").getString("@uri"), json.getJsonObject("annotation").getJsonObject("surfaceForm").getJsonObject("resource").getString("@label"));									
				
			}
			else{
				JsonArray array = json.getJsonObject("annotation").getJsonArray("surfaceForm"); 
				labelOB.add("http://dbpedia.org/resource/"+array.getJsonObject(0).getJsonObject("resource").getString("@uri"), array.getJsonObject(0).getJsonObject("resource").getString("@label"));
				queryString = "PREFIX dc:<http://purl.org/dc/elements/1.1/> \n" 
				            +"PREFIX dbpedia:<http://dbpedia.org/resource/> "
				            + "PREFIX dbproperty:<http://dbpedia.org/property/>\n"
				            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
						+ "SELECT ?s ?description"
				          +  " WHERE { "
						+ "\n { ?s ?d ?description."
								+ " filter((?s = dbpedia:"+array.getJsonObject(0).getJsonObject("resource").getString("@uri")+") "
															
															+ "&& (?d= dc:description || ?d= dbproperty:description) )}  ";
				
				for (int i = array.size()-1; i > 0; i--) {
					labelOB.add("http://dbpedia.org/resource/"+array.getJsonObject(i).getJsonObject("resource").getString("@uri"), array.getJsonObject(i).getJsonObject("resource").getString("@label"));
					queryString += "\n UNION { ?s ?d ?description."												
							+ " filter(?s = dbpedia:"+array.getJsonObject(i).getJsonObject("resource").getString("@uri")
					     	+ "&& (?d= dc:description || ?d= dbproperty:description) )}  ";
				}
				queryString += "}";
			}
				JsonObject labels = labelOB.build();
				Query query = QueryFactory.create(queryString);
				QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);				 
				ResultSet results = qexec.execSelect();
				while(results.hasNext()){
					hasDescription = true;
					QuerySolution solution = results.next();					
					String uri = solution.get("s").toString();					
					String label = labels.get(uri).toString();					
					jsonOB.add(uri,factory.createArrayBuilder()
							.add(factory.createObjectBuilder().add("description",solution.get("description").toString()))
							.add(factory.createObjectBuilder().add("label",label)));
					
				}
				
			    if(!hasDescription){
			    	return labelOB;
			    }
			    	
				 
			
			return jsonOB;
		}
		
		public static void setCandidates(String localUri, String propertyUri, ArrayList<String> candidatesUri) throws Exception{
			AGGraph graph = ConnectSingleRepository();			
			AGModel model = new AGModel(graph);
			Resource resource = model.getResource(localUri);
			Property property = model.getProperty(propertyUri);	
			for (int i = 0; i < candidatesUri.size(); i++) {						
				model.add(resource, property, model.createResource(candidatesUri.get(i)));
			}
			maker.close();
			conn.close();
			repository.shutDown();
		}
		
		public static JsonObject getResourceCandidates(String uri, JsonObjectBuilder jsonOB) throws Exception{
			
			ConnectCombinedRepository();			
			
			try {
				String dbpediaQuery;
	            String queryString = "PREFIX dcterms:<http://purl.org/dc/terms/>"
	            		+ "PREFIX site:<http://localhost:8080/RestWebService/sites/>"
	            		+ " SELECT ?title  WHERE { <"+uri+"> dcterms:title ?title. }";		            
	            AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	            tupleQuery.setIncludeInferred(true);
	            TupleQueryResult result = tupleQuery.evaluate();
	            
	            try {           	
	            	
	               while (result.hasNext()) {	                	
	                    BindingSet bindingSet = result.next();
	                    String title = bindingSet.getValue("title").stringValue();
	                    
	                    dbpediaQuery = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
	                    			+ "PREFIX dc:<http://purl.org/dc/elements/1.1/>"
	                    			+"SELECT ?candidate ?label ?abstract"
	                    			+ " WHERE{?candidate rdfs:label ?label. FILTER regex(str(?label),\""+title+"\",\"i\"). "
	                    			+ "optional{?candidate dc:abstract ?abstract.}}";
	                    
	                    Query query = QueryFactory.create( dbpediaQuery);
	    				QueryExecution qexec = QueryExecutionFactory.sparqlService("http://pt.dbpedia.org/sparql", query);	    				 
	    				ResultSet results = qexec.execSelect();		
	    				
	    				while(results.hasNext()){		    					
	    					QuerySolution solution = results.next();   
	    					JsonBuilderFactory factory = Json.createBuilderFactory(null);	    					
	    					JsonObjectBuilder labelOB = factory.createObjectBuilder();		
	    					String s = solution.get("candidate").toString();
	    					String candidateUri = new String(s.getBytes("ISO-8859-15"), "UTF-8");
	    					String label = solution.get("label").toString();
	    					labelOB.add("label", label);
	    					String description = " ";	    					
	    					if(solution.contains(description)){
	    						description = solution.get("abstract").toString();  
	    						labelOB.add("abstract", description);
	    					}
	    					jsonOB.add(candidateUri, labelOB.build());
	    					
	    				}
	                }
	            } finally {
	                result.close();
	            }
	        } finally {
	            conn.close();
	        }
			
			return jsonOB.build();
		}

		public static JsonObject getTesaurusCandidates(String uri, JsonObjectBuilder jsonOB) throws Exception{
			
			ConnectCombinedRepository();			
			try {
				String dbpediaQuery;
	            String queryString = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
	            		+ "PREFIX site:<http://localhost:8080/RestWebService/thesaurus/>"
	            		+ " SELECT ?o  WHERE { site:"+uri+" rdfs:label ?o. }";	           
	            AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	            tupleQuery.setIncludeInferred(true);
	            TupleQueryResult result = tupleQuery.evaluate();
	            
	            try {           	
	            	
	               while (result.hasNext()) {	                	
	                    BindingSet bindingSet = result.next();	                    
	                    Value o = bindingSet.getValue("o");
	                    
	                    dbpediaQuery = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
	                    		+ "PREFIX dc:<http://purl.org/dc/elements/1.1/>"
	                    		+"SELECT ?candidate ?label ?abstract"
	                    		+ " WHERE{?candidate rdfs:label ?label."
	                    		+ "FILTER regex(str(?label),"+o.toString()+",\"i\"). "
	                    		+ "optional{?candidate dc:abstract ?abstract.}}";
	                   
	                    Query query = QueryFactory.create( dbpediaQuery);
	    				QueryExecution qexec = QueryExecutionFactory.sparqlService("http://pt.dbpedia.org/sparql", query);	    				 
	    				ResultSet results = qexec.execSelect();
	    				
	    				
	    				while(results.hasNext()){		    					
	    					QuerySolution solution = results.next();   
	    					JsonBuilderFactory factory = Json.createBuilderFactory(null);	    					
	    					JsonObjectBuilder labelOB = factory.createObjectBuilder();		
	    					String s = solution.get("candidate").toString();
	    					String candidateUri = new String(s.getBytes("ISO-8859-15"), "UTF-8");
	    					String label = solution.get("label").toString();
	    					labelOB.add("label", label);
	    					String description = " ";	    					
	    					if(solution.contains("abstract")){
	    						description = solution.get("abstract").toString();  
	    						labelOB.add("abstract", description);
	    					}
	    					jsonOB.add(candidateUri, labelOB.build());
	    					
	    					
	    				}
	                }
	            } finally {
	                result.close();
	            }
	        } finally {
	            conn.close();
	        }			
			return jsonOB.build();
		}
		
		public static String getTesaurusDefinition(String uri) throws Exception{		
			AGGraph graph = ConnectSingleRepository();
			AGModel agModel = new AGModel(graph);
			AGReasoner reasoner = new AGReasoner();
			InfModel model = new AGInfModel(reasoner, agModel);
			Resource resource = model.getResource(uri);
			Property propertyDefinition = model.getProperty("http://www.w3.org/2004/02/skos/core#definition");			
			StmtIterator st = model.listStatements(resource, propertyDefinition, (Literal)null);			
			String definition="";
			while(st.hasNext()){
				definition += 	st.next().getObject().asLiteral().toString();
			}			
			maker.close();
			conn.close();
			repository.shutDown();
			return definition;
		}
		
		public static void putTesaurusCandidates(String id, JsonObject json) throws Exception{
			AGGraph graph = ConnectSingleRepository();			
			AGModel model = new AGModel(graph);
			Resource resource = model.getResource("http://localhost:8080/RestWebService/thesaurus/"+id);
			Property property = model.getProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso");
			Iterator iterator = json.entrySet().iterator();					
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry= (Entry<String, JsonValue>)iterator.next();								
				model.add(resource, property, model.createResource(entry.getKey()));
			}
			maker.close();
			conn.close();
			repository.shutDown();
		}
		
				
}


