package model;

import gurobi.*;

public class GurobiModel {

	private Instance instance;
	private GRBModel model;
	
	public GurobiModel(GRBModel model, Instance instance) {
		this.instance = instance;
		this.model = model;
	}
	
	/**
	 * 
	 * @return
	 * @throws GRBException
	 */
	public GRBModel createModel() throws GRBException {
	        	
	            // Decision variables
	            GRBVar[][][] x = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()][instance.getNodes().size()];
	            for(int k=0; k < x.length; k++) {
		            for(int i=0; i < x[k].length; i++) {
		                for(int j=0; j < x[k][i].length; j++) {
		                    x[k][i][j] = model.addVar(0, 1, 0, GRB.BINARY,"x^"+k+"_"+i+"_"+j);
		                }
		            }
	            }
	            
	            // Variables for selection constraints
	            GRBVar[] y = new GRBVar[instance.getNodes().size()];
            	for(Node i : instance.getPickupNodes()) {
	            	y[i.getNodeId()] = model.addVar(0, 1, 0, GRB.BINARY,"y^_"+i);	            	
	            }
	            
	            // w^k_i time when vehicle k visits node i 
	            // g^k_i normal time for vehicle k in node i 
	            // h^k_i extra time for vehicle k in node i 
	            // r^k_i reduction time for vehicle k in node i
	            GRBVar[][] w = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()];
	            GRBVar[][] g = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()];
	            GRBVar[][] h = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()];
	            GRBVar[][] r = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()];
	            for(int k=0; k < w.length; k++) {
	            	for(int i=0; i < w[k].length; i++) {
	            		w[k][i] = model.addVar(0, instance.getTimeSlotDuration()*instance.getNumberOfTimeSlots(), 0, GRB.CONTINUOUS ,"w^"+k+"_"+i);
	            		g[k][i] = model.addVar(0, instance.getTimeSlotDuration()*instance.getNumberOfTimeSlots(), 0, GRB.CONTINUOUS ,"g^"+k+"_"+i);
	            		h[k][i] = model.addVar(0, instance.getTimeSlotDuration()*instance.getNumberOfTimeSlots(), 0, GRB.CONTINUOUS ,"h^"+k+"_"+i);
	            		r[k][i] = model.addVar(0, 1, 0, GRB.CONTINUOUS ,"r^"+k+"_"+i);
	            	}
	            }
	            
	            // Auxiliary variables z
	            GRBVar[][][] z = new GRBVar[instance.getNumberOfVehicles()][instance.getNodes().size()][instance.getMaxMultiTaskReductionLength()];
	            for(int k=0; k < w.length; k++) {
	            	for (Node i : instance.getPickupNodes()) {    	
	            		for(int j=0; j < instance.getMaxMultiTaskReductionLength(); j++) {
	            			z[k][i.getNodeId()][j] = model.addVar(0, 1, 0, GRB.BINARY,"z^"+k+"_"+i+"_"+j);
	            		}
	            	}
	            }
	            	            
	            // Forbid edge from node back to itself
	            for(int k=0; k < x.length; k++) {
		            for(int i=0; i < x[k].length; i++) {
		            	x[k][i][i].set(GRB.DoubleAttr.UB, 0.0);
		            }
	            }
	             
	            // (1) 
	            objectiveFunction(model,x,y,h);
	            
	            // (2-3-9-11) 
	            pickupDeliveryAndConnectivityConstraints(model,x,w);

	            // (4) 
	            selectionConstraints(model,x,y);
	                                 
	            // (5-6)
	            oneTourConstraints(model,x);

	            // (7) 
	            flowConservationConstraints(model,x);
	            
	            // (8)
	            timeWindowsConstraints(model,x,w,r);
	            
	            // (10)
	            connectivityForProblemOne(model,x,w,r);
	        	
	        	// (12)
	        	sumOfNormalTimeAndExtraTime(model, w, g, h);
	        	
	        	// (13)
	        	normalTimeConstraints(model, g);
	        	
	        	// (14-15-16) 
	        	calculateReductions(model,x, z, r);
	        	
	        	// (17) 
	        	consecutiveNodeConstraints(model, w, r);

	        	model.write("model.lp");

				return model;
          
	        }

	/**
	 * sum distances + selection penalties + extra time penalties
	 * 
	 * @param model
	 * @param x
	 * @throws GRBException
	 */
	private void objectiveFunction(GRBModel model, GRBVar[][][] x, GRBVar[] y, GRBVar[][] h) throws GRBException{
		GRBLinExpr expr = new GRBLinExpr();
		for(int k=0; k < x.length; k++) {
			for(int i=0; i < x[k].length; i++) {
				for(int j=0; j < x[k][i].length; j++) {
						expr.addTerm(instance.getAdjacencyMatrix()[i][j], x[k][i][j]);
				}
			}
			for (Node i : instance.getPickupNodes()) {
				expr.addTerm(i.getTimePenalty(), h[k][i.getNodeId()]);
			}
		}
		for (Node i : instance.getPickupNodes()) {
			expr.addTerm(i.getSelectionPenalty(), y[i.getNodeId()]);
		}
		model.setObjective(expr);
	}

	/**
	 * Relation between x and y for pickup nodes
	 * 
	 * @param model
	 * @param x
	 * @param y
	 * @throws GRBException
	 */
	private void selectionConstraints(GRBModel model, GRBVar[][][] x,GRBVar[] y) throws GRBException {
		for (Node i : instance.getPickupNodes()) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for (int k = 0; k < x.length; k++) {
				for(int j = 0; j < x[k][i.getNodeId()].length; j++) {	
						expr.addTerm(1.0, x[k][i.getNodeId()][j]);
				}
			}
			expr.addTerm(-1.0, y[i.getNodeId()]);
			model.addConstr(expr, GRB.EQUAL, 1, "selectionConstraint"+"_"+i);
		}	
	}

	/**
	 * When entering a node exit from the same node
	 * 
	 * @param model
	 * @param x
	 * @throws GRBException
	 */
	private void flowConservationConstraints(GRBModel model, GRBVar[][][] x) throws GRBException {
		for (int k = 0; k < x.length; k++) {
			for (Node i : instance.getNodesExceptDepots()) {
				GRBLinExpr expr = new GRBLinExpr();
				
				for(int j = 0; j < x[k][i.getNodeId()].length; j++) {
						expr.addTerm(1.0, x[k][i.getNodeId()][j]);
				}
				
				for(int j = 0; j < x[k][i.getNodeId()].length; j++) {
						expr.addTerm(-1.0, x[k][j][i.getNodeId()]);
				}

				model.addConstr(expr, GRB.EQUAL, 0, "flowConservationConstraint^"+k+"_"+i.getNodeId());
			}	
		}		
	}
	
	/**
	 * Vehicles must go out of depot and go back to depot once
	 * 
	 * @param model
	 * @param x
	 * @throws GRBException
	 */
	private void oneTourConstraints(GRBModel model, GRBVar[][][] x) throws GRBException {
		for (int k = 0; k < x.length; k++) {
			int lastNode = x[k].length -1;
			GRBLinExpr expr1 = new GRBLinExpr(), expr2 = new GRBLinExpr(), expr3 = new GRBLinExpr();
			for (Node i : instance.getNodes()) {
				expr1.addTerm(1.0, x[k][0][i.getNodeId()]);
				expr2.addTerm(1.0, x[k][i.getNodeId()][lastNode]);
				expr3.addTerm(1.0, x[k][i.getNodeId()][0]);
			}
			model.addConstr(expr1, GRB.EQUAL, 1, "oneTourOut_"+k);
			model.addConstr(expr2, GRB.EQUAL, 1, "oneTourIn_"+k);
			model.addConstr(expr3, GRB.EQUAL, 0, "forbidToZero_"+k);
		}		
	}
	
	/**
	 * If vehicle visit node i then it must visit locations i+n and i+2n for problem type 1
	 * If vehicle visit node i then it must visit node i+n for problem type 2
	 * 
	 * @param model
	 * @param x
	 * @throws GRBException
	 */
	private void pickupDeliveryAndConnectivityConstraints(GRBModel model, GRBVar[][][] x, GRBVar[][] w) throws GRBException {
		for (int k = 0; k < x.length; k++) {
			GRBLinExpr expr1 = new GRBLinExpr();
			GRBLinExpr expr2 = new GRBLinExpr();
			GRBLinExpr expr3 = new GRBLinExpr();
			for (Node i : instance.getPickupNodes()) {
				if(i.getProblemType()==1) {
					GRBLinExpr time1 = new GRBLinExpr();
					GRBLinExpr time2 = new GRBLinExpr();
					int plusIndex = i.getNodeId()+instance.getProblemOneCount();
					int minusIndex = i.getNodeId()-instance.getProblemOneCount();
					time1.addTerm(-1.0, w[k][plusIndex]);
					time1.addTerm(1.0, w[k][i.getNodeId()]);
					time2.addTerm(-1.0, w[k][i.getNodeId()]);
					time2.addTerm(1.0, w[k][minusIndex]);
					for(int j = 0; j < x[k].length; j++) {
						expr1.addTerm(-1.0, x[k][plusIndex][j]);
						expr1.addTerm(1.0, x[k][i.getNodeId()][j]);
						expr2.addTerm(-1.0, x[k][minusIndex][j]);
						expr2.addTerm(1.0, x[k][i.getNodeId()][j]);
					}
					model.addConstr(time1, GRB.LESS_EQUAL, 0, "pickupDeliveryProblemOneTimePlusIndex^"+k+"_"+i);
					model.addConstr(time2, GRB.LESS_EQUAL, 0, "pickupDeliveryProblemOneTimeMinusIndex^"+k+"_"+i);
					model.addConstr(expr1, GRB.EQUAL, 0, "pickupDeliveryProblemOnePlusIndex^"+k+"_"+i);
					model.addConstr(expr2, GRB.EQUAL, 0, "pickupDeliveryProblemOneMinusIndex^"+k+"_"+i);
				}
				if(i.getProblemType()==2) {
					GRBLinExpr time = new GRBLinExpr();
					int plusIndex = i.getNodeId()+instance.getProblemTwoCount();
					time.addTerm(-1.0, w[k][plusIndex]);
					time.addTerm(1.0, w[k][i.getNodeId()]);
					for(int j = 0; j < x[k].length; j++) {
						expr3.addTerm(1.0, x[k][i.getNodeId()][j]);
						expr3.addTerm(-1.0, x[k][plusIndex][j]);
					}
					model.addConstr(time, GRB.LESS_EQUAL, 0, "pickupDeliveryProblemTwoTimePlusIndex^"+k+"_"+i);
					model.addConstr(expr3, GRB.EQUAL, 0, "pickupDeliveryProblemTwo^"+k+"_"+i);
				}
			}
		}
	}

	/**
	 * Delivery nodes visited first must have arrival time less than the delivery node visited after performing the service
	 *  
	 * @param model
	 * @param x
	 * @param w
	 * @param r
	 * @throws GRBException
	 */
	private void connectivityForProblemOne(GRBModel model, GRBVar[][][] x, GRBVar[][] w, GRBVar[][] r) throws GRBException {
		for (int k = 0; k < x.length; k++) {
			for (Node i : instance.getPickupNodes()) {
				if(i.getProblemType()==1) {
					GRBLinExpr expr = new GRBLinExpr();
					int plusIndex = i.getNodeId()+instance.getProblemOneCount();
					int minusIndex = i.getNodeId()-instance.getProblemOneCount();
					expr.addTerm(1.0, w[k][plusIndex]);
					expr.addTerm(-1.0, w[k][minusIndex]);
					expr.addTerm(i.getVariableServiceTime(), r[k][i.getNodeId()]);
					model.addConstr(expr, GRB.GREATER_EQUAL, i.getFixedServiceTime() + i.getVariableServiceTime()+2*instance.getTravelTime(i.getNodeId(), plusIndex), "connectivityForProblemOne^"+k+"_"+i);
				}
			}
		}
	}
	
	/**
	 * Nodes need to be visited consecutively
	 * 
	 * @param model
	 * @param w
	 * @throws GRBException
	 */
	private void consecutiveNodeConstraints(GRBModel model, GRBVar[][] w, GRBVar[][] r) throws GRBException {
	  for(int k=0; k < w.length; k++) {
		  for (Node i : instance.getPickupNodes()) {
			  if(i.isConsecutive()) {
				if(i.getProblemType()==2 ) {
					int plusIndex = i.getNodeId()+instance.getProblemOneCount();
					GRBLinExpr expr = new GRBLinExpr();
					expr.addTerm(1.0, w[k][i.getNodeId()]);
					expr.addTerm(-1.0, w[k][plusIndex]);
					expr.addTerm(i.getVariableServiceTime(), r[k][i.getNodeId()]);
					model.addConstr(expr, GRB.EQUAL, - instance.getTravelTime(i.getNodeId(),plusIndex)
							-(i.getFixedServiceTime() + i.getVariableServiceTime()), 
							"consecutiveNodeConstraintProblemOne^"+k+"_1");
				}
				if(i.getProblemType()==1 ) {
					int plusIndex = i.getNodeId()+ instance.getProblemOneCount();
					int minusIndex = i.getNodeId()-instance.getProblemOneCount();
					GRBLinExpr expr1 = new GRBLinExpr();
					GRBLinExpr expr2 = new GRBLinExpr();
					expr1.addTerm(1.0, w[k][i.getNodeId()]);
					expr1.addTerm(-1.0, w[k][plusIndex]);
					expr1.addTerm(i.getVariableServiceTime(), r[k][i.getNodeId()]);
					model.addConstr(expr1, GRB.EQUAL, - instance.getTravelTime(i.getNodeId(),plusIndex)
							-(i.getFixedServiceTime() + i.getVariableServiceTime()), 
							"consecutiveNodeConstraintProblemTwoPreviousNode^"+k+"_1");
					expr2.addTerm(-1.0, w[k][i.getNodeId()]);
					expr2.addTerm(+1.0, w[k][minusIndex]);
					model.addConstr(expr2, GRB.EQUAL, - instance.getTravelTime(minusIndex,i.getNodeId()),
							"consecutiveNodeConstraintProblemTwoNextNode^"+k+"_1");
				}
			  }
		  }
	  }
	}
	
	/**
	 * Consider time reduction when performing multiple tasks in the same node
	 * 
	 * @param model
	 * @param x 
	 * @param z Auxiliary variables
	 * @param r Reduction
	 * @throws GRBException
	 */
	private void calculateReductions(GRBModel model, GRBVar[][][] x, GRBVar[][][] z, GRBVar[][] r) throws GRBException {
		for(int k=0; k < x.length; k++) {
			GRBLinExpr sumOfZuMultipliedByuEqualsSumOfVehiclesVisitingI = new GRBLinExpr();
			GRBLinExpr sumOfZuEquals1 = new GRBLinExpr();
			GRBLinExpr sumOfZuMultipliedByTaskReductionOfuEqualsRi = new GRBLinExpr();
			for (Node i : instance.getPickupNodes()) {
	            for(int j=0; j < x[k].length; j++) {
	            	sumOfZuMultipliedByuEqualsSumOfVehiclesVisitingI.addTerm(-1, x[k][i.getNodeId()][j]);
	        	}
				sumOfZuMultipliedByTaskReductionOfuEqualsRi.addTerm(-1, r[k][i.getNodeId()]);
				if(i.getMultiTaskReduction().length>1) {
					for (int u = 0; u < i.getMultiTaskReduction().length; u++) {
						sumOfZuMultipliedByuEqualsSumOfVehiclesVisitingI.addTerm(u, z[k][i.getNodeId()][u]);
						sumOfZuEquals1.addTerm(1, z[k][i.getNodeId()][u]);
						sumOfZuMultipliedByTaskReductionOfuEqualsRi.addTerm(i.getMultiTaskReduction()[u], z[k][i.getNodeId()][u]);
					}
				}
			}
			model.addConstr(sumOfZuMultipliedByuEqualsSumOfVehiclesVisitingI, GRB.EQUAL, 0, "sumOfZjMultipliedByjEqualsQi^"+k);
			model.addConstr(sumOfZuEquals1, GRB.EQUAL, 1, "sumOfZjEquals1^"+k);
			model.addConstr(sumOfZuMultipliedByTaskReductionOfuEqualsRi, GRB.EQUAL, 0, "sumOfZjMultipliedByTaskReductionOfjEqualsRi^"+k);
		}
	}

	/**
	 * Time windows constraints 
	 * 
	 * @param model
	 * @param x
	 * @throws GRBException
	 */
	private void timeWindowsConstraints(GRBModel model, GRBVar[][][] x, GRBVar[][] w, GRBVar[][] r) throws GRBException {
		double M = 700;	
        for(int k=0; k < w.length; k++) {
            for(int i=0; i < w[k].length; i++) {
            	Node currentNode = instance.getNodes().get(i);
            	for(int j=0; j < w[k].length; j++) {
            		GRBLinExpr expr = new GRBLinExpr();
            		expr.addTerm(1.0, w[k][i]);
                	expr.addTerm(-1.0, w[k][j]);
                	expr.addTerm(M, x[k][i][j]);
                	expr.addTerm(-currentNode.getVariableServiceTime(), r[k][i]);
                	model.addConstr(expr, GRB.LESS_EQUAL, M - currentNode.getFixedServiceTime() -currentNode.getVariableServiceTime() - instance.getTravelTime(i,j), "serviceTime^"+k+"_"+i+"_"+j);
            	}
            }
        }
	}
	
	/**
	 * Constraint for calculating time penalties
	 * 
	 * @param model
	 * @param w
	 * @param g
	 * @param h
	 * @throws GRBException
	 */
	private void sumOfNormalTimeAndExtraTime(GRBModel model, GRBVar[][] w, GRBVar[][] g, GRBVar[][] h) throws GRBException {
		for(int k=0; k < w.length; k++) {
        	for(int i=0; i < w[k].length; i++) {
         		GRBLinExpr expr = new GRBLinExpr();
        		expr.addTerm(1.0, g[k][i]);
        		expr.addTerm(1.0, h[k][i]);
				model.addConstr(w[k][i],GRB.EQUAL, expr,"sumOfNormalTimeAndExtraTime^"+k+"_"+i);
        	}
		}
	}
	
	/**
	 * Time slots for normal time
	 * 
	 * @param model
	 * @param g
	 * @throws GRBException
	 */
	private void normalTimeConstraints(GRBModel model, GRBVar[][] g) throws GRBException {	
		for(int k=0; k < g.length; k++) {
        	for(int i=0; i < g[k].length; i++) {
            	Node currentNode = instance.getNodes().get(i);
        		GRBLinExpr expr = new GRBLinExpr();
        		expr.addTerm(1.0, g[k][i]);
        		if(currentNode.getReadyTime()!=0) {
        			model.addRange(expr, currentNode.getReadyTime(), currentNode.getDueTime(), "normalTimeConstraints_"+k+"_"+i);
        		}
        	}
        }
	}	
}