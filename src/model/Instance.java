package model;

import java.util.ArrayList;
import java.util.List;

public class Instance {

	public int numberOfActivities;
	public int numberOfUnits;
	public int numberOfRequests;
	public int numberOfTimeSlots;
	public int timeSlotDuration;
	public int numberOfVehicles;
	public int vehicleSpeed;
	public ArrayList<Location> locations;
	public ArrayList<Node> nodes;
	double[][]  adjacencyMatrix;
	double [][] travelTimeMatrix;
	public int problemZeroCount;
	public int problemOneCount;
	public int problemTwoCount;
	

	public void buildDistanceMatrix() {
        this.adjacencyMatrix = new double[getNodes().size()][getNodes().size()];
        for(int i=0; i < getNodes().size(); i++) {
            for(int j=0; j < getNodes().size(); j++) {
                this.adjacencyMatrix[i][j] = getDistance(i, j);
            }
        }
	}
	
	public void buildtravelTimeMatrix() {
        this.travelTimeMatrix = new double[getNodes().size()][getNodes().size()];
        for(int i=0; i < getNodes().size(); i++) {
            for(int j=0; j < getNodes().size(); j++) {
                this.travelTimeMatrix[i][j] = getTravelTime(i, j);
            }
        }
	}
	
	public double[][] getTravelTimeMatrix(){
		return this.travelTimeMatrix;
	}
	
	public double getDistance(int i, int j){		
		
		return this.nodes.get(i).distance(this.nodes.get(j));
	}

	public ArrayList<Node> getPickupNodes(){
		ArrayList<Node> pickupNodes = new ArrayList<Node>();
		for(Node p : getNodes()) {
			if(p.getSelectionPenalty()!=0) {
				pickupNodes.add(p);
			}
		}
		return pickupNodes;
	}
	
	public ArrayList<Node> getNodesExceptDepots(){
		ArrayList<Node> nodes = new ArrayList<Node>();
		for(Node p : getNodes()) {
			if(p.getNodeId()!=0 && p.getNodeId()!=getNodes().size()-1) {
				nodes.add(p);
			}
		}
		return nodes;
	}
	
	public int getNumberOfActivities() {
		return numberOfActivities;
	}

	public int getNumberOfVehicles() {
		return numberOfVehicles;
	}
	
	public double[][] getAdjacencyMatrix() {
		return adjacencyMatrix;
	}
	

	public List<Location> getLocations() {
		return locations;
	}
	
	public Location getLocationFromLocationId(int locationId) {
		Location location = null;
		for (Location i : this.getLocations()) {
			if(i.getLocationId()==locationId) {
				location=i;
			}
		}
		return location;
	}


	public void setLocations(ArrayList<Location> locations) {
		this.locations = locations;
	}

	public ArrayList<Node> getNodes() {

		return this.nodes;
	}


	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
		
	}

	public int getProblemZeroCount() {
		return problemZeroCount;
	}

	public void setProblemZeroCount(int problemZeroCount) {
		this.problemZeroCount = problemZeroCount;
	}

	public int getProblemOneCount() {
		return problemOneCount;
	}

	public void setProblemOneCount(int problemOneCount) {
		this.problemOneCount = problemOneCount;
	}

	public int getProblemTwoCount() {
		return problemTwoCount;
	}

	public void setProblemTwoCount(int problemTwoCount) {
		this.problemTwoCount = problemTwoCount;
	}

	public int getVehicleSpeed() {
		return vehicleSpeed;
	}

	public int getMaxMultiTaskReductionLength(){
		int maxLength=0;
		for(Node p : this.getNodesExceptDepots()) {
			if (p.getMultiTaskReduction().length > maxLength) {
				maxLength=p.getMultiTaskReduction().length;
			}
		}
		return maxLength;
	}
	
	/**
	 * in minutes
	 * 
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public double getTravelTime(int fromNode, int toNode) {
		return getDistance(fromNode,toNode)/(getVehicleSpeed()*60);
	}
	
	public int getNumberOfTimeSlots() {
		return numberOfTimeSlots;
	}

	public int getTimeSlotDuration() {
		return timeSlotDuration;
	}
}