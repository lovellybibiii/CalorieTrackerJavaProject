import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector {
    private Connection connection;
    private boolean connected = false;

    // Constructor
    public DatabaseConnector(Connection connection) throws SQLException {
        this.connection = connection;
        if (connection != null && !connection.isClosed()) {
            this.connected = true;
            initializeDatabase();
        }
    }

    // Initialize database tables
    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // Create foods table if not exists
            String createFoodsTable = "CREATE TABLE IF NOT EXISTS foods (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "calories INT NOT NULL, " +
                    "UNIQUE KEY unique_name (name)" +
                    ")";
            stmt.execute(createFoodsTable);

            // Create meals table if not exists
            String createMealsTable = "CREATE TABLE IF NOT EXISTS meals (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "food_id INT NULL, " +
                    "food_name VARCHAR(100) NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "calories INT NOT NULL, " +
                    "category VARCHAR(50), " +
                    "meal_type VARCHAR(20) NOT NULL, " +
                    "meal_date DATE NOT NULL DEFAULT (CURDATE()), " +
                    "meal_time TIME DEFAULT (CURTIME()), " +
                    "user_id INT DEFAULT 1" +
                    ")";
            stmt.execute(createMealsTable);

            // Create user_settings table if not exists
            String createUserSettingsTable = "CREATE TABLE IF NOT EXISTS user_settings (" +
                    "user_id INT PRIMARY KEY, " +
                    "user_name VARCHAR(50) NOT NULL, " +
                    "age INT, " +
                    "gender VARCHAR(10), " +
                    "weight DECIMAL(5,2), " +
                    "height DECIMAL(5,2), " +
                    "activity_level VARCHAR(20), " +
                    "daily_calorie_goal INT DEFAULT 2000" +
                    ")";
            stmt.execute(createUserSettingsTable);

            // Check if foods table is empty
            String countQuery = "SELECT COUNT(*) as count FROM foods";
            ResultSet rs = stmt.executeQuery(countQuery);
            if (rs.next() && rs.getInt("count") == 0) {
                insertSampleFoods();
            }

            // Insert default user
            String insertUser = "INSERT IGNORE INTO user_settings (user_id, user_name, daily_calorie_goal) " +
                    "VALUES (1, 'Default User', 2000)";
            stmt.executeUpdate(insertUser);

            System.out.println("Database initialized successfully");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    // Insert sample foods data
    private void insertSampleFoods() {
        String[] foods = {
                "('Apple', 52),",
                "('Banana', 89),",
                "('Orange', 47),",
                "('Grapes', 69),",
                "('Tomato', 18),",
                "('Cucumber', 15),",
                "('Boiled Potato', 87),",
                "('Cooked White Rice', 130),",
                "('Boiled Pasta', 131),",
                "('White Bread', 265),",
                "('Boiled Egg', 155),",
                "('Grilled Chicken', 239),",
                "('Cooked Beef', 250),",
                "('Canned Tuna (Water)', 116),",
                "('Fava Beans', 110),",
                "('Cooked Lentils', 116),",
                "('Cottage Cheese', 98),",
                "('Low-fat Yogurt', 59),",
                "('Whole Milk', 61),",
                "('Olive Oil', 884),",
                "('Chocolate', 546),",
                "('French Fries', 312),",
                "('Pizza (100g slice)', 266),",
                "('Oats', 389),",
                "('Dried Fruits (Raisins)', 299),",
                "('Lettuce', 15),",
                "('Radish', 16),",
                "('Carrot', 41),",
                "('Boiled Corn', 96),",
                "('Mango', 60),",
                "('Dates', 282),",
                "('White Cheese', 250),",
                "('Mozzarella Cheese', 280),",
                "('Cheddar Cheese', 400),",
                "('Almonds', 575),",
                "('Peanuts', 567),",
                "('Cashews', 553),",
                "('Halva (Tahini Sweet)', 540),",
                "('Jam', 250),",
                "('Sugar', 387),",
                "('Chocolate Cake', 370),",
                "('Plain Biscuits', 450),",
                "('Honey', 304),",
                "('Corn Chips', 112),",
                "('Butter', 717),",
                "('Cooked Red Beans', 127),",
                "('Cooked Chickpeas', 164),",
                "('Salmon', 208),",
                "('Chicken Shawarma', 275),",
                "('Tomato Paste', 82),",
                "('Cooked Eggplant', 84),",
                "('Tuna in Oil', 198),",
                "('Cooked Brown Rice', 123),",
                "('Fried Eggplant', 245),",
                "('Cooked Zucchini', 17),",
                "('Cooked Spinach', 23),",
                "('Baked Sweet Potato', 86),",
                "('Brown Bread', 247),",
                "('Strawberries', 32),",
                "('Pear', 57),",
                "('Watermelon', 30),",
                "('Bell Pepper', 26),",
                "('Potato Wedges (Frozen/Fried)', 312)"
        };

        try (Statement stmt = connection.createStatement()) {
            StringBuilder insertQuery = new StringBuilder("INSERT INTO foods (name, calories) VALUES ");

            for (int i = 0; i < foods.length; i++) {
                insertQuery.append(foods[i]);
                if (i < foods.length - 1) {
                    insertQuery.append("\n");
                }
            }

            stmt.executeUpdate(insertQuery.toString());
            System.out.println("Sample foods inserted successfully");

        } catch (SQLException e) {
            System.err.println("Error inserting sample foods: " + e.getMessage());
        }
    }

    // Get all foods for search combo box
    public List<FoodItem> getAllFoods() {
        List<FoodItem> foods = new ArrayList<>();
        if (!connected) return foods;

        String sql = "SELECT name, calories FROM foods ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                foods.add(new FoodItem(
                        rs.getString("name"),
                        rs.getInt("calories")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading foods: " + e.getMessage());
        }
        return foods;
    }

    // Get count of foods in database
    public int getFoodCount() {
        if (!connected) return 0;

        String sql = "SELECT COUNT(*) as count FROM foods";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting food count: " + e.getMessage());
        }
        return 0;
    }

    // Save a new meal
    public int saveMeal(String foodName, int quantity, int calories, String category, String mealType) {
        if (!connected) return -1;

        String sql = "INSERT INTO meals (food_name, quantity, calories, category, meal_type, meal_date, meal_time) " +
                "VALUES (?, ?, ?, ?, ?, CURDATE(), CURTIME())";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, foodName);
            pstmt.setInt(2, quantity);
            pstmt.setInt(3, calories);
            pstmt.setString(4, category);
            pstmt.setString(5, mealType);
            pstmt.executeUpdate();

            // Get the generated ID
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;

        } catch (SQLException e) {
            System.err.println("Error saving meal: " + e.getMessage());
            return -1;
        }
    }

    // Get today's meals
    public List<Meal> getTodayMeals() {
        List<Meal> meals = new ArrayList<>();
        if (!connected) return meals;

        String sql = "SELECT * FROM meals WHERE meal_date = CURDATE() ORDER BY meal_time DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Meal meal = new Meal(
                        rs.getInt("id"),
                        rs.getString("food_name"),
                        rs.getInt("quantity"),
                        rs.getInt("calories"),
                        rs.getString("category"),
                        rs.getString("meal_type"),
                        rs.getTime("meal_time").toString()
                );
                meals.add(meal);
            }
        } catch (SQLException e) {
            System.err.println("Error loading meals: " + e.getMessage());
        }
        return meals;
    }

    // Delete a specific meal
    public boolean deleteMeal(int id) {
        if (!connected) return false;

        String sql = "DELETE FROM meals WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting meal: " + e.getMessage());
            return false;
        }
    }

    // Delete all today's meals
    public boolean deleteAllTodayMeals() {
        if (!connected) return false;

        String sql = "DELETE FROM meals WHERE meal_date = CURDATE()";

        try (Statement stmt = connection.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            return rows >= 0;
        } catch (SQLException e) {
            System.err.println("Error deleting all meals: " + e.getMessage());
            return false;
        }
    }

    // Get daily calorie goal
    public int getDailyCalorieGoal() {
        if (!connected) return 2000;

        String sql = "SELECT daily_calorie_goal FROM user_settings WHERE user_id = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("daily_calorie_goal");
            }
        } catch (SQLException e) {
            System.err.println("Error getting calorie goal: " + e.getMessage());
        }
        return 2000;
    }

    // Update daily calorie goal
    public boolean updateDailyCalorieGoal(int newGoal) {
        if (!connected) return false;

        String sql = "UPDATE user_settings SET daily_calorie_goal = ? WHERE user_id = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newGoal);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating calorie goal: " + e.getMessage());
            return false;
        }
    }

    // Check connection status
    public boolean isConnected() {
        return connected;
    }

    // Close connection
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // Inner Meal class
    public static class Meal {
        private int id;
        private String foodName;
        private int quantity;
        private int calories;
        private String category;
        private String mealType;
        private String mealTime;

        public Meal(int id, String foodName, int quantity, int calories,
                    String category, String mealType, String mealTime) {
            this.id = id;
            this.foodName = foodName;
            this.quantity = quantity;
            this.calories = calories;
            this.category = category;
            this.mealType = mealType;
            this.mealTime = mealTime;
        }

        // Getters
        public int getId() { return id; }
        public String getFoodName() { return foodName; }
        public int getQuantity() { return quantity; }
        public int getCalories() { return calories; }
        public String getCategory() { return category; }
        public String getMealType() { return mealType; }
        public String getMealTime() { return mealTime; }
    }
}