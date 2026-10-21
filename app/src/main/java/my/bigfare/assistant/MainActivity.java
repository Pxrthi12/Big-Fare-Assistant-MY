package my.bigfare.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class MainActivity extends Activity {

    EditText fareInput, distanceInput;
    TextView resultText, totalText;
    double dailyTotal = 0;
    DecimalFormat money = new DecimalFormat("RM #,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 28, 28, 28);
        layout.setBackgroundColor(Color.rgb(245, 248, 250));

        TextView title = new TextView(this);
        title.setText("BIG FARE ASSISTANT");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(0, 130, 80));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 24);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Malaysia • E-Hailing Fare Calculator");
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 24);
        layout.addView(subtitle);

        fareInput = new EditText(this);
        fareInput.setHint("Enter trip fare (RM)");
        fareInput.setInputType(8194);
        layout.addView(fareInput, matchWidth());

        distanceInput = new EditText(this);
        distanceInput.setHint("Enter trip distance (km)");
        distanceInput.setInputType(8194);
        layout.addView(distanceInput, matchWidth());

        Button calculateButton = new Button(this);
        calculateButton.setText("CALCULATE FARE");
        layout.addView(calculateButton, matchWidth());

        resultText = new TextView(this);
        resultText.setTextSize(18);
        resultText.setTextColor(Color.DKGRAY);
        resultText.setPadding(0, 24, 0, 12);
        layout.addView(resultText);

        Button addButton = new Button(this);
        addButton.setText("ADD FARE TO DAILY TOTAL");
        layout.addView(addButton, matchWidth());

        totalText = new TextView(this);
        totalText.setText("Today's total: RM 0.00");
        totalText.setTextSize(20);
        totalText.setTextColor(Color.rgb(0, 130, 80));
        totalText.setPadding(0, 24, 0, 12);
        layout.addView(totalText);

        TextView minimum = new TextView(this);
        minimum.setText("Suggested minimum fare: RM 30.00\nVehicle profile: Proton Saga");
        minimum.setTextSize(15);
        minimum.setPadding(0, 16, 0, 12);
        layout.addView(minimum);

        calculateButton.setOnClickListener(v -> calculate(false));
        addButton.setOnClickListener(v -> calculate(true));

        setContentView(layout);
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void calculate(boolean addToTotal) {
        try {
            double fare = Double.parseDouble(fareInput.getText().toString().trim());
            double distance = Double.parseDouble(distanceInput.getText().toString().trim());

            if (fare < 0 || distance <= 0) {
                Toast.makeText(this, "Enter a valid fare and distance.", Toast.LENGTH_SHORT).show();
                return;
            }

            double perKm = fare / distance;
            resultText.setText(
                    "Fare: " + money.format(fare) +
                    "\nDistance: " + distance + " km" +
                    "\nFare per km: RM " + new DecimalFormat("0.00").format(perKm) +
                    (fare < 30 ? "\n⚠ Below your RM30 minimum target." : "\n✓ Meets your RM30 minimum target.")
            );

            if (addToTotal) {
                dailyTotal += fare;
                totalText.setText("Today's total: " + money.format(dailyTotal));
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter the fare and distance.", Toast.LENGTH_SHORT).show();
        }
    }
}
