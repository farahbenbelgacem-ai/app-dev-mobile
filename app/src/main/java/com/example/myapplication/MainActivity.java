package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ExpenseAdapter;
import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.model.Expense;
import com.example.myapplication.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseDeleteListener {

    private TextView tvWelcomeName, tvBudgetSummary, tvBudgetPercentage, tvAlertText, tvCategoryBreakdown, tvEmptyExpenses;
    private ImageButton btnLogoutHeader;
    private ProgressBar progressBarBudget;
    private MaterialCardView cardAlert;
    private MaterialButton btnAddExpense, btnSetBudget, btnViewStats, btnCompleteChallenge;
    private RecyclerView rvExpenses;

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;
    private ExpenseAdapter expenseAdapter;

    private int userId;
    private boolean isChallengeCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si l'utilisateur n'est pas connecté, redirection vers la page de connexion
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        userId = sessionManager.getUserId();
        if (userId <= 0) {
            userId = databaseHelper.getUserIdByEmail(sessionManager.getUserEmail());
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadDashboardData();
    }

    private void initViews() {
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        tvBudgetSummary = findViewById(R.id.tv_budget_summary);
        tvBudgetPercentage = findViewById(R.id.tv_budget_percentage);
        tvAlertText = findViewById(R.id.tv_alert_text);
        tvCategoryBreakdown = findViewById(R.id.tv_category_breakdown);
        tvEmptyExpenses = findViewById(R.id.tv_empty_expenses);

        btnLogoutHeader = findViewById(R.id.btn_logout);
        progressBarBudget = findViewById(R.id.progress_bar_budget);
        cardAlert = findViewById(R.id.card_alert);

        btnAddExpense = findViewById(R.id.btn_add_expense);
        btnSetBudget = findViewById(R.id.btn_set_budget);
        btnViewStats = findViewById(R.id.btn_view_stats);
        btnCompleteChallenge = findViewById(R.id.btn_complete_challenge);

        rvExpenses = findViewById(R.id.rv_expenses);

        String userName = sessionManager.getUserName();
        tvWelcomeName.setText(getString(R.string.welcome_student, userName));
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter(this);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
    }

    private void setupListeners() {
        btnLogoutHeader.setOnClickListener(v -> performLogout());
        btnAddExpense.setOnClickListener(v -> showAddExpenseDialog());
        btnSetBudget.setOnClickListener(v -> showSetBudgetDialog());
        btnViewStats.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        btnCompleteChallenge.setOnClickListener(v -> toggleChallenge());
    }

    private void loadDashboardData() {
        double totalSpent = databaseHelper.getTotalExpensesForUser(userId);
        double weeklyLimit = databaseHelper.getWeeklyBudget(userId);

        if (weeklyLimit <= 0) {
            weeklyLimit = 100.0;
        }

        int percentage = (int) Math.min(100, Math.round((totalSpent / weeklyLimit) * 100));

        tvBudgetSummary.setText(String.format(Locale.FRANCE, "%.2f € / %.2f €", totalSpent, weeklyLimit));
        tvBudgetPercentage.setText(percentage + "%");
        progressBarBudget.setProgress(percentage);

        // Alert Status logic
        if (totalSpent >= weeklyLimit) {
            cardAlert.setCardBackgroundColor(Color.parseColor("#FEE2E2")); // Red light
            cardAlert.setStrokeColor(Color.parseColor("#FCA5A5"));
            tvAlertText.setTextColor(Color.parseColor("#991B1B"));
            tvAlertText.setText(String.format(Locale.FRANCE, "🚨 ALERTE ! Tu as dépassé ton budget de +%.2f € !", (totalSpent - weeklyLimit)));
        } else if (percentage >= 80) {
            cardAlert.setCardBackgroundColor(Color.parseColor("#FEF3C7")); // Orange light
            cardAlert.setStrokeColor(Color.parseColor("#FDE68A"));
            tvAlertText.setTextColor(Color.parseColor("#92400E"));
            tvAlertText.setText(String.format(Locale.FRANCE, "⚠️ ATTENTION ! Tu as consommé %d%% de ton budget hebdomadaire !", percentage));
        } else {
            cardAlert.setCardBackgroundColor(Color.parseColor("#ECFDF5")); // Green light
            cardAlert.setStrokeColor(Color.parseColor("#A7F3D0"));
            tvAlertText.setTextColor(Color.parseColor("#065F46"));
            tvAlertText.setText("🟢 Ton budget est parfaitement sous contrôle !");
        }

        // Load Expenses List
        List<Expense> expenses = databaseHelper.getExpensesForUser(userId);
        expenseAdapter.setExpenses(expenses);

        if (expenses.isEmpty()) {
            tvEmptyExpenses.setVisibility(View.VISIBLE);
            rvExpenses.setVisibility(View.GONE);
        } else {
            tvEmptyExpenses.setVisibility(View.GONE);
            rvExpenses.setVisibility(View.VISIBLE);
        }

        // Category Breakdown Calculation
        updateCategoryBreakdown(expenses);
    }

    private void updateCategoryBreakdown(List<Expense> expenses) {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory();
            double current = categoryTotals.containsKey(cat) ? categoryTotals.get(cat) : 0.0;
            categoryTotals.put(cat, current + e.getAmount());
        }

        if (categoryTotals.isEmpty()) {
            tvCategoryBreakdown.setText("Aucune dépense enregistrée cette semaine.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(String.format(Locale.FRANCE, "%.2f €", entry.getValue())).append("  |  ");
        }

        String breakdownStr = sb.toString();
        if (breakdownStr.endsWith("  |  ")) {
            breakdownStr = breakdownStr.substring(0, breakdownStr.length() - 5);
        }

        tvCategoryBreakdown.setText(breakdownStr);
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);
        builder.setView(view);

        TextInputEditText etAmount = view.findViewById(R.id.et_expense_amount);
        Spinner spinnerCategory = view.findViewById(R.id.spinner_expense_category);
        TextInputEditText etNote = view.findViewById(R.id.et_expense_note);
        TextInputEditText etDate = view.findViewById(R.id.et_expense_date);

        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_expense);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_expense);

        // Populate Categories Spinner
        String[] categories = new String[]{
                "🍔 Repas / Resto",
                "🚌 Transports",
                "🍻 Soirées & Loisirs",
                "📚 Cours & Fournitures",
                "📱 Abonnements & Forfaits",
                "🛒 Courses / Supermarché",
                "💡 Autre"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        // Default Date: Today
        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(new Date());
        etDate.setText(todayDate);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "🍔 Repas / Resto";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
            String date = etDate.getText() != null ? etDate.getText().toString().trim() : todayDate;

            if (TextUtils.isEmpty(amountStr)) {
                etAmount.setError("Veuillez entrer un montant");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                etAmount.setError("Montant invalide");
                return;
            }

            boolean isAdded = databaseHelper.addExpense(userId, amount, category, note, date);
            if (isAdded) {
                Toast.makeText(this, "Dépense de " + amountStr + " € enregistrée ! 💰", Toast.LENGTH_SHORT).show();
                loadDashboardData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Erreur lors de l'enregistrement de la dépense.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showSetBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null);
        builder.setView(view);

        TextInputEditText etLimit = view.findViewById(R.id.et_budget_limit);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_budget);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_budget);

        double currentLimit = databaseHelper.getWeeklyBudget(userId);
        etLimit.setText(String.format(Locale.FRANCE, "%.2f", currentLimit));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String limitStr = etLimit.getText() != null ? etLimit.getText().toString().trim() : "";
            if (TextUtils.isEmpty(limitStr)) {
                etLimit.setError("Veuillez entrer une limite");
                return;
            }

            double limit;
            try {
                limit = Double.parseDouble(limitStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                etLimit.setError("Limite invalide");
                return;
            }

            boolean isUpdated = databaseHelper.setWeeklyBudget(userId, limit);
            if (isUpdated) {
                Toast.makeText(this, "Nouveau budget défini à " + limitStr + " € 🎯", Toast.LENGTH_SHORT).show();
                loadDashboardData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour du budget.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void toggleChallenge() {
        isChallengeCompleted = !isChallengeCompleted;
        if (isChallengeCompleted) {
            btnCompleteChallenge.setText("🎉 Défi Relevé avec Succès ! (Badge Débloqué)");
            btnCompleteChallenge.setBackgroundColor(Color.parseColor("#059669"));
            Toast.makeText(this, "Bravo ! Tu as relevé ton défi d'économies de la semaine 🏆🎉", Toast.LENGTH_LONG).show();
        } else {
            btnCompleteChallenge.setText("🏆 Relever le défi (-50% Livraisons)");
            btnCompleteChallenge.setBackgroundColor(Color.parseColor("#10B981"));
        }
    }

    @Override
    public void onDeleteExpense(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la dépense")
                .setMessage("Es-tu sûr de vouloir supprimer la dépense '" + expense.getCategory() + "' (" + expense.getAmount() + " €) ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    boolean isDeleted = databaseHelper.deleteExpense(expense.getId());
                    if (isDeleted) {
                        Toast.makeText(this, "Dépense supprimée.", Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    } else {
                        Toast.makeText(this, "Impossible de supprimer la dépense.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void performLogout() {
        sessionManager.logoutUser();
        Toast.makeText(this, "Vous avez été déconnecté avec succès.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
