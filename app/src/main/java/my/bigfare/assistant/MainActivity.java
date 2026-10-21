package my.bigfare.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private EditText fareInput, pickupInput, tripDistanceInput, minutesInput, fuelPriceInput, consumptionInput;
    private TextView resultText, fuelText, totalText, targetText, reportText;
    private double dailyTotal;
    private double dailyFuel;
    private static final double DEFAULT_MIN_FARE = 30.0;
    private static final double DEFAULT_DAILY_TARGET = 400.0;
    private final DecimalFormat money = new DecimalFormat("RM #,##0.00");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("big_fare_assistant", MODE_PRIVATE);
        loadToday();
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 28);
        root.setBackgroundColor(Color.rgb(245, 248, 250));
        scroll.addView(root);

        TextView title = label("BIG FARE ASSISTANT V3", 24, Color.rgb(0, 130, 80), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = label("Malaysia • Trip decision helper", 14, Color.DKGRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 16);
        root.addView(subtitle);

        section(root, "TRIP DETAILS");
        fareInput = input(root, "Offered fare (RM)", true);
        pickupInput = input(root, "Pickup distance (km)", true);
        tripDistanceInput = input(root, "Passenger trip distance (km)", true);
        minutesInput = input(root, "Estimated total time (minutes)", true);

        section(root, "SETTINGS");
        fuelPriceInput = input(root, "Fuel price RM/L", true);
        consumptionInput = input(root, "Fuel consumption L/100 km", true);
        fuelPriceInput.setText(prefs.getString("fuel_price", "2.05"));
        consumptionInput.setText(prefs.getString("consumption", "7.0"));

        Button saveSettings = button("SAVE SETTINGS");
        root.addView(saveSettings);
        saveSettings.setOnClickListener(v -> saveSettings());

        Button evaluate = button("EVALUATE TRIP");
        root.addView(evaluate);
        resultText = output(root, "");
        fuelText = output(root, "");
        evaluate.setOnClickListener(v -> evaluateTrip());

        Button addFare = button("ADD OFFERED FARE TO DAILY TOTAL");
        root.addView(addFare);
        addFare.setOnClickListener(v -> addFare());

        section(root, "DAILY TARGET & REPORT");
        totalText = output(root, "");
        targetText = output(root, "");
        reportText = output(root, "");
        updateReport();

        Button reset = button("RESET TODAY'S TOTAL");
        root.addView(reset);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Reset today's totals?")
            .setMessage("This clears today's gross fare and estimated fuel totals.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (d, w) -> {
                dailyTotal = 0;
                dailyFuel = 0;
                saveToday();
                updateReport();
            }).show());

        section(root, "FLOATING ASSISTANT");
        TextView note = output(root, "Optional quick-access floating button. Android will ask you to allow display over other apps. This does not automatically read Grab/Bolt offers.");
        root.addView(note);
        Button floating = button("ENABLE FLOATING QUICK ACCESS");
        root.addView(floating);
        floating.setOnClickListener(v -> enableFloating());

        TextView privacy = output(root, "Privacy: trip calculations and totals are stored locally on this phone. No location or Grab/Bolt screen-reading permission is requested by this version.");
        privacy.setTextSize(13);
        root.addView(privacy);

        setContentView(scroll);
    }

    private void loadToday() {
        String today = todayKey();
        if (!today.equals(prefs.getString("saved_day", ""))) {
            dailyTotal = 0;
            dailyFuel = 0;
            prefs.edit().putString("saved_day", today).putString("daily_total", "0").putString("daily_fuel", "0").apply();
        } else {
            dailyTotal = readDouble("daily_total", 0);
            dailyFuel = readDouble("daily_fuel", 0);
        }
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private double readDouble(String key, double fallback) {
        try { return Double.parseDouble(prefs.getString(key, Double.toString(fallback))); }
        catch (Exception e) { return fallback; }
    }

    private void saveToday() {
        prefs.edit().putString("saved_day", todayKey())
            .putString("daily_total", Double.toString(dailyTotal))
            .putString("daily_fuel", Double.toString(dailyFuel)).apply();
    }

    private void section(LinearLayout root, String text) {
        TextView t = label(text, 15, Color.rgb(0, 110, 75), true);
        t.setPadding(0, 18, 0, 6);
        root.addView(t);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private EditText input(LinearLayout root, String hint, boolean decimal) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setInputType(decimal ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        root.addView(e, matchWidth());
        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private TextView output(LinearLayout root, String text) {
        TextView t = label(text, 16, Color.rgb(40, 50, 60), false);
        t.setPadding(0, 10, 0, 10);
        root.addView(t);
        return t;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void saveSettings() {
        try {
            double p = Double.parseDouble(fuelPriceInput.getText().toString().trim());
            double c = Double.parseDouble(consumptionInput.getText().toString().trim());
            if (p <= 0 || c <= 0) throw new NumberFormatException();
            prefs.edit().putString("fuel_price", Double.toString(p)).putString("consumption", Double.toString(c)).apply();
            Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Enter a valid fuel price and consumption.", Toast.LENGTH_SHORT).show();
        }
    }

    private void evaluateTrip() {
        try {
            double fare = number(fareInput);
            double pickup = number(pickupInput);
            double trip = number(tripDistanceInput);
            double minutes = number(minutesInput);
            double fuelPrice = number(fuelPriceInput);
            double consumption = number(consumptionInput);
            if (fare < 0 || pickup < 0 || trip <= 0 || minutes <= 0 || fuelPrice <= 0 || consumption <= 0)
                throw new NumberFormatException();

            double totalKm = pickup + trip;
            double fuelCost = totalKm * consumption / 100.0 * fuelPrice;
            double afterFuel = fare - fuelCost;
            double perKm = fare / totalKm;
            double hourlyGross = fare / (minutes / 60.0);
            double minFare = readDouble("minimum_fare", DEFAULT_MIN_FARE);
            double target = readDouble("daily_target", DEFAULT_DAILY_TARGET);
            String verdict;
            if (fare < minFare) verdict = "LOW FARE: below your minimum fare setting.";
            else if (afterFuel < 0) verdict = "AVOID / REVIEW: estimated fuel cost exceeds fare.";
            else if (perKm < 1.0) verdict = "REVIEW: gross fare per total km is below RM1.00.";
            else verdict = "MEETS BASIC RULES: check traffic, tolls, commission and pickup time before deciding.";

            resultText.setText("Trip check: " + verdict
                + "\nTotal driving distance: " + fmt(totalKm) + " km"
                + "\nGross fare per total km: " + money.format(perKm)
                + "\nGross fare per hour (time estimate): " + money.format(hourlyGross)
                + "\nDaily target setting: " + money.format(target));
            fuelText.setText("Estimated fuel cost: " + money.format(fuelCost)
                + "\nFare after estimated fuel: " + money.format(afterFuel)
                + "\nExcludes platform commission, tolls, parking, maintenance and dead time.");
        } catch (Exception e) {
            Toast.makeText(this, "Fill in all trip fields with valid numbers.", Toast.LENGTH_SHORT).show();
        }
    }

    private double number(EditText e) {
        return Double.parseDouble(e.getText().toString().trim());
    }

    private String fmt(double d) { return new DecimalFormat("0.##").format(d); }

    private void addFare() {
        try {
            double fare = number(fareInput);
            double pickup = number(pickupInput);
            double trip = number(tripDistanceInput);
            double price = number(fuelPriceInput);
            double consumption = number(consumptionInput);
            if (fare < 0 || pickup < 0 || trip <= 0 || price <= 0 || consumption <= 0) throw new NumberFormatException();
            dailyTotal += fare;
            dailyFuel += (pickup + trip) * consumption / 100.0 * price;
            saveToday();
            updateReport();
            Toast.makeText(this, "Fare added to today's gross total.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Fill fare, pickup distance, trip distance and valid fuel settings first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReport() {
        if (totalText == null) return;
        double target = readDouble("daily_target", DEFAULT_DAILY_TARGET);
        double minFare = readDouble("minimum_fare", DEFAULT_MIN_FARE);
        totalText.setText("Today's gross fares: " + money.format(dailyTotal)
            + "\nEstimated fuel total: " + money.format(dailyFuel)
            + "\nFare after estimated fuel: " + money.format(dailyTotal - dailyFuel));
        targetText.setText("Daily target: " + money.format(target)
            + "\nProgress: " + Math.min(100, Math.round(dailyTotal / target * 100)) + "%"
            + "\nRemaining: " + money.format(Math.max(0, target - dailyTotal))
            + "\nMinimum fare setting: " + money.format(minFare));
        reportText.setText("Today's report\nGross: " + money.format(dailyTotal)
            + "\nFuel estimate: " + money.format(dailyFuel)
            + "\nAfter fuel only: " + money.format(dailyTotal - dailyFuel));
    }

    private void enableFloating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Allow display over other apps, then return to Big Fare Assistant.", Toast.LENGTH_LONG).show();
            return;
        }
        showFloatingQuickAccess();
    }

    private void showFloatingQuickAccess() {
        // Safe starter: shows an in-app dialog quick-access panel; a persistent cross-app overlay needs a foreground service.
        new AlertDialog.Builder(this)
            .setTitle("Floating Assistant Quick Access")
            .setMessage("Overlay permission is available. This starter build does not yet run a persistent floating button over Grab/Bolt. It does not read their screens.")
            .setPositiveButton("OK", null)
            .show();
    }
}
