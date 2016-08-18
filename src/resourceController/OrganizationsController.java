package resourceController;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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

import persistence.OrganizationGraph;
import persistence.OrganizationGraph;
@Path("/organizations")
public class OrganizationsController {

String uriBase = "http://localhost:8080/RestWebService/organizations/";	
	
	@GET
	@Path("{id}/")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getOrganization(@PathParam("id") String id, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		
		String file = OrganizationGraph.getGraph(uriBase+ id, format);
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
		
		String file = OrganizationGraph.getAll(format);		
		if(file == null)
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"all"+"."+format+"\" ").build();
	}
	
	@POST
	@Consumes("application/json")	
	public Response postOrganization(JsonObject json){			
		try {					
			String uri = OrganizationGraph.setOrganizationGraph(expandJson(json,"Organization"));			
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
    public Response putOrganization(@PathParam("id") String id, JsonObject json)  {		
		try {				
			String exist = OrganizationGraph.getGraph(uriBase + id,"rdf");
			if(exist.equals("False")){
				 Response.status(404).build();
			}			
		} catch (Exception e1) {
			e1.printStackTrace();
			return Response.status(500).build();
		}		
		try {			
			OrganizationGraph.updateOrganizationGraph(uriBase + id,expandJson(json,"Organization"));			
			return Response.status(200).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}
			
	@DELETE
	@Path("{Organizationuri}/")	
	public Response deleteOrganization(@PathParam("Organizationuri")String id) {		
		boolean exist;
		try {
			exist = OrganizationGraph.deleteOrganizationGraph(uriBase + id);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(401).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}	
	
	@GET
	@Path("{organizationid}/groups/{groupid}")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getGroup(@PathParam("organizationid") String organizationid,@PathParam("groupid") String groupid, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		
		String file = OrganizationGraph.getGraph(uriBase+organizationid +"/groups/"+ groupid, format);
		if(file.equals("False"))
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\""+groupid+"."+format+"\" ").build();
	}		
	
	@POST
    @Consumes("application/json")	
	@Path("{id}/groups")
    public Response postGroup(@PathParam("id") String id, JsonObject json)  {		
		try {				
			String exist = OrganizationGraph.getGraph(uriBase + id,"rdf");
			if(exist.equals("False")){
				 Response.status(404).build();
			}			
		} catch (Exception e1) {
			e1.printStackTrace();
			return Response.status(500).build();
		}		
		try {			
			String uri = OrganizationGraph.setGroupGraph(uriBase + id,expandJson(json,"Group"));			
			return Response.status(200).entity(uri).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}
	@PUT
    @Consumes("application/json")	
	@Path("{id}/groups/{groupid}")
    public Response putGroup(@PathParam("id") String id, @PathParam("groupid") String groupid,JsonObject json)  {		
				
		try {			
			OrganizationGraph.updateGroupGraph(uriBase+id, uriBase + id+"/groups/"+groupid, expandJson(json,"Group"));			
			return Response.status(200).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}
			
	@DELETE
	@Path("{organizationid}/groups/{groupid}")	
	public Response deleteGroup(@PathParam("organizationid")String organizationid, @PathParam("groupid")String groupid) {		
		boolean exist;
		try {
			exist = OrganizationGraph.deleteGroupGraph(uriBase+organizationid+"/groups/"+groupid);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(401).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}	
	
	private JsonObject expandJson(JsonObject obj, String type){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
		if(type.equals("Group")){	
			if(obj.containsKey("member")){
				JsonArray array = obj.getJsonArray("member");
				JsonArrayBuilder expandedArray = factory.createArrayBuilder();				
				for (int i = 0; i < array.size(); i++) {
					String memberobj = array.get(i).toString();				
					expandedArray.add("http://localhost:8080/RestWebService/people/"+array.get(i).toString().substring(1,array.get(i).toString().length()-1));
				}
				jsonOB.add("http://xmlns.com/foaf/0.1/member", expandedArray.build());
			}
			if(obj.containsKey("isPartOf")){
				jsonOB.add("http://purl.org/dc/terms/isPartOf", obj.get("isPartOf").toString().substring(1, obj.get("isPartOf").toString().length()-1));
			}
				
			jsonOB.add("http://xmlns.com/foaf/0.1/title", obj.get("title").toString().substring(1, obj.get("title").toString().length()-1))	;			
						
			
		}
		else{
			Iterator iterator = obj.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();
				if(entry.getKey().equals("isPartOf"))
					jsonOB.add("http://purl.org/dc/terms/isPartOf", entry.getValue().toString().substring(1, entry.getValue().toString().length()-1) );
				else if(entry.getKey().equals("Description") || entry.getKey().equals("created") ||  entry.getKey().equals("related"))
					jsonOB.add("http://purl.org/dc/terms/"+entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1) );
				else 
					jsonOB.add("http://xmlns.com/foaf/0.1/"+entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
			}		
		}		
		return jsonOB.build();
	}
	
}


