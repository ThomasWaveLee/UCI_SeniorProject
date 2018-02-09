package se.bitcraze.crazyfliecontrol2;


import android.graphics.Point;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Vector;
/**
 * Created by Paolo on 1/24/2018.
 */

public class Graph {
    private Vector<Node> nodeTable;
    private int numOfNodes;

    Graph(){
        numOfNodes = 5;
    }
    int getNumOfNodes(){
        return numOfNodes;
    }
    Vector<Node> getAllNodes(){
        return nodeTable;
    }
    /*
    In the future, this function can take an input floor plan
    and create a graph
    */
    Graph init(){
        /*Test map:

         .....................................[E]...................
         ......................................|....................
         .........................[C]---[B]---[D]...................
         ................................|..........................
         ...............................[A].........................

         */
        nodeTable = new Vector<Node>();
        /*Hard code the nodes*/
        nodeTable.addElement(new Node(0, "A", 0, 0));
        nodeTable.addElement(new Node(1, "B", 0, 5));
        nodeTable.addElement(new Node(2, "C", -5, 5));
        nodeTable.addElement(new Node(3, "D", 7, 5));
        nodeTable.addElement(new Node(4, "E", 7,10));

        /*Hard code the edges*/
        addEdge(nodeTable.elementAt(0), nodeTable.elementAt(1), 5, 90);
        addEdge(nodeTable.elementAt(1), nodeTable.elementAt(2), 5, 180);
        addEdge(nodeTable.elementAt(1), nodeTable.elementAt(3), 7, 0);
        addEdge(nodeTable.elementAt(3), nodeTable.elementAt(4), 5, 90);

        return this;
    }

    class Node implements Comparable<Node>{
        int ID;
        String name;
        boolean isDrawn;

        /*These are node coordinates. Used for drawing on canvas*/
        Point coordinate;

        /*This points to the parent after calculating Dijkstra*/
        Node parent;

        /*This is the distance to the parent*/
        double distToParent;

        /*This is the direction to the parent*/
        double directionToParent;

        /*This is the minimum distance from the source calculated
        by Dijkstra*/
        double minDistance;

        LinkedList<Node> adjList;
        ArrayList<Edge> edgeList;

        Node(int ID, String n, int x, int y){
            this.ID = ID;
            this.name = n;
            coordinate = new Point(x, y);
            isDrawn = false;
            edgeList = new ArrayList<Edge>();
            adjList = new LinkedList<Node>();
        }

        @Override
        public int compareTo(Node other){
            return Double.compare(minDistance,other.minDistance);
        }
    }

    class Edge{
        /*DISTANCE
        Unit distance between nodes
        */
        double distance;

        /* DIRECTION
        Degrees with respect to East. With
        East  -> 0 degrees
        North -> 90 degrees
        West  -> 180 degrees
        South -> 270 degrees

        Range: [0, 360)
        */
        double direction;

        Node target;

        Edge(double dist, double direction, Node dest){
            this.distance = dist;
            this.direction = direction;
            this.target = dest;
        }
    }

    private void addEdge(Node src, Node dest,
                         double dist,
                         double direction)
    {
        src.edgeList.add(new Edge(dist, direction, dest));
        dest.edgeList.add(new Edge(dist, (direction+180)%360, src));
    }

    private boolean validNodeID(int id){
        return id >= 0 && id < numOfNodes;
    }

    private String directions(Node dest){
        if(dest.parent == null)
            return "";
        else{
            return directions(dest.parent) + "\n"+
                    dest.parent.name + " -> " + dest.name +
                    " : " + dest.distToParent + ", " + dest.directionToParent;
        }
    }

    String getDirections(int srcNodeID, int destNodeID){
        return getDirections(getNode(srcNodeID), getNode(destNodeID));
    }

    String getDirections(String srcNodeName, String destNodeName){
        return getDirections(getNode(srcNodeName), getNode(destNodeName));
    }

    String getDirections(Node src, Node dest){
        /*Execute dijkstra*/
        try{
            dijkstra(src);
            return directions(dest);
        }catch(Exception e){
            return "Invalid input. Node not found!";
        }
    }

    private Node getNode(int ID){
        for(Node n: nodeTable){
            if(n.ID == ID)
                return n;
        }
        return null;
    }

    private Node getNode(String name){
        for(Node n: nodeTable){
            if(n.name.equals(name))
                return n;
        }
        return null;
    }

    private void dijkstra(Node src){
        PriorityQueue<Node> queue = new PriorityQueue<Node>();

        for(Node n: nodeTable){
            n.minDistance = Double.MAX_VALUE;
            n.parent = null;
            n.distToParent = Double.MAX_VALUE;
        }

        src.minDistance = 0;
        //Add the src to the queue
        queue.add(src);

        while(!queue.isEmpty()){
            //Take out the highest priority node
            Node u = queue.poll();
            for(Edge e: u.edgeList){
                //relax
                Double newDist = u.minDistance + e.distance;
                if(e.target.minDistance > newDist){
                    //update the distance to neighboring node
                    queue.remove(e.target);
                    e.target.minDistance = newDist;

                    //update prev node according to shortest path
                    e.target.parent = u;
                    e.target.distToParent = e.distance;
                    e.target.directionToParent = e.direction;

                    //update list of nodes with known paths to
                    e.target.adjList = new LinkedList<Node>(u.adjList);
                    e.target.adjList.add(u);

                    //add the neighboring node back to sort queue
                    queue.add(e.target);
                }
            }
        }
    }
}
