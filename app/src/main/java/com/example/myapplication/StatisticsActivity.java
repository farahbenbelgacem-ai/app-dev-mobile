package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.model.Expense;
import com.example.myapplication.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialCardView cardHealth;
    private TextView tvHealthStatus, tvDailyAvg, tvFoodAmount, tvTransportAmount, tvPartiesAmount, tvStudentTip;
    private ProgressBar pbFood, pbTransport, pbParties;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        sessionManager = new SessionManager(this);
        databaseHelper = new DatabaseHelper(this);

        userId = sessionManager.getUserId();
        if (userId <= 0) {
            userId = databaseHelper.getUserIdByEmail(sessionManager.getUserEmail());
        }

        initViews();
        loadStatistics();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_stats);
        cardHealth = findViewById(R.id.card_health);
        tvHealthStatus = findViewById(R.id.tv_health_status);
        tvDailyAvg = findViewById(R.id.tv_daily_avg);

        tvFoodAmount = findViewById(R.id.tv_food_amount);
        tvTransportAmount = findViewById(R.id.tv_transport_amount);
        tvPartiesAmount = findViewById(R.id.tv_parties_amount);
        tvStudentTip = findViewById(R.id.tv_student_tip);

        pbFood = findViewById(R.id.pb_food);
        pbTransport = findViewById(R.id.pb_transport);
        pbParties = findViewById(R.id.pb_parties);

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStatistics() {
        double totalSpent = databaseHelper.getTotalExpensesForUser(userId);
        double weeklyLimit = databaseHelper.getWeeklyBudget(userId);

        if (weeklyLimit <= 0) weeklyLimit = 100.0;

        // Health Status Indicator
        double ratio = totalSpent / weeklyLimit;
        if (ratio < 0.7) {
            cardHealth.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
            cardHealth.setStrokeColor(Color.parseColor("#A7F3D0"));
            tvHealthStatus.setTextColor(Color.parseColor("#065F46"));
            tvHealthStatus.setText("🟢 Gestion Excellente !");
            tvStudentTip.setText("Super boulot ! Tu gères ton budget comme un pro. Pense à mettre quelques euros de côté !");
        } else if (ratio < 1.0) {
            cardHealth.setCardBackgroundColor(Color.parseColor("#FEF3C7"));
            cardHealth.setStrokeColor(Color.parseColor("#FDE68A"));
            tvHealthStatus.setTextColor(Color.parseColor("#92400E"));
            tvHealthStatus.setText("🟡 Budget sous Vigilance");
            tvStudentTip.setText("Attention aux petites dépenses imprévues de fin de semaine. Privilégie le Crous et les repas maison.");
        } else {
            cardHealth.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
            cardHealth.setStrokeColor(Color.parseColor("#FCA5A5"));
            tvHealthStatus.setTextColor(Color.parseColor("#991B1B"));
            tvHealthStatus.setText("🔴 Zone Rouge (Dépassement)");
            tvStudentTip.setText("Alerte ! Essaie de faire une pause sur les livraisons et les sorties pendant quelques jours pour rééquilibrer.");
        }

        // Daily average
        double dailyAvg = totalSpent / 7.0;
        tvDailyAvg.setText(String.format(Locale.FRANCE, "Dépense moyenne : %.2f € / jour", dailyAvg));

        // Category breakdown calculation
        List<Expense> expenses = databaseHelper.getExpensesForUser(userId);
        double foodTotal = 0.0, transportTotal = 0.0, partiesTotal = 0.0;

        for (Expense e : expenses) {
            String cat = e.getCategory();
            if (cat.contains("Repas") || cat.contains("🍔")) {
                foodTotal += e.getAmount();
            } else if (cat.contains("Transport") || cat.contains("🚌")) {
                transportTotal += e.getAmount();
            } else if (cat.contains("Soirée") || cat.contains("🍻")) {
                partiesTotal += e.getAmount();
            }
        }

        double maxCategory = Math.max(1.0, totalSpent);

        int foodPct = (int) Math.round((foodTotal / maxCategory) * 100);
        int transportPct = (int) Math.round((transportTotal / maxCategory) * 100);
        int partiesPct = (int) Math.round((partiesTotal / maxCategory) * 100);

        pbFood.setProgress(foodPct);
        tvFoodAmount.setText(String.format(Locale.FRANCE, "%.2f € (%d%%)", foodTotal, foodPct));

        pbTransport.setProgress(transportPct);
        tvTransportAmount.setText(String.format(Locale.FRANCE, "%.2f € (%d%%)", transportTotal, transportPct));

        pbParties.setProgress(partiesPct);
        tvPartiesAmount.setText(String.format(Locale.FRANCE, "%.2f € (%d%%)", partiesTotal, partiesPct));
    }
}
