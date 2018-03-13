package lightingtheway;


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

    public Graph(){
        //numOfNodes = 5+2;
        numOfNodes = 11+2;
    }

    public int getNumOfNodes(){
        return numOfNodes;
    }

    public Vector<Node> getAllNodes(){
        return nodeTable;
    }
    /*
    In the future, this function can take an input floor plan
    and create a graph
    */
    public Graph init(){
        /*Test map:

         .....................................[E]...................
         ......................................|....................
         .........................[C]---[B]---[D]...................
         ................................|..........................
         ...............................[A].........................

         */
        nodeTable = new Vector<Node>();
        /*Hard code the nodes*/
        nodeTable.addElement(new Node(0, "Drone", 0, 0));
        nodeTable.addElement(new Node(1, "User", 0, 0));
        nodeTable.addElement(new Node(2, "A", 0, 0));
        nodeTable.addElement(new Node(3, "B", 0, 5));
        nodeTable.addElement(new Node(4, "C", -5, 5));
        nodeTable.addElement(new Node(5, "D", 7, 5));
        nodeTable.addElement(new Node(6, "E", 7,10));

        /*Hard code the edges*/
        addEdge(nodeTable.elementAt(2), nodeTable.elementAt(3), 5, 90);
        addEdge(nodeTable.elementAt(3), nodeTable.elementAt(4), 5, 180);
        addEdge(nodeTable.elementAt(3), nodeTable.elementAt(5), 7, 0);
        addEdge(nodeTable.elementAt(5), nodeTable.elementAt(6), 5, 90);

        return this;
    }

    public Graph init_Apartment(){
        nodeTable = new Vector<Node>();
        /*Apartment map:
         -----------------------------------------------
         |.............................................|
         |.............................................|
         |...............[D]............[J]............|
         |................|..............|.............|
         |................|.......[G]---[H]------[K]...|
         |...[E]---------[C]------[F]..................|
         |................|............................|
         |................|............................|
         |.........[A]---[B]...........................|
         |.............................................|
         |.............................................|
         -----------------------------------------------
         */

        nodeTable.addElement(new Node(0, "Drone", 0, 0));
        nodeTable.addElement(new Node(1, "User", 0, 0));
        nodeTable.addElement(new Node(2, "Door", 0, 0));                // A
        nodeTable.addElement(new Node(3, "Path1", 1, 0));               // B
        nodeTable.addElement(new Node(4, "Path2", 1, 3));               // C
        nodeTable.addElement(new Node(5, "Kitchen", 1, 5));             // D
        nodeTable.addElement(new Node(6, "Room1", -2, 3));              // E
        nodeTable.addElement(new Node(7, "Path3", 3, 3));               // F
        nodeTable.addElement(new Node(8, "Path4", 3, 4));               // G
        nodeTable.addElement(new Node(9, "Room3", 5, 4));               // H
        nodeTable.addElement(new Node(10, "Path5", 5, 5));              // I
        nodeTable.addElement(new Node(11, "Shared Restroom", 5, 6));    // J
        nodeTable.addElement(new Node(12, "Room2", 7, 5));              // K

        addEdge(nodeTable.elementAt(2), nodeTable.elementAt(3), 1.2954, 0);
        addEdge(nodeTable.elementAt(3), nodeTable.elementAt(4), 2.8194, 90);
        addEdge(nodeTable.elementAt(4), nodeTable.elementAt(5), 2.286, 90);
        addEdge(nodeTable.elementAt(4), nodeTable.elementAt(6), 3.048, 180);
        addEdge(nodeTable.elementAt(4), nodeTable.elementAt(7), 2.0574, 180);
        addEdge(nodeTable.elementAt(7), nodeTable.elementAt(8), 0.381, 90);
        addEdge(nodeTable.elementAt(8), nodeTable.elementAt(9), 1.524, 0);
        addEdge(nodeTable.elementAt(9), nodeTable.elementAt(10), 0.762, 90);
        addEdge(nodeTable.elementAt(10), nodeTable.elementAt(11), 1.524, 90);
        addEdge(nodeTable.elementAt(10), nodeTable.elementAt(12), 2.032, 0);

        return this;
    }

    public class Node implements Comparable<Node>{
        int ID;
        public String name;
        boolean isDrawn;

        /*These are node coordinates. Used for drawing on canvas*/
        public Point coordinate;

        /*This points to the parent after calculating Dijkstra*/
        Node parent;

        /*This is the distance to the parent*/
        double distToParent;

        /*This is the direction to the parent*/
        double directionToParent;

        /*This is the minimum distance from the source calculated
        by Dijkstra*/
        double minDistance;

        public LinkedList<Node> adjList;
        public ArrayList<Edge> edgeList;

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

    public class Edge{
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

        public Node target;

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

    private void removeEdge(Node src, Node dest)
    {
        src.edgeList.remove(dest);
        dest.edgeList.remove(src);
    }

    /* methods for connecting and disconeecting User and Drone nodes into graph */
    public void connectDrone(String dest, double dist, double direction){
        addEdge(getNode("Drone"),getNode(dest),dist,direction);
    }

    public void disconnectDrone(String dest, double dist, double direction) {
        removeEdge(getNode("Drone"), getNode(dest));
    }

    public void clearDrone(){
        Node drone = getNode("Drone");
        drone.edgeList.clear();
        drone.adjList.clear();
        for (Node n: nodeTable){
            n.adjList.remove(drone);
        }
    }

    public void connectUser(String dest, double dist, double direction){
        addEdge(getNode("User"),getNode(dest),dist,direction);
    }

    public void disconnectUser(String dest, double dist, double direction) {
        removeEdge(getNode("User"), getNode(dest));
    }

    public void clearUser(){
        Node user = getNode("User");
        user.edgeList.clear();
        user.adjList.clear();
        for (Node n: nodeTable){
            n.adjList.remove(user);
        }

    }

    private boolean validNodeID(int id){
        return id >= 0 && id < numOfNodes;
    }

    private String directions(Node dest){
        if(dest.parent == null)
            return "";
        else{
            return directions(dest.parent) + "\n" +
                    dest.parent.name + " -> " + dest.name +
                    " : " + dest.distToParent + ", " + dest.directionToParent;
        }
    }

    public String getDirections(int srcNodeID, int destNodeID){
        return getDirections(getNode(srcNodeID), getNode(destNodeID));
    }

    public String getDirections(String srcNodeName, String destNodeName){
        return getDirections(getNode(srcNodeName), getNode(destNodeName));
    }

    public String getDirections(Node src, Node dest){
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
        String lowercase = name.toLowerCase();
        for(Node n: nodeTable){
            if((n.name.toLowerCase()).equals(lowercase))
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
