package sample.chloe.theremin2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.app.Activity;

/**
 * Created by Chloe on 11/1/2015.
 */


public class activity2 extends Activity {

    Button button;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.in);
        addListenerOnButton();
        addListenerOnButton1();
    }

    public void addListenerOnButton(){
        final Context context = this;

        button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, credits.class);
                startActivity(intent);
            }
        });

    }

    public void addListenerOnButton1() {
        final Context context = this;

        button = (Button) findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });
    }

}



