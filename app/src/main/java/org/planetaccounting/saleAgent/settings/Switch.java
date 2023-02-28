package org.planetaccounting.saleAgent.settings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.databinding.ActivitySwitchBinding;

public class Switch extends AppCompatActivity {

    ActivitySwitchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_switch);
        Kontabiliteti.getKontabilitetiComponent().inject(this);

        binding.testSwitch.findViewById(R.id.test_switch);
        binding.print80mmSwitch.findViewById(R.id.print80mm_switch);

        //switchat by default jon t'checked
        binding.testSwitch.setChecked(true);
        binding.print80mmSwitch.setChecked(true);

        SharedPreferences sharedPreferences = getSharedPreferences("switchState", MODE_PRIVATE);
        SharedPreferences sharedPreferences1 = getSharedPreferences("switchState1", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor1 = sharedPreferences1.edit();

        binding.testSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //The switch is turned on
                    editor.putBoolean("switchState", true);
                    editor1.putBoolean("switchState1", true);
                    editor.apply();
                    editor1.apply();
                } else {
                    //The switch is turned off
                    editor.putBoolean("switchState", false);
                    editor1.putBoolean("switchState1",false);
                    editor.apply();
                    editor1.apply();
                }
            }
        });

    }
}