package Controller;

import ClientModel.Agent;
import ClientModel.DataPackage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Purpose: A thread class that can be started via the server simulator.
 * AgentHandler manages a group of agents that have the same color.
 * An AgentHandler checks for collisions, updates the group's positions, and
 * sends position data over a socket to the server.
 *
 * @author Amin Yassin
 * @version 1.0 03/01/2018
 *
 */
public class AgentHandler extends Thread {

    private List<Agent> agentList = new ArrayList<>();
    private String handlerID;
    private float[] xyPositions;
    private float[] oppositeXY;

    /**
     * A constructor of the AgentHandler.
     * <br>
     * The handler ID is specified by the client that will initiate the handlers.
     *
     * @param ID
     */
    /*public AgentHandler(String ID){
        this.handlerID = ID;
    }*/


    /**
     * A getter function that returns the ID of the handler.
     *
     * @return A string representation of the AgentHandler.
     *
     */
    public String getHandlerID(){
        return this.handlerID;
    }

    /**
     * Checks the distance difference between agents in the same group.
     * The function is optimized so that agents that were already checked
     * aren't checked again.
     *
     */
    private void checkCollisionSameGroup(){

        int c = 0;
        float x, y;

        if(getHandlerID().equalsIgnoreCase("orange")) {
            for (int i = 0; i < xyPositions.length - 2; i += 2) {
                for (int j = i + 2; j < xyPositions.length; j += 2) {
                    x = xyPositions[i] - xyPositions[j];
                    y = xyPositions[i+1] - xyPositions[j+1];
                    if(Math.abs(x) < 50.0f && Math.abs(y) < 50.0f){
                        agentList.get(c).moveUp();
                    }
                }
                c++;
            }
        }

        if(getHandlerID().equalsIgnoreCase("blue")){
            for (int i = 0; i < xyPositions.length - 2; i += 2) {
                for (int j = i + 2; j < xyPositions.length; j += 2) {
                    x = xyPositions[i] - xyPositions[j];
                    y = xyPositions[i+1] - xyPositions[j+1];
                    if(Math.abs(x) < 50.0f && Math.abs(y) < 50.0f){
                        agentList.get(c).moveDown();
                    }
                }
                c++;
            }
        }
    }

    private void checkCollisionOtherGroup(){
        float x, y;
        int c = 0;
        if(getHandlerID().equalsIgnoreCase("orange")){
            for(int i = 0; i < xyPositions.length - 1; i += 2){
                for(int j = 0; j < oppositeXY.length - 1; j += 2){
                    x = xyPositions[i] - oppositeXY[j];
                    y = xyPositions[i+1] - oppositeXY[j+1];
                    if(Math.abs(x) < 100.0f && Math.abs(y) < 50.0f){
                        agentList.get(c).moveUpAndBack();
                    }
                }
                agentList.get(c).moveToGoal();
                c++;
            }
        }

        if(getHandlerID().equalsIgnoreCase("blue")){
            for(int i = 0; i < xyPositions.length - 1; i += 2){
                for(int j = 0; j < oppositeXY.length - 1; j += 2){
                    x = xyPositions[i] - oppositeXY[j];
                    y = xyPositions[i+1] - oppositeXY[j+1];
                    if(Math.abs(x) < 100.0f && Math.abs(y) < 50.0f){
                        agentList.get(c).moveDownAndBack();
                    }
                }
                agentList.get(c).moveToGoal();
                c++;
            }
        }
    }

    /**
     * Run method for the class. Connection to the server is established here.
     * Values are sent from this function to the server.
     *
     */
    @Override
    public void run() {

        System.out.println("Agent Handler started!");
        final String localhost = "127.0.0.1";
        final int PORT = 1337;
        boolean running = true;
        DataOutputStream dData = null;
        DataInputStream dinData = null;
        ObjectOutputStream doutObject = null;
        ObjectInputStream dinObject = null;
        Socket clientSocket = null;
        int numberOfHandlersConnected = 0;

        try {
            clientSocket = new Socket(localhost, PORT);
            doutObject = new ObjectOutputStream(clientSocket.getOutputStream());
            dinObject = new ObjectInputStream(clientSocket.getInputStream());
            dData = new DataOutputStream(clientSocket.getOutputStream());
            dinData = new DataInputStream(clientSocket.getInputStream());

            // Receive data package from Server ================================
            DataPackage datapackage = (DataPackage) dinObject.readUnshared();
            for (int i = 0; i < datapackage.getAmount(); i++){
                Agent a = new Agent(datapackage.getID());
                agentList.add(a);
            }
            this.handlerID = datapackage.getID();
            System.out.println("Amount of agents created: " + agentList.size());
            oppositeXY = new float[datapackage.getAmountOfOtherAgents() * 2];
            //==================================================================
            // Initiate arrays to hold positions for the first time.
            fillList(agentList);

            while (running) {

                Thread.sleep(50);
                if(numberOfHandlersConnected == 2) {
                    checkCollisionSameGroup();
                    checkCollisionOtherGroup();
                    fillList(agentList); // New data to be sent.
                    dData.writeUTF(getHandlerID()); // handlerID of agent handler.
                    doutObject.writeUnshared(xyPositions);
                    // Receive positions of the other agents:
                    oppositeXY = (float[]) dinObject.readUnshared();
                    /*System.out.println("X: " + oppositeXY[0] + " Y: " +
                            oppositeXY[1]);*/
                }
                else {
                    numberOfHandlersConnected = dinData.readInt();
                    System.out.println("Number of handlers: " +
                            numberOfHandlersConnected);
                }
            }
            closeConnections(clientSocket, doutObject, dinObject, dData, dinData);
            System.out.println("######## AgentHandler is Terminating ########");

        } catch (IOException e) {
            closeConnections(clientSocket, doutObject, dinObject, dData, dinData);
            System.err.println("Error - AgentHandler: " + e);
            e.printStackTrace();
        }  catch (ClassNotFoundException e) {
            closeConnections(clientSocket, doutObject, dinObject, dData, dinData);
            System.err.println("Error - AgentHandler: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            closeConnections(clientSocket, doutObject, dinObject, dData, dinData);
            e.printStackTrace();
        }
    }

    /**
     * To close all the socket and data streams if an exceptions occurs or the
     * run() function's while loop has finished.
     *
     * @param socket The socket corresponding to its handler.
     * @param doutObj The object stream that the positions are sent through.
     * @param dinObj The object stream that receives positions from server.
     * @param dout A stream to send the ID of the handler to the server.
     * @param din A stream to receive data from server.
     *
     */
    private void closeConnections(Socket socket, ObjectOutputStream doutObj,
                                  ObjectInputStream dinObj,
                                  DataOutputStream dout,
                                  DataInputStream din){
        try{
            socket.close();
            doutObj.close();
            dinObj.close();
            dout.close();
            din.close();
        }catch(IOException e){
            System.err.println("Error - CloseConnection - AgentHandler: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Fills the position array with the current positions of all the agents
     * before updating them.
     * Also, before updating the positions, a collision check is made.
     *
     * @param agents A list containing all the agents a handler has.
     *
     */
    private void fillList(List<Agent> agents){
        int c = 0;
        float x;
        xyPositions = new float[agents.size() * 2];
        for(int i = 0; i < agents.size(); i++){

            x = agents.get(i).getPosX();

            // Check if goal reached.
            if(x < -10.0f){
                agents.get(i).setPosX(800.0f);
                agents.get(i).setPosY(ThreadLocalRandom.current().nextFloat()
                        * 450.f + 120.0f);
                agents.get(i).setGoalY(ThreadLocalRandom.current().nextFloat
                        () * 450.f + 120.0f);
            }

            // Check if goal reached.
            else if(x > 810.0f){
                agents.get(i).setPosX(0.0f);
                agents.get(i).setPosY(ThreadLocalRandom.current().nextFloat()
                        * 450.f + 120.0f);
                agents.get(i).setGoalY(ThreadLocalRandom.current().nextFloat
                        () * 450.f + 120.0f);
            }

            xyPositions[c] = agents.get(i).getPosX();
            xyPositions[c+1] = agents.get(i).getPosY();
            c += 2;
        }
    }


}
