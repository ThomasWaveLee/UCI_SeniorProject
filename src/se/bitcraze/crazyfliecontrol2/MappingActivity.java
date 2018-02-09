package se.bitcraze.crazyfliecontrol2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MappingActivity extends Activity {

    private Button mainButton;
    private Button calculateButton;
    private EditText startDestEntryText;
    private TextView directionsTextView;
    private Graph graph;
    private LinearLayout linearLayout;
    private MappingView mappingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        graph = new Graph().init();
        setContentView(R.layout.activity_mapping);

        mappingView = new MappingView(this);
        mappingView.setGraph(graph);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
        linearLayout.addView(mappingView);

        directionsTextView = (TextView) findViewById(R.id.textView_directions);
        startDestEntryText = (EditText) findViewById(R.id.editText_startDest);

        calculateButton = (Button) findViewById(R.id.button_calculate);
        calculateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                directionsTextView.setText(" ");
                String s = startDestEntryText.getText().toString();
                String[] inputs = s.split(",");
                String result = "";
                /*Compute direction vectors*/
                try {
                    result = graph.getDirections(Integer.parseInt(inputs[0]), Integer.parseInt(inputs[1]));
                }
                catch(Exception e){ result = e.getMessage();
                }
                finally{ directionsTextView.setText(s + "\n" +result);}
            }
        });

        mainButton = (Button) findViewById(R.id.button_main);
        mainButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent();
                setResult(Activity.RESULT_OK, i);
                finish();
            }
        });
    }

    Graph getGraph(){ return graph;}
}
