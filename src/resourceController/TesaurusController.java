package resourceController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
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

import persistence.SiteGraph;

@Path("/thesaurus")
public class TesaurusController {
	String uriBase = "http://localhost:8080/RestWebService/thesaurus/";
	

	@GET
	@Path("{id}")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getTesaurus(@PathParam("id")String id, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = SiteGraph.getTesaurus(id, format);
		if(file.equals("False"))
				return Response.status(404).build();
		else
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"tesaurus_"+id+"."+format+"\" ").build();
	}
	
	@GET	
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAll(@HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";		
		String file = SiteGraph.getAllTesaurus(format);	
		if(file == null)
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"tesaurus_all"+"."+format+"\" ").build();
	}
	
	@POST
	@Consumes("application/json")
	public Response postTesaurus(JsonObject json){
		try {					
			String uri = SiteGraph.setTesaurus(expandJson(json));			
			return Response.status(200).entity(uri).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}	
	}
	
	@PUT
	@Path("{id}")
	@Consumes("application/json")
	public Response putTesaurus(@PathParam("id")String id, JsonObject json){
		try {					
			SiteGraph.updateTesaurus(uriBase+id, expandJson(json));			
			return Response.status(200).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}	
	}
	
	@DELETE
	@Path("{uri}/")	
	public Response deleteTesaurus(@PathParam("uri")String id) {		
		boolean exist;
		try {
			exist = SiteGraph.deleteTesaurus(uriBase + id);
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
	@Path("{id}/candidates/")
	@Produces("application/json")
	public Response getTesaurusCandidates(@PathParam("id") String id){		
		String definition;					  
		String textEncoded;		
		URL url;		
		try {
			definition = SiteGraph.getTesaurusDefinition( uriBase + id);			
			textEncoded = URLEncoder.encode(definition, "utf-8");
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
			JsonObject response = SiteGraph.getTesaurusCandidates(id, spotlightresponse);			
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
	
	
	private JsonObject expandJson(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();	
		Iterator iterator = obj.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();
			jsonOB.add("http://www.w3.org/2004/02/skos/core#"+entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
		}	
		return jsonOB.build();
	}
	
	private JsonObject expandJsonCandidates(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
		Iterator iterator = obj.entrySet().iterator();
		
		while(iterator.hasNext()){
			String key = "http://purl.org/dc/terms/subject";
			Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();	
			if(entry.getKey().equals("seeAlso")){
				key = "http://www.w3.org/2000/01/rdf-schema#seeAlso";
			}
			else if(entry.getKey().equals("sameAs")){
				key = "http://www.w3.org/2002/07/owl#sameAs";
			}			
			jsonOB.add(key, entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
		}
		return jsonOB.build();
	}
}
