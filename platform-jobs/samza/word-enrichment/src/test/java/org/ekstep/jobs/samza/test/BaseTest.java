package org.ekstep.jobs.samza.test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.ekstep.language.util.ControllerUtil;
import org.junit.Assert;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import com.ilimi.common.Platform;
import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.logger.PlatformLogger;
import com.ilimi.graph.common.enums.GraphEngineParams;
import com.ilimi.graph.common.enums.GraphHeaderParams;
import com.ilimi.graph.dac.enums.GraphDACParams;
import com.ilimi.graph.dac.model.Node;
import com.ilimi.graph.engine.router.GraphEngineManagers;


abstract public class BaseTest {

	protected static ControllerUtil util = new ControllerUtil();
	private static GraphDatabaseService graphDb;
	private static ObjectMapper mapper = new ObjectMapper();
	protected static String languageId = "en";
	protected static String ka_languageId = "ka";
	protected static String languageCommonId = "testLanguage";
	
	protected static void before(){
		GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector( "0" );
        System.out.println("Starting neo4j in embedded mode");
       
        graphDb = new GraphDatabaseFactory()
		        .newEmbeddedDatabaseBuilder(new File(Platform.config.getString("graph.dir")))
		        .setConfig( bolt.type, "BOLT" )
		        .setConfig( bolt.enabled, "true" )
		        .setConfig( bolt.address, "localhost:7687" )
		        .newGraphDatabase();
		registerShutdownHook(graphDb);
		
		try(Transaction tx = graphDb.beginTx()){
			System.out.println("Loading All Definitions...!!");
			loadAllDefinitions(new File("src/test/resources/definitions"), languageId);
			loadAllDefinitions(new File("src/test/resources/definitions"), languageCommonId);
			loadAllDefinitions(new File("src/test/resources/definitions"), ka_languageId);
		}
	}
	
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	private static void loadAllDefinitions(File folder, String graphId) {
		for (File fileEntry : folder.listFiles()) {
			if (!fileEntry.isDirectory()) {
				String definition;
				try {
					definition = FileUtils.readFileToString(fileEntry);
					createDefinition(definition, graphId);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static void createDefinition(String contentString, String graph_id) throws IOException{
		
		Request request = new Request();
		request.setManagerName(GraphEngineManagers.NODE_MANAGER);
		request.setOperation("importDefinitions");
		request.getContext().put(GraphHeaderParams.graph_id.name(),
				graph_id);
		request.put(GraphEngineParams.input_stream.name(), contentString);
		PlatformLogger.log("List | Request: " , request);
		Response response = util.getResponse(
				request);
		PlatformLogger.log("List | Response: " ,response);
		
		Assert.assertEquals("successful", response.getParams().getStatus());
	}
	
	protected static String createWord(String lemma) throws Exception{
		return createWord(lemma, languageId);
	}
	
	protected static String createWord(String lemma, String graphId) throws Exception{
		String synsetRequest = "{\"nodeType\":\"DATA_NODE\",\"objectType\":\"Synset\",\"metadata\":{\"gloss\":\""+lemma+"\",\"category\":\"Place\"}}";
		Object synsetNodeObj = mapper.readValue(synsetRequest, Class.forName("com.ilimi.graph.dac.model.Node"));		
		String synsetId = createNode(synsetNodeObj, graphId);
		String wordRequest = "{\"nodeType\":\"DATA_NODE\",\"objectType\":\"Word\",\"metadata\":{\"lemma\":\""+lemma+"\",\"primaryMeaningId\":\""+synsetId+"\"},\"inRelations\": [{\"endNodeId\":null, \"relationType\":\"synonym\",\"startNodeId\":\""+synsetId+"\"}]}";
		Object wordNodeObj = mapper.readValue(wordRequest, Class.forName("com.ilimi.graph.dac.model.Node"));		
		return createNode(wordNodeObj, graphId);
	}

	protected static Node getWord(String wordId, String graphId) throws Exception{
		
		Request request = new Request();
		request.setManagerName(GraphEngineManagers.SEARCH_MANAGER);
		request.setOperation("getDataNode");
		request.getContext().put(GraphHeaderParams.graph_id.name(),
				graphId);
		request.put(GraphDACParams.node_id.name(), wordId);
		PlatformLogger.log("List | Request: " , request);
		Response response = util.getResponse(
				request);
		PlatformLogger.log("List | Response: " ,response);
		
		Assert.assertEquals("successful", response.getParams().getStatus());
		return (Node) response.get(GraphDACParams.node.name());
		
	}
	
	
	protected static String createNode(Object node, String graphId) throws Exception{
		Request request = new Request();
		request.setManagerName(GraphEngineManagers.NODE_MANAGER);
		request.setOperation("createDataNode");
		request.getContext().put(GraphHeaderParams.graph_id.name(),
				graphId);
		request.put(GraphDACParams.node.name(), node);
		Response response = util.getResponse(
				request);
		Assert.assertEquals("successful", response.getParams().getStatus());
		return response.getResult().get(GraphDACParams.node_id.name()).toString();
	}
	
	protected static String updateNode(String identifier, Object node, String graphId) throws Exception{
		Request request = new Request();
		request.setManagerName(GraphEngineManagers.NODE_MANAGER);
		request.setOperation("updateDataNode");
		request.getContext().put(GraphHeaderParams.graph_id.name(),
				graphId);
		request.put(GraphDACParams.node.name(), node);
		request.put(GraphDACParams.node_id.name(), identifier);
		Response response = util.getResponse(
				request);
		Assert.assertEquals("successful", response.getParams().getStatus());
		return response.getResult().get(GraphDACParams.node_id.name()).toString();
	}
	
	protected static String createNode(Object node) throws Exception{
		return createNode(node, languageId);
	}
	
	protected static void createRelation(String graphId, String startId, String relationType, String endId, Map<String, Object> metadata) {
		
		Request request = new Request();
		request.setManagerName(GraphEngineManagers.GRAPH_MANAGER);
		request.setOperation("createRelation");
		request.getContext().put(GraphHeaderParams.graph_id.name(),
				graphId);
		request.put(GraphDACParams.start_node_id.name(), startId);
		request.put(GraphDACParams.relation_type.name(), relationType);
		request.put(GraphDACParams.end_node_id.name(), endId);
		request.put(GraphDACParams.metadata.name(), metadata);
		Response response = util.getResponse(request);
		Assert.assertEquals("successful", response.getParams().getStatus());
	}
	
	public static void after() {
		System.out.println("deleting Graph...!!");
		graphDb.shutdown();
		deleteGraph(languageId);
	}
	
	
	private static void deleteGraph(String graphId) {

		try {
			Request request = new Request();
			request.setManagerName(GraphEngineManagers.GRAPH_MANAGER);
			request.setOperation("deleteGraph");
			request.getContext().put(GraphHeaderParams.graph_id.name(),
					graphId);
			Response resp = util.getResponse(
					request);
			PlatformLogger.log("List | Response: " ,resp);
			
			if (!resp.getParams().getStatus().equalsIgnoreCase("successful")) {
				System.out.println(resp.getParams().getErr() + resp.getParams().getErrmsg());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
