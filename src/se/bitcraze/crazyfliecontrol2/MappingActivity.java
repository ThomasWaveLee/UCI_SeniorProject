package se.bitcraze.crazyfliecontrol2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MappingActivity extends Activity{

    private Button mainButton;
    private Button calculateButton;
    private EditText startDestEntryText;
    private TextView directionsTextView;
    private Graph graph;
    private String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);

        graph = new Graph();

        directionsTextView = (TextView) findViewById(R.id.textView_directions);
        startDestEntryText = (EditText) findViewById(R.id.editText_startDest);

        calculateButton = (Button) findViewById(R.id.button_calculate);
        calculateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                directionsTextView.setText(" ");
                String s = startDestEntryText.getText().toString();
                String[] inputs = s.split(",");


                /*Compute direction vectors*/
                try {
                    result = graph.getDirections(Integer.parseInt(inputs[0]), Integer.parseInt(inputs[1]));
                }
                catch(Exception e){ result = "Invalid inputs. Keep format 'X,Y' ' " ;}
                finally{ directionsTextView.setText(result);}
            }
        });

        mainButton = (Button) findViewById(R.id.button_main);
        mainButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(MappingActivity.this, MainActivity.class);
                i.putExtra("result", result);
                startActivity(i);
                // setResult(Activity.RESULT_OK, i);
                finish();
            }
        });
    }
}
