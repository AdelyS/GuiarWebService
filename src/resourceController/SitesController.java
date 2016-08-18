package resourceController;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
//import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import persistence.*;
import riotcmd.json;

import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

@Path("/sites")
public class SitesController {	
	String uriBase = "http://localhost:8080/RestWebService/sites/";	
	
	@GET	
	@Path("{siteid}/")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getSite(@PathParam("siteid") String siteid, @HeaderParam("Accept") String accept) throws Exception{			
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		
		String file = SiteGraph.getGraph( uriBase +siteid, format);
		if(file.equals("False"))
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\""+siteid+"."+format+"\" ").build();
	}		
	
	@GET	
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAll(@HeaderParam("Accept") String accept) throws Exception{	
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";		
		String file = SiteGraph.getAll(format);		
		if(file == null)
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"all"+"."+format+"\" ").build();
	}
	
	@POST	
	@Consumes("application/json")	
	public Response postSite(JsonObject json){			
		try {					
			String uri = SiteGraph.setGraph(expandJson(json));			
			return Response.status(201).entity(uri).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
		
	}	
	
	@PUT	
    @Consumes("application/json")	
	@Path("{siteid}/")
    public Response putSite(@PathParam("siteid") String siteid, JsonObject json)  {		
			
		try {			
			SiteGraph.updateGraph(uriBase + siteid,expandJson(json));			
			return Response.status(200).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}
			
	@DELETE
	@Path("{siteid}/")	
	public Response deleteSite(@PathParam("siteid")String siteid) {		
		boolean exist;
		try {
			exist = SiteGraph.deleteResource(uriBase + siteid);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(404).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}	
		
	@GET
	@Path("{siteid}/candidates/")
	@Produces("application/json")
	public Response getCandidates(@PathParam("siteid") String siteid){		
		String description;					  
		String textEncoded;		
		URL url;		
		try {
			description = SiteGraph.getResourceDescription( uriBase + siteid);			
			textEncoded = URLEncoder.encode(description, "utf-8");
			url = new URL("http://spotlight.sztaki.hu:2222/rest/candidates?" +
						"text="+textEncoded+"&confidence=0.35&support=20");			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();			
			connection.setRequestMethod("GET");			
			connection.setRequestProperty("Accept", "application/json");						
			InputStream is = connection.getInputStream();			
			JsonReader jsonReader = Json.createReader(is);			
			JsonObject candidates = jsonReader.readObject();
			jsonReader.close();
			JsonObjectBuilder spotlightresponse = SiteGraph.getCandidatesDescription(candidates);			
			JsonObject response = SiteGraph.getResourceCandidates(uriBase+siteid, spotlightresponse);			
			if(response == null){
				return Response.status(404).build();		
			}
			else					
			return Response.ok(response.toString()).build();
			 
		} catch (Exception e) {				
			e.printStackTrace();
			return Response.status(500).build();
		}
	}

	@PUT	
	@Path("{id}/seeAlso")
	@Consumes("application/json")
	public Response putSeeAlso(@PathParam("id")String id,JsonObject jsonCandidates){
		ArrayList<String> uris = new ArrayList<>();	
		if(jsonCandidates.get("uri").getValueType().toString().equals("STRING")){			 
			uris.add(jsonCandidates.getString("uri").toString());
			
			
		}
		else if(jsonCandidates.get("uri").getValueType().toString().equals("ARRAY")){
			ListIterator array = jsonCandidates.getJsonArray("uri").listIterator();
			while(array.hasNext()){				
				uris.add(array.next().toString());
			}
		}		
		try {
			SiteGraph.setCandidates(uriBase+id, "http://www.w3.org/2000/01/rdf-schema#seeAlso",uris);	
		} catch (Exception e) {				
			e.printStackTrace();
			return Response.status(500).build();
		}		
		return Response.ok().build();
	}
	
	@PUT	
	@Path("{id}/sameAs")
	@Consumes("application/json")
	public Response putSameAs(@PathParam("id")String id,JsonObject jsonCandidates){
		ArrayList<String> uris = new ArrayList<>();	
		if(jsonCandidates.get("uri").getValueType().toString().equals("STRING")){			
			uris.add(jsonCandidates.getString("uri").toString());	
			
		}
		else if(jsonCandidates.get("uri").getValueType().toString().equals("ARRAY")){
			ListIterator array = jsonCandidates.getJsonArray("uri").listIterator();
			while(array.hasNext()){				
				uris.add(array.next().toString());
				
			}
		}		
		
		try {
			SiteGraph.setCandidates(uriBase+id, "http://www.w3.org/2002/07/owl#sameAs",uris);	
		} catch (Exception e) {				
			e.printStackTrace();
			return Response.status(500).build();
		}		
		return Response.ok().build();
	}
	@PUT	
	@Path("{id}/subject")
	@Consumes("application/json")
	public Response putSubject(@PathParam("id")String id,JsonObject jsonCandidates){
		ArrayList<String> uris = new ArrayList<>();	
		if(jsonCandidates.get("uri").getValueType().toString().equals("STRING")){			
			uris.add(jsonCandidates.getString("uri").toString());			
		}
		else if(jsonCandidates.get("uri").getValueType().toString().equals("ARRAY")){
			ListIterator array = jsonCandidates.getJsonArray("uri").listIterator();
			while(array.hasNext()){				
				uris.add(array.next().toString());
			}
		}		
		try {
			SiteGraph.setCandidates(uriBase+id, "http://purl.org/dc/terms/subject",uris);	
		} catch (Exception e) {				
			e.printStackTrace();
			return Response.status(500).build();
		}		
		return Response.ok().build();
	}
	
	@GET
	@Path("{siteid}/shortestpath")
	@Produces("application/json")
	public Response getPath(@PathParam("siteid") String id, @QueryParam("origin") String origin, @QueryParam("destination") String destination){
		JsonObject file =null;
		try {
			file = SitesPath.getShortestPath(uriBase + id, uriBase +origin, uriBase +destination);		
			if(file == null)
				return Response.status(404).build();
			else		
				
				return Response.ok(file.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(404).build();
		}
		
	}
	
	private JsonObject expandJson(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();			
		JsonArrayBuilder expandedArray = factory.createArrayBuilder();
		JsonArrayBuilder hasPartArray = factory.createArrayBuilder();		
		Iterator iterator = obj.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();					
			if(entry.getKey().equals("PhysicalResource") && entry.getValue().toString().equals("true"))
				jsonOB.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://purl.org/dc/terms/PhysicalResource");					
				
			if(entry.getKey().equals("adjacency")){
				JsonArray array = (JsonArray)entry.getValue();
				for (int i = 0; i < array.size(); i++) {
					JsonObject adjacencyObject = (JsonObject)array.get(i);
					Iterator edgeiterator = adjacencyObject.entrySet().iterator();	
					String target = uriBase+adjacencyObject.getString("target");					
					JsonObjectBuilder adjacencyObjectBuider = factory.createObjectBuilder();
					adjacencyObjectBuider.add("http://www.iff.edu.br/ontologies/guiar#distance", adjacencyObject.getString("distance"));
					adjacencyObjectBuider.add("direction", getDirectionPropery(adjacencyObject.getString("direction")));
					adjacencyObjectBuider.add("target", target);						  
					expandedArray.add(adjacencyObjectBuider.build());
				}					
				jsonOB.add("http://www.iff.edu.br/ontologies/guiar#Adjacency", expandedArray.build());
			}
			else if(entry.getKey().equals("hasPart")){
				JsonArray array = (JsonArray)entry.getValue();
				for (int i = 0; i < array.size(); i++) {						
					hasPartArray.add(array.get(i).toString().substring(1,array.get(i).toString().length()-1));								  		
				}		  
				jsonOB.add("http://purl.org/dc/terms/hasPart", hasPartArray.build());	
			}
			
			else
				jsonOB.add("http://purl.org/dc/terms/"+entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
			}				
	
		return jsonOB.build();
	}
	private String getDirectionPropery(String direction){
		if(direction.equalsIgnoreCase("south"))
			return "http://www.iff.edu.br/ontologies/guiar#southLink";
		else if(direction.equalsIgnoreCase("north"))
			return "http://www.iff.edu.br/ontologies/guiar#northLink";
		else if(direction.equalsIgnoreCase("east"))
			return "http://www.iff.edu.br/ontologies/guiar#eastLink";
		else if(direction.equalsIgnoreCase("west"))
			return "http://www.iff.edu.br/ontologies/guiar#westLink";
		else if(direction.equalsIgnoreCase("northwest"))
			return "http://www.iff.edu.br/ontologies/guiar#northwestLink";
		else if(direction.equalsIgnoreCase("northeast"))
			return "http://www.iff.edu.br/ontologies/guiar#northeastLink";
		else if(direction.equalsIgnoreCase("southwest"))
			return "http://www.iff.edu.br/ontologies/guiar#southwestLink";
		else if(direction.equalsIgnoreCase("southeast"))
			return "http://www.iff.edu.br/ontologies/guiar#southeastLink";
		return null;
	}
	
	
	private JsonObject expandJsonCandidates(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
		Iterator iterator = obj.entrySet().iterator();
		
		while(iterator.hasNext()){
			String key = "http://purl.org/dc/terms/subject";
			Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();	
			if(entry.getValue().getValueType().toString().equals("ARRAY")){	
				JsonArray array = (JsonArray)entry.getValue();
				if(array.get(0).getValueType().toString().equals("STRING")){
					for (int i = 0; i < array.size(); i++) {
						jsonOB.add(entry.getKey(), array.getString(i));						
					}							
				}
			}
			if(entry.getKey().equals("seeAlso")){
				key = "http://www.w3.org/2000/01/rdf-schema#seeAlso";
			}
			else if(entry.getKey().equals("sameAs")){
				key = "http://www.w3.org/2002/07/owl#sameAs";
			}			
			jsonOB.add(key, entry.getValue());
		}
		return jsonOB.build();
	}
	
}



