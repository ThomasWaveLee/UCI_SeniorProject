package se.bitcraze.crazyfliecontrol2;

import java.util.Vector;


/**
 * Created by Paolo on 1/24/2018.
 */

public class Graph {

    private int[][] directionMatrix;//holds direction to face from one node to another;
    private int[][] adjMatrix;
    private int numberVertices;
    private final static int NO_PARENT = -1;
    private final static int NO_PATH =  Integer.MIN_VALUE;
    private Dijkstra d;

    Graph(){
        numberVertices = 5;
        adjMatrix = new int[][]{{       0,      5, NO_PATH, NO_PATH, NO_PATH},
                {       5,      0,       3,       3, NO_PATH},
                { NO_PATH,      3,       0, NO_PATH,       5},
                { NO_PATH,      3, NO_PATH,       0, NO_PATH},
                { NO_PATH,NO_PATH,       5, NO_PATH,       0}};
        directionMatrix = new int[][]{{       0,     90, NO_PATH, NO_PATH, NO_PATH},
                {     -90,      0,       0,     180, NO_PATH},
                { NO_PATH,      0,       0, NO_PATH,      90},
                { NO_PATH,    180, NO_PATH,       0, NO_PATH},
                { NO_PATH,NO_PATH,     -90, NO_PATH,       0}};
        d = new Dijkstra(numberVertices, adjMatrix);
    }

    private boolean validNode(int n){
        return n >= 0 && n < numberVertices;
    }

    String getDirections(int src, int dest){

        if(!validNode(src) || !validNode(dest))
            return "Invalid inputs";

        String  s    = new String(),
                prev = Integer.toString(src),
                next;
        d.execute(src);
        Vector<Integer> v = d.getDirectionsTo(dest);
        for(Integer i : v){
            s += "\n";
            next = i.toString(i);
            s += prev + " -> " + next +": ";
            s += Integer.toString(adjMatrix[Integer.parseInt(prev)][i]) + ", ";
            s += Integer.toString(directionMatrix[Integer.parseInt(prev)][i]);
            prev = next;
        }
        return s;
    }

    class Dijkstra{
        private int[] distance;
        private int numberOfVertices;
        private boolean[] visited;
        private int[][] copy;
        private int[] parent;
        private Vector<Integer> directions;

        Dijkstra(int n, int[][] graph)
        {
            numberOfVertices = n;
            visited = new boolean[numberOfVertices];
            distance = new int[numberOfVertices];
            parent = new int[numberOfVertices];
            directions = new Vector<Integer>(numberOfVertices);
            copy = new int[numberOfVertices][numberOfVertices];
            //Copy matrix
            for(int srcV = 0; srcV < numberOfVertices; srcV++){
                for(int destV = 0; destV < numberOfVertices; destV++){
                    copy[srcV][destV] = graph[srcV][destV];
                }
            }
        }

        int[] getDistances(){
            return distance;
        }

        Vector<Integer> getDirectionsTo(int dest){
            printPath(dest);
            return directions;
        }

        void execute(int src)
        {

            //Initialize arrays
            for(int i =0; i < numberOfVertices; i++){
                visited[i] = false;
                distance[i] = Integer.MAX_VALUE;
            }

            distance[src] = 0;

            //Start vertex has not parent
            parent[src] = NO_PARENT;

            for(int i = 0; i < numberOfVertices-1; i++){
                visitAdj(extractMin());
            }
        }

        private int extractMin()
        {
            //Find nearest vertex
            int min = Integer.MAX_VALUE, index = -1;

            for(int i = 0; i < numberOfVertices; i++){
                if(visited[i] ==  false && distance[i] <= min){
                    index = i;
                    min = distance[i];
                }
            }

            return index;
        }

        void visitAdj(int adj)
        {
            int newDistance;

            for(int dest = 0; dest < numberOfVertices; dest++){
                if(copy[adj][dest] != NO_PATH && visited[dest] == false){
                    newDistance = distance[adj] + copy[adj][dest];
                    if(distance[dest] > newDistance){
                        parent[dest] = adj;
                        distance[dest] = newDistance;
                    }
                }
            }
            visited[adj] = true;
        }

        //Create string of directions recursively
        void printPath(int dest)
        {
            //If it has no parent, it's the src vertex
            if(parent[dest] == NO_PARENT)
                return;

            printPath(parent[dest]);
            directions.addElement(dest);
        }
    }

}
