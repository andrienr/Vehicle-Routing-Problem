package reader;

import model.Location;
import model.Node;
import model.Instance;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;   

public class InstanceReader {
	private Instance instance;
	private String file;
	private boolean printDebugInfo;

	public InstanceReader(Instance instance, String file, boolean printDebugInfo){
		this.instance = instance;
		this.file = file;
		this.printDebugInfo = printDebugInfo;
	}

	/**
	 * 
	 * @param maxActivities
	 * @param maxUnits
	 * @param numberOfVehicles
	 */
	public void read(Integer... params) {

		instance.setLocations(new ArrayList<Location>());
		instance.setNodes(new ArrayList<Node>());			
		int maxActivities = params.length > 0 ? params[0] : 0;
		int maxUnits = params.length > 1 ? params[1] : 0;
		int maxVehicles = params.length > 2 ? params[2] : 0;
		int counter = -1, problemZeroCount = 0, problemOneCount=0, problemTwoCount = 0;
		
		List<String> lines = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(file)))
        {
        	lines = br.lines().collect(Collectors.toList());
        } catch (IOException e)
		{
			e.printStackTrace();
		}
        
        for(String line : lines)
        {
			counter++;
			String[] tokens = line.replace("\r", "").trim().split(" +");
			if (counter == 0) {
	    		instance.numberOfActivities = Integer.parseInt(tokens[1]);
	    		instance.numberOfUnits = Integer.parseInt(tokens[2]);
			    instance.numberOfVehicles = maxVehicles > 0 ? maxVehicles : Integer.parseInt(tokens[6]);
				instance.numberOfTimeSlots=Integer.parseInt(tokens[4]);
				instance.timeSlotDuration=Integer.parseInt(tokens[5]);
				instance.vehicleSpeed=Integer.parseInt(tokens[7]);
				if(printDebugInfo) {
					System.out.println("Activities: "+maxActivities+" Units: "+maxUnits+" Vehicles: "+maxVehicles);
				}				
			}
			if (counter > 1 && counter < instance.numberOfActivities+3) {
				if(maxActivities != 0 && maxActivities+2 < counter) {
					continue;
				}
				readLocations(tokens);
			}
			if (counter >= instance.numberOfActivities+3 && counter <= instance.numberOfUnits+instance.numberOfActivities+3) {
				if(maxUnits != 0 && maxUnits+instance.numberOfActivities+2 < counter) {
					continue;
				}
				readLocations(tokens);
			}
			if (counter > instance.numberOfUnits+instance.numberOfActivities+4) {
				if (((instance.getLocationFromLocationId(Integer.parseInt(tokens[1]))!=null || Integer.parseInt(tokens[1])==-1) 
						&& instance.getLocationFromLocationId(Integer.parseInt(tokens[2]))!=null)) {
					int pickupNodePosX=Integer.parseInt(tokens[1])>-1?instance.getLocationFromLocationId(Integer.parseInt(tokens[1])).getX():-1; 
					int pickupNodePosY=Integer.parseInt(tokens[1])>-1?instance.getLocationFromLocationId(Integer.parseInt(tokens[1])).getY():-1;
					int deliveryNodePosX=instance.getLocationFromLocationId(Integer.parseInt(tokens[2])).getX(); 
					int deliveryNodePosY=instance.getLocationFromLocationId(Integer.parseInt(tokens[2])).getY(); 
					int readyTime=Integer.parseInt(tokens[3])*60;
					int dueTime=(1+Integer.parseInt(tokens[3]))*60;
					int fixedServiceTime=Integer.parseInt(tokens[4]);
					int variableServiceTime=Integer.parseInt(tokens[5]);
					//task reduction only in the service node, i.e. problem types 1 and 2
					double[] multiTaskReduction=Integer.parseInt(tokens[1])>-1?
							instance.getLocationFromLocationId(Integer.parseInt(tokens[1])).getMultiTaskReduction():new double[] {0};
					int selectionPenalty = Integer.parseInt(tokens[6]);
					double timePenalty = Double.parseDouble(tokens[7]);
					int problemType = Integer.parseInt(tokens[8]);
					boolean consecutive = Integer.parseInt(tokens[9])==0?false:true;
						
					switch (Integer.parseInt(tokens[8])) {
					  case 0:
						  // problem type 0 -> use delivery node
						  instance.getNodes().add(new Node(1, deliveryNodePosX, deliveryNodePosY, readyTime,
								  dueTime, fixedServiceTime, variableServiceTime, multiTaskReduction, selectionPenalty, timePenalty, problemType, consecutive));
						  problemZeroCount++;
						  break;
					  case 1:
						// problem type 1 -> create pickup node
						  instance.getNodes().add(new Node(1, deliveryNodePosX, deliveryNodePosY, 0, 0, 0, 0, new double[] {0}, 0, 0, problemType, false));
						  instance.getNodes().add(new Node(2, pickupNodePosX, pickupNodePosY, readyTime, 
								 dueTime, fixedServiceTime, variableServiceTime, multiTaskReduction, selectionPenalty, timePenalty, problemType, consecutive));
						  instance.getNodes().add(new Node(3, deliveryNodePosX, deliveryNodePosY, 0, 0, 0, 0, new double[] {0}, 0, 0, problemType, false));
						  problemOneCount++;
						  break;
					  case 2:
						  // problem type 2 -> use pickup node
						  instance.getNodes().add(new Node(1,pickupNodePosX, pickupNodePosY,readyTime,
								  dueTime, fixedServiceTime, variableServiceTime, multiTaskReduction, selectionPenalty, timePenalty, problemType, consecutive));
						  instance.getNodes().add(new Node(2, deliveryNodePosX, deliveryNodePosY, 0, 0, 0, 0, new double[] {0}, 0, 0, problemType, false));
						  problemTwoCount++;
					    break;
					}
				}
			}
		}
		instance.setProblemZeroCount(problemZeroCount);
		instance.setProblemOneCount(problemOneCount) ;
		instance.setProblemTwoCount(problemTwoCount);
		if(printDebugInfo) {
			int totalNodes = 2+problemZeroCount+3*problemOneCount+2*problemTwoCount;
			System.out.println("problemZeroCount: "+instance.getProblemZeroCount()+" - problemOneCount: "+instance.getProblemOneCount()+
					" - problemTwoCount: "+instance.getProblemTwoCount()+" - totalNodes: "+totalNodes);
		}
		sortNodes(instance.getNodes());
	} 
	

	private void sortNodes(ArrayList<Node> nodes) {
		ArrayList<Node> sortedNodes = new ArrayList<Node>();
		sortedNodes.add(new Node(0, instance.getLocationFromLocationId(0).getX(), instance.getLocationFromLocationId(0).getY(),
				0, 0, 0, 0, new double[] {0}, 0, 0, -1, false));
		sortedNodes.add(new Node(nodes.size()+1, instance.getLocationFromLocationId(0).getX(), instance.getLocationFromLocationId(0).getY(),
				0, 0, 0, 0, new double[] {0}, 0, 0, -1, false));
		int i=1, j=0, k=0;
		for(Node p:nodes) {
			switch (p.getProblemType()) {
				case 0:
					p.setNodeId(i);
					sortedNodes.add(p);
					i++;
					break;
				case 1:
					switch(p.getNodeId()) {
						case 1:
							p.setNodeId(1+instance.getProblemZeroCount()+j);					
							sortedNodes.add(p);
							break;
						case 2:
							p.setNodeId(1+instance.getProblemZeroCount()+instance.getProblemOneCount()+j);					
							sortedNodes.add(p);
							break;
						case 3:
							p.setNodeId(1+instance.getProblemZeroCount()+2*instance.getProblemOneCount()+j);					
							sortedNodes.add(p);
							j++;
							break;
					}
					break;
				case 2:
					switch(p.getNodeId()) {
					case 1:
						p.setNodeId(1+instance.getProblemZeroCount()+3*instance.getProblemOneCount()+k);					
						sortedNodes.add(p);
						break;
					case 2:
						p.setNodeId(1+instance.getProblemZeroCount()+3*instance.getProblemOneCount()+instance.getProblemTwoCount()+k);					
						sortedNodes.add(p);
						k++;
						break;
					}
					break;
				}
			}
		sortedNodes.sort(Comparator.comparingInt(Node::getNodeId));
		instance.setNodes(sortedNodes);
	}

	private void readLocations(String[] tokens) {
		Location location = new Location(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
		if(tokens.length>3) {
			List<Double> x = new ArrayList<Double>();

			for(String t : Arrays.copyOfRange(tokens,3,tokens.length)) {
				 x.add(Double.parseDouble(t));
			}
			location.setMultiTaskReduction(x);
		}
		instance.getLocations().add(location);
	}
}