package se.bitcraze.crazyfliecontrol2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import lightingtheway.Graph;

public class MappingActivity extends Activity{

    private Button mainButton;
    private Button calculateButton;
    //private EditText startDestEntryText;
    private TextView directionsTextView;
    private Graph graph;
    private LinearLayout linearLayout;
    private MappingView mappingView;
    private String result = "";
    private Spinner spinnerSrc;
    private Spinner spinnerDest;
    private Vector<Graph.Node> nodeTable;
    private ArrayList<String> nodeNames;
    private ArrayAdapter<String> dataAdapter;
    private String src;
    private String dest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //graph = new Graph().init();
        graph = new Graph().init_Apartment();
        nodeTable = graph.getAllNodes();

        setContentView(R.layout.activity_mapping);

        mappingView = new MappingView(this);
        mappingView.setGraph(graph);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
        linearLayout.addView(mappingView);

        directionsTextView = (TextView) findViewById(R.id.textView_directions);
        //startDestEntryText = (EditText) findViewById(R.id.editText_startDest);

        calculateButton = (Button) findViewById(R.id.button_calculate);
        calculateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                directionsTextView.setText(" ");
                //String s = startDestEntryText.getText().toString();
                //String[] inputs = s.split(",");
                result = "";


                /*Compute direction vectors*/
                try {
                    //result = graph.getDirections(inputs[0], inputs[1]);
                    result = graph.getDirections(src, dest);
                }
                catch(Exception e){ result = e.getMessage();
                }
                finally{
                    //directionsTextView.setText(s + "\n" +result);
                    directionsTextView.setText(src + "," + dest + "\n" + result);
                }
            }
        });

        mainButton = (Button) findViewById(R.id.button_main);
        mainButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MappingActivity.this, MainActivity.class);
                intent.putExtra("result", result);
                setResult(Activity.RESULT_OK, intent);
                //startActivity(i);
                finish();
            }
        });

        //spinners
        nodeNames = new ArrayList<String>();

        //start at 2 to omit user/drone nodes, populate spinners with node names
        for(int i=2; i<graph.getNumOfNodes(); i++){
            nodeNames.add(nodeTable.get(i).name);
        }
        dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nodeNames);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSrc = (Spinner) findViewById(R.id.spinner_src);
        spinnerSrc.setAdapter(dataAdapter);
        spinnerSrc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                src = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerDest = (Spinner) findViewById(R.id.spinner_dest);
        spinnerDest.setAdapter(dataAdapter);
        spinnerDest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dest = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }
    Graph getGraph(){ return graph;}
}
