package com.example.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication.model.Expense;
import com.example.myapplication.model.User;
import com.example.myapplication.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WhereDidItGo.db";
    private static final int DATABASE_VERSION = 2;

    // Table Users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_NAME = "full_name";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_CREATED_AT = "created_at";

    // Table Expenses
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_EXPENSE_ID = "expense_id";
    public static final String COLUMN_EXPENSE_USER_ID = "user_id";
    public static final String COLUMN_EXPENSE_AMOUNT = "amount";
    public static final String COLUMN_EXPENSE_CATEGORY = "category";
    public static final String COLUMN_EXPENSE_NOTE = "note";
    public static final String COLUMN_EXPENSE_DATE = "date";

    // Table Budgets
    public static final String TABLE_BUDGETS = "budgets";
    public static final String COLUMN_BUDGET_ID = "budget_id";
    public static final String COLUMN_BUDGET_USER_ID = "user_id";
    public static final String COLUMN_BUDGET_WEEKLY_LIMIT = "weekly_limit";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table Utilisateurs
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_NAME + " TEXT NOT NULL, "
                + COLUMN_USER_EMAIL + " TEXT UNIQUE NOT NULL, "
                + COLUMN_USER_PASSWORD + " TEXT NOT NULL, "
                + COLUMN_USER_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Table Dépenses
        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + " ("
                + COLUMN_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_EXPENSE_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_EXPENSE_AMOUNT + " REAL NOT NULL, "
                + COLUMN_EXPENSE_CATEGORY + " TEXT NOT NULL, "
                + COLUMN_EXPENSE_NOTE + " TEXT, "
                + COLUMN_EXPENSE_DATE + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + COLUMN_EXPENSE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);

        // Table Budgets
        String CREATE_BUDGETS_TABLE = "CREATE TABLE " + TABLE_BUDGETS + " ("
                + COLUMN_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_BUDGET_USER_ID + " INTEGER UNIQUE NOT NULL, "
                + COLUMN_BUDGET_WEEKLY_LIMIT + " REAL NOT NULL, "
                + "FOREIGN KEY(" + COLUMN_BUDGET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                + ")";
        db.execSQL(CREATE_BUDGETS_TABLE);

        // Insertion du compte de démonstration par défaut
        ContentValues demoUser = new ContentValues();
        demoUser.put(COLUMN_USER_NAME, "Étudiant Démo");
        demoUser.put(COLUMN_USER_EMAIL, "etudiant@test.com");
        demoUser.put(COLUMN_USER_PASSWORD, PasswordUtils.hashPassword("123456"));
        long demoUserId = db.insert(TABLE_USERS, null, demoUser);

        if (demoUserId != -1) {
            insertInitialSampleExpenses(db, (int) demoUserId);
        }
    }

    private void insertInitialSampleExpenses(SQLiteDatabase db, int userId) {
        ContentValues e1 = new ContentValues();
        e1.put(COLUMN_EXPENSE_USER_ID, userId);
        e1.put(COLUMN_EXPENSE_AMOUNT, 12.50);
        e1.put(COLUMN_EXPENSE_CATEGORY, "🍔 Repas / Resto");
        e1.put(COLUMN_EXPENSE_NOTE, "Menu étudiant Crous / Fast food");
        e1.put(COLUMN_EXPENSE_DATE, "Aujourd'hui");
        db.insert(TABLE_EXPENSES, null, e1);

        ContentValues e2 = new ContentValues();
        e2.put(COLUMN_EXPENSE_USER_ID, userId);
        e2.put(COLUMN_EXPENSE_AMOUNT, 24.00);
        e2.put(COLUMN_EXPENSE_CATEGORY, "🚌 Transports");
        e2.put(COLUMN_EXPENSE_NOTE, "Recharge carte Navigo / Bus");
        e2.put(COLUMN_EXPENSE_DATE, "Hier");
        db.insert(TABLE_EXPENSES, null, e2);

        ContentValues e3 = new ContentValues();
        e3.put(COLUMN_EXPENSE_USER_ID, userId);
        e3.put(COLUMN_EXPENSE_AMOUNT, 18.00);
        e3.put(COLUMN_EXPENSE_CATEGORY, "🍻 Soirées & Loisirs");
        e3.put(COLUMN_EXPENSE_NOTE, "Verre entre amis");
        e3.put(COLUMN_EXPENSE_DATE, "Il y a 2 jours");
        db.insert(TABLE_EXPENSES, null, e3);

        ContentValues b1 = new ContentValues();
        b1.put(COLUMN_BUDGET_USER_ID, userId);
        b1.put(COLUMN_BUDGET_WEEKLY_LIMIT, 100.00);
        db.insert(TABLE_BUDGETS, null, b1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Enregistre un nouvel utilisateur.
     */
    public boolean registerUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String hashedPassword = PasswordUtils.hashPassword(user.getPassword());

        values.put(COLUMN_USER_NAME, user.getFullName());
        values.put(COLUMN_USER_EMAIL, user.getEmail().trim().toLowerCase());
        values.put(COLUMN_USER_PASSWORD, hashedPassword);

        long result = db.insert(TABLE_USERS, null, values);

        if (result != -1) {
            ContentValues b = new ContentValues();
            b.put(COLUMN_BUDGET_USER_ID, (int) result);
            b.put(COLUMN_BUDGET_WEEKLY_LIMIT, 100.00);
            db.insert(TABLE_BUDGETS, null, b);
        }

        return result != -1;
    }

    /**
     * Vérifie si un e-mail est déjà utilisé.
     */
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email.trim().toLowerCase()};

        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            if (cursor != null) {
                exists = cursor.getCount() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return exists;
    }

    /**
     * Récupère l'ID utilisateur à partir de l'email.
     */
    public int getUserIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return -1;
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email.trim().toLowerCase()};
        Cursor cursor = null;
        int userId = -1;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID}, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                userId = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return userId;
    }

    /**
     * Vérifie les identifiants de connexion.
     */
    public User checkUserCredentials(String email, String rawPassword) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = PasswordUtils.hashPassword(rawPassword);

        String[] columns = {COLUMN_USER_ID, COLUMN_USER_NAME, COLUMN_USER_EMAIL};
        String selection = COLUMN_USER_EMAIL + " = ? AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email.trim().toLowerCase(), hashedPassword};

        Cursor cursor = null;
        User user = null;
        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(COLUMN_USER_ID);
                int nameIndex = cursor.getColumnIndex(COLUMN_USER_NAME);
                int emailIndex = cursor.getColumnIndex(COLUMN_USER_EMAIL);

                int userId = idIndex != -1 ? cursor.getInt(idIndex) : -1;
                String name = nameIndex != -1 ? cursor.getString(nameIndex) : "";
                String userEmail = emailIndex != -1 ? cursor.getString(emailIndex) : "";

                user = new User(userId, name, userEmail, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return user;
    }

    // --- GESTION DES DÉPENSES ---

    /**
     * Ajoute une nouvelle dépense pour l'utilisateur.
     */
    public boolean addExpense(int userId, double amount, String category, String note, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPENSE_USER_ID, userId);
        values.put(COLUMN_EXPENSE_AMOUNT, amount);
        values.put(COLUMN_EXPENSE_CATEGORY, category);
        values.put(COLUMN_EXPENSE_NOTE, note);
        values.put(COLUMN_EXPENSE_DATE, date);

        long result = db.insert(TABLE_EXPENSES, null, values);
        return result != -1;
    }

    /**
     * Récupère la liste de toutes les dépenses de l'utilisateur.
     */
    public List<Expense> getExpensesForUser(int userId) {
        List<Expense> expenseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_EXPENSE_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        String orderBy = COLUMN_EXPENSE_ID + " DESC";

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_EXPENSES, null, selection, selectionArgs, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex(COLUMN_EXPENSE_ID);
                    int amountIndex = cursor.getColumnIndex(COLUMN_EXPENSE_AMOUNT);
                    int categoryIndex = cursor.getColumnIndex(COLUMN_EXPENSE_CATEGORY);
                    int noteIndex = cursor.getColumnIndex(COLUMN_EXPENSE_NOTE);
                    int dateIndex = cursor.getColumnIndex(COLUMN_EXPENSE_DATE);

                    int id = idIndex != -1 ? cursor.getInt(idIndex) : 0;
                    double amount = amountIndex != -1 ? cursor.getDouble(amountIndex) : 0.0;
                    String category = categoryIndex != -1 ? cursor.getString(categoryIndex) : "";
                    String note = noteIndex != -1 ? cursor.getString(noteIndex) : "";
                    String date = dateIndex != -1 ? cursor.getString(dateIndex) : "";

                    expenseList.add(new Expense(id, userId, amount, category, note, date));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return expenseList;
    }

    /**
     * Supprime une dépense par son ID.
     */
    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COLUMN_EXPENSE_ID + " = ?", new String[]{String.valueOf(expenseId)});
        return rows > 0;
    }

    /**
     * Calcule la somme totale dépensée par l'utilisateur.
     */
    public double getTotalExpensesForUser(int userId) {
        double total = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_EXPENSE_USER_ID + " = ?";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    // --- GESTION DU BUDGET HEBDOMADAIRE ---

    /**
     * Définit ou met à jour le budget hebdomadaire.
     */
    public boolean setWeeklyBudget(int userId, double limit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUDGET_USER_ID, userId);
        values.put(COLUMN_BUDGET_WEEKLY_LIMIT, limit);

        long result = db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    /**
     * Récupère le budget hebdomadaire configuré pour l'utilisateur (par défaut 100€).
     */
    public double getWeeklyBudget(int userId) {
        double limit = 100.0;
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_BUDGET_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BUDGETS, new String[]{COLUMN_BUDGET_WEEKLY_LIMIT}, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                limit = cursor.getDouble(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return limit;
    }
}
