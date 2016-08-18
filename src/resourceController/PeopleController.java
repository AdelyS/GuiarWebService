package resourceController;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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

import persistence.PersonGraph;
@Path("/people")
public class PeopleController {

	String uriBase = "http://localhost:8080/RestWebService/people/";
	@GET
	@Path("{id}/")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getPerson(@PathParam("id") String id, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = PersonGraph.getGraph(uriBase+ id, format);
		if(file.equals("False"))
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\""+id+"."+format+"\" ").build();
	}		
	
	@GET	
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAll(@HeaderParam("Accept") String accept) throws Exception{		
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = PersonGraph.getAll(format);		
		if(file == null)
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"all"+"."+format+"\" ").build();
	}
	
	@POST
	@Consumes("application/json")	
	public Response postPerson(JsonObject json){			
		try {					
			String uri = PersonGraph.setPersonGraph(expandJson(json));			
			return Response.status(201).entity(uri).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
		
	}	
	
	@PUT
    @Consumes("application/json")	
	@Path("{id}/")
    public Response putPerson(@PathParam("id") String id, JsonObject json)  {		
		try {				
			String exist = PersonGraph.getGraph(uriBase + id,"rdf");
			if(exist.equals("False")){
				 Response.status(404).build();
			}			
		} catch (Exception e1) {
			e1.printStackTrace();
			return Response.status(500).build();
		}		
		try {			
			PersonGraph.updatePersonGraph(uriBase + id,expandJson(json));			
			return Response.status(200).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}
			
	@DELETE
	@Path("{Personuri}/")	
	public Response deletePerson(@PathParam("Personuri")String id) {		
		boolean exist;
		try {
			exist = PersonGraph.deletePersonGraph(uriBase + id);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(404).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}	
	
	private JsonObject expandJson(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
	
			Iterator iterator = obj.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();
				jsonOB.add("http://xmlns.com/foaf/0.1/"+entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
			}		
		
		
		return jsonOB.build();
	}
	
	
}
