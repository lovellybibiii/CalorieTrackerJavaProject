import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector {
    private Connection connection;
    private boolean connected = false;

    public DatabaseConnector(Connection connection) throws SQLException {
        this.connection = connection;
        if (connection != null && !connection.isClosed()) {
            this.connected = true;
            System.out.println("✅ Database connected successfully");
        }
    }

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
            System.out.println("📋 Loaded " + foods.size() + " foods");

        } catch (SQLException e) {
            System.err.println("❌ Error loading foods: " + e.getMessage());
        }
        return foods;
    }

    public int saveMeal(String foodName, int quantity, int calories,
                        String category, String mealType) {
        if (!connected) return -1;

        String sql = "INSERT INTO meals (food_name, quantity, calories, category, " +
                "meal_type, meal_date, meal_time) " +
                "VALUES (?, ?, ?, ?, ?, CURDATE(), CURTIME())";

        try (PreparedStatement pstmt = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, foodName);
            pstmt.setInt(2, quantity);
            pstmt.setInt(3, calories);
            pstmt.setString(4, category);
            pstmt.setString(5, mealType);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("➕ Meal saved with ID: " + id);
                return id;
            }
            return -1;

        } catch (SQLException e) {
            System.err.println("❌ Error saving meal: " + e.getMessage());
            return -1;
        }
    }

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
            System.out.println("📊 Loaded " + meals.size() + " meals for today");

        } catch (SQLException e) {
            System.err.println("❌ Error loading meals: " + e.getMessage());
        }
        return meals;
    }

    public boolean deleteMeal(int id) {
        if (!connected) return false;

        String sql = "DELETE FROM meals WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            boolean success = rows > 0;
            System.out.println(success ? "🗑️ Meal " + id + " deleted" : "❌ Meal not found");
            return success;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting meal: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAllTodayMeals() {
        if (!connected) return false;

        String sql = "DELETE FROM meals WHERE meal_date = CURDATE()";

        try (Statement stmt = connection.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println("🗑️ Deleted " + rows + " meals");
            return rows >= 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting all meals: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

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

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
                System.out.println("🔌 Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

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

        public int getId() { return id; }
        public String getFoodName() { return foodName; }
        public int getQuantity() { return quantity; }
        public int getCalories() { return calories; }
        public String getCategory() { return category; }
        public String getMealType() { return mealType; }
        public String getMealTime() { return mealTime; }
    }
}