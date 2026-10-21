package my.bigfare.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText fareInput, distanceInput, fuelPriceInput, consumptionInput;
    private TextView resultText, totalText, targetText, fuelText;
    private SharedPreferences prefs;
    private double dailyTotal = 0.0;
    private static final double DAILY_TARGET = 400.0;
    private static final double MIN_FARE = 30.0;
    private final DecimalFormat money = new DecimalFormat("RM #,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("big_fare_assistant", MODE_PRIVATE);
        loadDailyTotal();

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 24, 28, 28);
        layout.setBackgroundColor(Color.rgb(245, 248, 250));
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("BIG FARE ASSISTANT");
        title.setTextSize(25);
        title.setTypeface(null, 1);
        title.setTextColor(Color.rgb(0, 130, 80));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 16, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Malaysia • E-Hailing Trip Helper");
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 20);
        layout.addView(subtitle);

        addLabel(layout, "TRIP FARE");
        fareInput = addInput(layout, "Enter trip fare (RM)", true);
        addLabel(layout, "TRIP DISTANCE");
        distanceInput = addInput(layout, "Enter trip distance (km)", true);

        Button calculateButton = addButton(layout, "CALCULATE FARE & FUEL");
        resultText = addResult(layout, "");
        fuelText = addResult(layout, "Estimated fuel cost and profit will appear here.");

        calculateButton.setOnClickListener(v -> calculate(false));

        Button addButton = addButton(layout, "ADD FARE TO TODAY'S TOTAL");
        addButton.setOnClickListener(v -> calculate(true));

        totalText = addResult(layout, "");
        targetText = addResult(layout, "");
        updateTotals();

        addLabel(layout, "FUEL SETTINGS");
        fuelPriceInput = addInput(layout, "Fuel price RM/L (default 2.05)", true);
        consumptionInput = addInput(layout, "Vehicle consumption L/100 km (default 7.0)", true);
        fuelPriceInput.setText(prefs.getString("fuel_price", "2.05"));
        consumptionInput.setText(prefs.getString("consumption", "7.0"));

        Button saveFuelButton = addButton(layout, "SAVE FUEL SETTINGS");
        saveFuelButton.setOnClickListener(v -> {
            if (validFuelSettings()) {
                prefs.edit()
                    .putString("fuel_price", fuelPriceInput.getText().toString().trim())
                    .putString("consumption", consumptionInput.getText().toString().trim())
                    .apply();
                Toast.makeText(this, "Fuel settings saved.", Toast.LENGTH_SHORT).show();
                calculate(false);
            }
        });

        TextView profile = addResult(layout,
            "Vehicle profile: Proton Saga\nMinimum fare target: RM30\nDaily earnings target: RM400\n\nFuel cost is an estimate. Actual consumption and fuel prices may vary.");
        profile.setTextSize(14);

        Button resetButton = addButton(layout, "RESET TODAY'S TOTAL");
        resetButton.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Reset today's earnings?")
            .setMessage("This will set today's saved total to RM0.00.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> {
                dailyTotal = 0.0;
                saveDailyTotal();
                updateTotals();
                Toast.makeText(this, "Today's total reset.", Toast.LENGTH_SHORT).show();
            }).show());

        setContentView(scroll);
    }

    private void loadDailyTotal() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDay = prefs.getString("saved_day", "");
        if (!today.equals(savedDay)) {
            dailyTotal = 0.0;
            prefs.edit().putString("saved_day", today).putString("daily_total", "0").apply();
        } else {
            try {
                dailyTotal = Double.parseDouble(prefs.getString("daily_total", "0"));
            } catch (NumberFormatException e) {
                dailyTotal = 0.0;
            }
        }
    }

    private void saveDailyTotal() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        prefs.edit().putString("saved_day", today)
            .putString("daily_total", Double.toString(dailyTotal)).apply();
    }

    private void addLabel(LinearLayout layout, String label) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(13);
        text.setTypeface(null, 1);
        text.setTextColor(Color.rgb(70, 80, 90));
        text.setPadding(0, 12, 0, 4);
        layout.addView(text);
    }

    private EditText addInput(LinearLayout layout, String hint, boolean decimal) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(decimal
            ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            : InputType.TYPE_CLASS_TEXT);
        layout.addView(input, matchWidth());
        return input;
    }

    private Button addButton(LinearLayout layout, String label) {
        Button button = new Button(this);
        button.setText(label);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = 8;
        params.bottomMargin = 4;
        layout.addView(button, params);
        return button;
    }

    private TextView addResult(LinearLayout layout, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(16);
        text.setTextColor(Color.rgb(40, 50, 60));
        text.setPadding(0, 12, 0, 12);
        layout.addView(text);
        return text;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private boolean validFuelSettings() {
        try {
            double price = Double.parseDouble(fuelPriceInput.getText().toString().trim());
            double consumption = Double.parseDouble(consumptionInput.getText().toString().trim());
            if (price <= 0 || consumption <= 0) throw new NumberFormatException();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Enter valid fuel price and consumption.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void calculate(boolean addToTotal) {
        try {
            double fare = Double.parseDouble(fareInput.getText().toString().trim());
            double distance = Double.parseDouble(distanceInput.getText().toString().trim());
            if (fare < 0 || distance <= 0) {
                Toast.makeText(this, "Enter a valid fare and distance greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }

            double perKm = fare / distance;
            double fuelPrice = Double.parseDouble(fuelPriceInput.getText().toString().trim());
            double consumption = Double.parseDouble(consumptionInput.getText().toString().trim());
            if (fuelPrice <= 0 || consumption <= 0) {
                Toast.makeText(this, "Fuel settings must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
            double fuelCost = distance * consumption / 100.0 * fuelPrice;
            double estimatedProfit = fare - fuelCost;

            resultText.setText("Fare: " + money.format(fare)
                + "\nDistance: " + new DecimalFormat("0.##").format(distance) + " km"
                + "\nFare per km: " + money.format(perKm)
                + (fare < MIN_FARE ? "\n⚠ Below your RM30 minimum target." : "\n✓ Meets your RM30 minimum target."));
            fuelText.setText("Estimated fuel cost: " + money.format(fuelCost)
                + "\nFare after estimated fuel only: " + money.format(estimatedProfit)
                + "\n(Not including tolls, maintenance, commission or other costs.)");

            if (addToTotal) {
                dailyTotal += fare;
                saveDailyTotal();
                updateTotals();
                Toast.makeText(this, "Fare added to today's total.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Enter fare, distance and valid fuel settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotals() {
        if (totalText != null) totalText.setText("Today's gross fares: " + money.format(dailyTotal));
        if (targetText != null) {
            double remaining = Math.max(0.0, DAILY_TARGET - dailyTotal);
            int percent = (int) Math.min(100, Math.round(dailyTotal / DAILY_TARGET * 100));
            targetText.setText("RM400 daily target: " + percent + "% reached"
                + "\nRemaining: " + money.format(remaining)
                + (dailyTotal >= DAILY_TARGET ? "\n✓ Daily target reached!" : ""));
        }
    }
                                       }
