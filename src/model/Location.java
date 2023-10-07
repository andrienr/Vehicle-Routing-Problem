package model;

import java.util.List;

public class Location {
	
	int X, Y, locationId;
	double[] multiTaskReduction;


	public Location(int i, int x, int y) {
		this.locationId=i;
		this.X=x;
		this.Y=y;
		this.multiTaskReduction = null;
	}
	
	public int getX() {
		return X;
	}

	public void setX(int x) {
		X = x;
	}

	public int getY() {
		return Y;
	}

	public void setY(int y) {
		Y = y;
	}

	public void setLocationId(int parseInt) {
		
		this.locationId=parseInt;
	}

	public int getLocationId() {

		return this.locationId;
	}

	/**
	 * Euclidean distance
	 * @param p
	 * @return
	 */
	public double distance (Location p) {
		if(p==null) {
			return Double.MAX_VALUE;
		}
		double dx = this.getX() - p.getX();
		double dy = this.getY() - p.getY();
		return Math.sqrt(dx*dx + dy*dy);
	}

	public void setMultiTaskReduction(List<Double> multiTaskReduction) {
		this.multiTaskReduction = multiTaskReduction.stream().mapToDouble(d -> d).toArray();		
	}

	public double[] getMultiTaskReduction() {
		return multiTaskReduction;
	}
}