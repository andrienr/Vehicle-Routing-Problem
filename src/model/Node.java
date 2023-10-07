package model;

import java.util.List;

public class Node {

	int nodeId;
	int posX; 
	int posY;
	int readyTime;
	int dueTime;
	int fixedServiceTime;
	int variableServiceTime;
	double[] multiTaskReduction;

	int selectionPenalty; 
	double timePenalty;
	int problemType;
	boolean consecutive;


	
	public Node(int nodeId, int posX, int posY, int readyTime, int dueTime, int fixedServiceTime,
			int variableServiceTime, double[] multiTaskReduction, int selectionPenalty, double timePenalty,
			int problemType, boolean consecutive) {
		this.nodeId = nodeId;
		this.posX = posX;
		this.posY = posY;
		this.readyTime = readyTime;
		this.dueTime = dueTime;
		this.fixedServiceTime = fixedServiceTime;
		this.variableServiceTime = variableServiceTime;
		this.multiTaskReduction = multiTaskReduction;
		this.selectionPenalty = selectionPenalty;
		this.timePenalty = timePenalty;
		this.problemType = problemType;
		this.consecutive = consecutive;
	}

	public int getPosX() {
		return posX;
	}

	public void setPosX(int x) {
		posX = x;
	}

	public int getProblemType() {
		return problemType;
	}
	
	public int getPosY() {
		return posY;
	}

	public void setPosY(int y) {
		posY = y;
	}

	public void setNodeId(int parseInt) {
		
		this.nodeId=parseInt;
	}

	public int getReadyTime() {
		return readyTime;
	}

	public double getTimePenalty() {
		return timePenalty;
	}

	public int getDueTime() {
		return dueTime;
	}

	public int getFixedServiceTime() {
		return fixedServiceTime;
	}

	public int getVariableServiceTime() {
		return variableServiceTime;
	}

	public int getSelectionPenalty() {
		return selectionPenalty;
	}
	
	public int getNodeId() {

		return this.nodeId;
	}
	
	/**
	 * Euclidean distance
	 * @param p
	 * @return
	 */
	public double distance (Node p) {
		if(p==null) {
			return Double.MAX_VALUE;
		}
		double dx = this.getPosX() - p.getPosX();
		double dy = this.getPosY() - p.getPosY();
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	public void setMultiTaskReduction(List<Double> multiTaskReduction) {
		this.multiTaskReduction = multiTaskReduction.stream().mapToDouble(d -> d).toArray();		
	}
	
	public double[] getMultiTaskReduction() {
		return multiTaskReduction;
	}

	public boolean isConsecutive() {
		return this.consecutive;
	}
}
