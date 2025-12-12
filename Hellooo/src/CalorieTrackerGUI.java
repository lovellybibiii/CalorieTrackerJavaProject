import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.awt.SystemColor.text;

public class CalorieTrackerGUI extends JFrame {
    // Components
    private JLabel caloriesTodayLabel, dailyGoalLabel, remainingLabel, progressPercentLabel;
    private JTextField foodNameField, quantityField, caloriesField, totalCaloriesField, newGoalField;
    private JTable mealsTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JButton addMealBtn, clearBtn, refreshBtn, deleteAllBtn, setGoalBtn, saveReportBtn;
    private JComboBox<Object> mealTypeCombo;
    private JComboBox<Object> foodSearchCombo;

    // Database connector
    private DatabaseConnector dbConnector;

    // Columns for table
    private String[] columns = {"ID", "Food", "Quantity", "Calories", "Category", "Time", "Meal Type", "Actions"};
    private String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
    private int dailyGoal = 2000;
    private int totalCalories = 0;

    // Constructor
    public CalorieTrackerGUI(Connection connection) {
        if (connection != null) {
            try {
                this.dbConnector = new DatabaseConnector(connection);
                System.out.println("Database connector initialized successfully!");
            } catch (SQLException e) {
                showError("Failed to initialize database: " + e.getMessage());
            }
        }

        initializeUI();
        loadFoodsFromDatabase();
        loadMealsFromDatabase();
        updateStats();
    }

    private void initializeUI() {
        setTitle("Calorie Tracker");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 240));

        // Header
        JPanel headerPanel = createHeaderPanel();

        // Center panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setBackground(new Color(240, 240, 240));

        // Left panel - Add Meal Form
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createAddMealPanel(), BorderLayout.CENTER);

        // Right panel - Meals Table
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(createMealsPanel(), BorderLayout.CENTER);

        centerPanel.add(leftPanel);
        centerPanel.add(rightPanel);

        // Statistics panel
        JPanel statsPanel = createStatsPanel();

        // Footer
        JPanel footerPanel = createFooterPanel();

        // Add all panels
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Set window size
        setSize(1300, 850);
        setLocationRelativeTo(null);

        // Add footer
        add(footerPanel, BorderLayout.PAGE_END);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Calorie Tracker");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Select food, enter quantity - we calculate calories automatically");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Database status
        JLabel dbStatusLabel = new JLabel();
        dbStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dbStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (dbConnector != null && dbConnector.isConnected()) {
            dbStatusLabel.setText(dbConnector.getFoodCount() + " foods available");
            dbStatusLabel.setForeground(new Color(46, 204, 113));
        } else {
            dbStatusLabel.setText("Working in Local Mode");
            dbStatusLabel.setForeground(new Color(241, 196, 15));
        }

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(dbStatusLabel);

        return headerPanel;
    }

    private JPanel createAddMealPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Add New Meal"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);

        // Create form try null layout
        JPanel formPanel = new JPanel(new GridLayout (6,2,10,10));
        formPanel.setBackground(Color.WHITE);

        // Select Food
        formPanel.add(new JLabel("Select Food:"));
        foodSearchCombo = new JComboBox<>();
        foodSearchCombo.addItem("-- Select Food --");
        formPanel.add(foodSearchCombo);

        // Food Name (display only)
        formPanel.add(new JLabel("Selected Food:"));
        foodNameField = new JTextField();
        foodNameField.setEditable(false);
        foodNameField.setBackground(new Color(245, 245, 245));
        formPanel.add(foodNameField);

        // Quantity
        formPanel.add(new JLabel("Quantity (grams):"));
        quantityField = new JTextField("100");
        formPanel.add(quantityField);

        // Calories per 100g (display only)
        formPanel.add(new JLabel("Calories/100g:"));
        caloriesField = new JTextField();
        caloriesField.setEditable(false);
        caloriesField.setBackground(new Color(245, 245, 245));
        formPanel.add(caloriesField);

        // Meal Type
        formPanel.add(new JLabel("Meal Type:"));
        mealTypeCombo = new JComboBox<>(mealTypes);
        formPanel.add(mealTypeCombo);

        // Total Calories (calculated automatically)
        formPanel.add(new JLabel("Total Calories:"));
        totalCaloriesField = new JTextField();
        totalCaloriesField.setEditable(false);
        totalCaloriesField.setBackground(new Color(220, 240, 220));
        totalCaloriesField.setFont(new Font("Arial", Font.BOLD, 12));
        formPanel.add(totalCaloriesField);

        // Add listener for food selection
        foodSearchCombo.addActionListener(e -> {
            Object selected = foodSearchCombo.getSelectedItem();

            if (selected instanceof FoodItem) {
                FoodItem food = (FoodItem) selected;

                // Auto-fill the fields
                foodNameField.setText(food.getName());
                caloriesField.setText(String.valueOf(food.getCalories()));

                // Auto-calculate total calories
                calculateTotalCalories();

                // Focus on quantity field
                quantityField.requestFocus();
                quantityField.selectAll();
            } else if (selected != null && selected.equals("-- Select Food --")) {
                // Clear fields
                foodNameField.setText("");
                caloriesField.setText("");
                totalCaloriesField.setText("");
            }
        });

        // Auto-calculate when quantity changes
        quantityField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (foodSearchCombo.getSelectedItem() instanceof FoodItem) {
                    calculateTotalCalories();
                }
            }
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        addMealBtn = new JButton("+ Add Meal");
        addMealBtn.setBackground(new Color(46, 204, 113));
        addMealBtn.setForeground(Color.WHITE);
        addMealBtn.setFont(new Font("Arial", Font.BOLD, 12));

        clearBtn = new JButton(" - Clear   ");
        clearBtn.setBackground(new Color(241, 196, 15));
        clearBtn.setForeground(Color.BLACK);
        clearBtn.setFont(new Font("Arial", Font.BOLD, 12));

        buttonPanel.add(addMealBtn);
        buttonPanel.add(clearBtn);

        // Add listeners
        addMealBtn.addActionListener(e -> addMeal());
        clearBtn.addActionListener(e -> clearForm());

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMealsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Today's Meals"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);

        // Create table
        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column
            }
        };

        mealsTable = new JTable(tableModel);
        mealsTable.setRowHeight(30);
        mealsTable.setFont(new Font("Arial", Font.PLAIN, 12));

        // Hide ID column
        mealsTable.getColumnModel().getColumn(0).setMinWidth(0);
        mealsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        mealsTable.getColumnModel().getColumn(0).setWidth(0);

        // Add action buttons to table
        mealsTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        mealsTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));

        // Style header
        JTableHeader header = mealsTable.getTableHeader();
        header.setBackground(new Color(52, 152, 219));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 12));

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(mealsTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setBackground(new Color(52, 152, 219));
        refreshBtn.setForeground(Color.WHITE);

        deleteAllBtn = new JButton(" X  Delete All");
        deleteAllBtn.setBackground(new Color(231, 76, 60));
        deleteAllBtn.setForeground(Color.WHITE);

        buttonPanel.add(refreshBtn);
        buttonPanel.add(deleteAllBtn);

        // Add listeners
        refreshBtn.addActionListener(e -> refreshTable());
        deleteAllBtn.addActionListener(e -> deleteAllMeals());

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Calories Consumed
        JPanel caloriesPanel = createStatPanel("Calories Consumed Today", "0",
                new Color(41, 128, 185), true);

        // Daily Goal
        JPanel goalPanel = createStatPanel("Daily Goal", String.valueOf(dailyGoal),
                new Color(46, 204, 113), false);

        // Calories Remaining
        JPanel remainingPanel = createStatPanel("Calories Remaining", String.valueOf(dailyGoal),
                new Color(231, 76, 60), false);

        panel.add(caloriesPanel);
        panel.add(goalPanel);
        panel.add(remainingPanel);

        return panel;
    }

    private JPanel createStatPanel(String title, String value, Color color, boolean showProgress) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.DARK_GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Unit
        JLabel unitLabel = new JLabel("calories");
        unitLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        unitLabel.setForeground(Color.GRAY);
        unitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(valueLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(unitLabel);

        // Store references
        if (title.equals("Calories Consumed Today")) {
            caloriesTodayLabel = valueLabel;

            if (showProgress) {
                panel.add(Box.createRigidArea(new Dimension(0, 20)));

                // Progress bar
                JPanel progressPanel = new JPanel();
                progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
                progressPanel.setBackground(Color.WHITE);

                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.setBackground(Color.WHITE);

                JLabel progressLabel = new JLabel("Daily Progress");
                progressLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                progressLabel.setForeground(Color.GRAY);

                progressPercentLabel = new JLabel("0%");
                progressPercentLabel.setFont(new Font("Arial", Font.BOLD, 12));
                progressPercentLabel.setForeground(Color.DARK_GRAY);

                labelPanel.add(progressLabel);
                labelPanel.add(Box.createRigidArea(new Dimension(80, 0)));
                labelPanel.add(progressPercentLabel);

                progressBar = new JProgressBar(0, 100);
                progressBar.setValue(0);
                progressBar.setForeground(color);
                progressBar.setBackground(new Color(220, 220, 220));
                progressBar.setStringPainted(false);

                progressPanel.add(labelPanel);
                progressPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                progressPanel.add(progressBar);

                panel.add(progressPanel);
            }
        } else if (title.equals("Daily Goal")) {
            dailyGoalLabel = valueLabel;

            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Goal input
            JPanel goalInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            goalInputPanel.setBackground(Color.WHITE);

            newGoalField = new JTextField(String.valueOf(dailyGoal), 8);
            setGoalBtn = new JButton("Set Goal");
            setGoalBtn.setBackground(color);
            setGoalBtn.setForeground(Color.WHITE);
            setGoalBtn.addActionListener(e -> setDailyGoal());

            goalInputPanel.add(newGoalField);
            goalInputPanel.add(setGoalBtn);

            panel.add(goalInputPanel);

            // Recommendation
            JLabel recLabel = new JLabel("Recommended: 2000-2500 calories/day");
            recLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            recLabel.setForeground(Color.GRAY);
            recLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(recLabel);
        } else if (title.equals("Calories Remaining")) {
            remainingLabel = valueLabel;

            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Save report button
            saveReportBtn = new JButton("Save Report");
            saveReportBtn.setBackground(new Color(52, 152, 219));
            saveReportBtn.setForeground(Color.WHITE);
            saveReportBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            saveReportBtn.addActionListener(e -> saveReport());

            panel.add(saveReportBtn);

            // Database info
            panel.add(Box.createRigidArea(new Dimension(0, 15)));
            String dbInfo = (dbConnector != null && dbConnector.isConnected()) ?
                    " Your daily report will be saved " : "Local Mode Only";
            JLabel dbLabel = new JLabel(dbInfo);
            dbLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            dbLabel.setForeground(Color.GRAY);
            dbLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(dbLabel);
        }

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // App info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        infoPanel.setBackground(Color.WHITE);

        // App icon
        JLabel appIcon = new JLabel("*");
        appIcon.setFont(new Font("Arial", Font.BOLD, 20));
        appIcon.setForeground(new Color(41, 128, 185));
        appIcon.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // App name
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel appName = new JLabel("   Calorie Tracker");
        appName.setFont(new Font("Arial", Font.BOLD, 16));
        appName.setForeground(Color.DARK_GRAY);

        JLabel appDesc = new JLabel("Automatic calorie calculation");
        appDesc.setFont(new Font("Arial", Font.PLAIN, 12));
        appDesc.setForeground(Color.GRAY);

        textPanel.add(appName);
        textPanel.add(appDesc);

        infoPanel.add(appIcon);
        infoPanel.add(textPanel);

        // Copyright
        JLabel copyright = new JLabel("© 2025 Calorie Tracker | Just select food and enter quantity");
        copyright.setFont(new Font("Arial", Font.PLAIN, 11));
        copyright.setForeground(Color.GRAY);
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Instruction
        JLabel instruction = new JLabel("Select food from list → Enter quantity → Calories calculated automatically!");
        instruction.setFont(new Font("Arial", Font.PLAIN, 10));
        instruction.setForeground(new Color(41, 128, 185));
        instruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(infoPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(copyright);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(instruction);

        return panel;
    }

    private void calculateTotalCalories() {
        try {
            // Get quantity from user
            int quantity = Integer.parseInt(quantityField.getText());

            // Get calories from caloriesField (auto-filled from database)
            if (caloriesField.getText().isEmpty()) {
                totalCaloriesField.setText("0");
                return;
            }

            int caloriesPer100g = Integer.parseInt(caloriesField.getText());

            // Calculate: (quantity × calories_per_100g) ÷ 100
            int total = (quantity * caloriesPer100g) / 100;
            totalCaloriesField.setText(String.valueOf(total));

        } catch (NumberFormatException e) {
            totalCaloriesField.setText("0");
        }
    }

    private void updateStats() {
        // Calculate total calories from table
        int total = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String calStr = tableModel.getValueAt(i, 3).toString();
                int calories = Integer.parseInt(calStr);
                total += calories;
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }

        totalCalories = total;

        // Update labels
        if (caloriesTodayLabel != null) {
            caloriesTodayLabel.setText(String.valueOf(total));
        }

        if (dailyGoalLabel != null) {
            dailyGoalLabel.setText(String.valueOf(dailyGoal));
        }

        if (remainingLabel != null) {
            int remaining = Math.max(0, dailyGoal - total);
            remainingLabel.setText(String.valueOf(remaining));

            // Change color based on remaining
            if (remaining > dailyGoal * 0.5) {
                remainingLabel.setForeground(new Color(46, 204, 113));
            } else if (remaining > dailyGoal * 0.2) {
                remainingLabel.setForeground(new Color(241, 196, 15));
            } else {
                remainingLabel.setForeground(new Color(231, 76, 60));
            }
        }

        // Update progress bar
        if (progressBar != null && progressPercentLabel != null) {
            int percent = dailyGoal > 0 ? Math.min((total * 100) / dailyGoal, 100) : 0;
            progressBar.setValue(percent);
            progressPercentLabel.setText(percent + "%");

            // Change progress bar color
            if (percent < 70) {
                progressBar.setForeground(new Color(46, 204, 113));
            } else if (percent < 90) {
                progressBar.setForeground(new Color(241, 196, 15));
            } else {
                progressBar.setForeground(new Color(231, 76, 60));
            }
        }
    }

    private void addMeal() {
        Object selectedFood = foodSearchCombo.getSelectedItem();

        // Check if food is selected
        if (!(selectedFood instanceof FoodItem)) {
            showError("Please select a food from the list!");
            return;
        }

        FoodItem food = (FoodItem) selectedFood;
        String quantityStr = quantityField.getText();
        String mealType = (String) mealTypeCombo.getSelectedItem();

        // Validate quantity
        if (quantityStr.isEmpty()) {
            showError("Please enter quantity!");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);

            // Get calculated total calories
            String totalCalStr = totalCaloriesField.getText();
            if (totalCalStr.isEmpty() || totalCalStr.equals("0")) {
                showError("Please calculate calories first!");
                return;
            }

            int totalCalories = Integer.parseInt(totalCalStr);

            // Determine category
            String category = determineCategory(food.getName());

            // Get current time
            String time = new SimpleDateFormat("HH:mm").format(new Date());

            // Add to table
            int newRowIndex = tableModel.getRowCount();
            tableModel.addRow(new Object[]{
                    newRowIndex + 1, // Temporary ID
                    food.getName(),
                    quantity + "g",
                    String.valueOf(totalCalories),
                    category,
                    time,
                    mealType,
                    "Edit | Delete"
            });

            // Save to database
            if (dbConnector != null && dbConnector.isConnected()) {
                int mealId = dbConnector.saveMeal(food.getName(), quantity, totalCalories, category, mealType);
                if (mealId > 0) {
                    // Update the ID in the table
                    tableModel.setValueAt(mealId, newRowIndex, 0);
                    showInfo("✓ Meal added successfully!");
                } else {
                    showError("Failed to save meal to database!");
                }
            } else {
                showInfo("Meal added locally (database not connected)");
            }

            // Clear form and reset selection
            clearForm();

            // Update statistics
            updateStats();

        } catch (NumberFormatException e) {
            showError("Please enter valid quantity (numbers only)!");
        }
    }

    private String determineCategory(String foodName) {
        String lowerName = foodName.toLowerCase();
        if (lowerName.contains("apple") || lowerName.contains("banana") || lowerName.contains("orange") ||
                lowerName.contains("fruit") || lowerName.contains("grapes") || lowerName.contains("mango") ||
                lowerName.contains("strawberry") || lowerName.contains("watermelon") || lowerName.contains("pear")) {
            return "Fruit";
        } else if (lowerName.contains("chicken") || lowerName.contains("beef") || lowerName.contains("fish") ||
                lowerName.contains("egg") || lowerName.contains("meat") || lowerName.contains("tuna") ||
                lowerName.contains("salmon") || lowerName.contains("shawarma")) {
            return "Protein";
        } else if (lowerName.contains("rice") || lowerName.contains("pasta") || lowerName.contains("bread") ||
                lowerName.contains("potato") || lowerName.contains("corn") || lowerName.contains("oats") ||
                lowerName.contains("biscuit") || lowerName.contains("cake")) {
            return "Carbohydrate";
        } else if (lowerName.contains("salad") || lowerName.contains("vegetable") || lowerName.contains("lettuce") ||
                lowerName.contains("tomato") || lowerName.contains("cucumber") || lowerName.contains("carrot") ||
                lowerName.contains("spinach") || lowerName.contains("zucchini") || lowerName.contains("pepper")) {
            return "Vegetable";
        } else if (lowerName.contains("cheese") || lowerName.contains("milk") || lowerName.contains("yogurt") ||
                lowerName.contains("cottage")) {
            return "Dairy";
        } else if (lowerName.contains("oil") || lowerName.contains("butter") || lowerName.contains("nuts") ||
                lowerName.contains("almond") || lowerName.contains("peanut") || lowerName.contains("cashew")) {
            return "Fat";
        } else {
            return "Other";
        }
    }

    private void clearForm() {
        foodSearchCombo.setSelectedIndex(0);
        foodNameField.setText("");
        quantityField.setText("100");
        caloriesField.setText("");
        totalCaloriesField.setText("");
    }

    private void loadFoodsFromDatabase() {
        if (dbConnector == null) return;

        try {
            List<FoodItem> foods = dbConnector.getAllFoods();
            for (FoodItem food : foods) {
                foodSearchCombo.addItem(food);
            }
            System.out.println("Loaded " + foods.size() + " foods from database");
        } catch (Exception e) {
            System.err.println("Error loading foods: " + e.getMessage());
            showError("Failed to load foods from database!");
        }
    }

    private void loadMealsFromDatabase() {
        tableModel.setRowCount(0);

        if (dbConnector == null || !dbConnector.isConnected()) {
            return;
        }

        try {
            List<DatabaseConnector.Meal> meals = dbConnector.getTodayMeals();
            for (DatabaseConnector.Meal meal : meals) {
                tableModel.addRow(new Object[]{
                        meal.getId(),
                        meal.getFoodName(),
                        meal.getQuantity() + "g",
                        String.valueOf(meal.getCalories()),
                        meal.getCategory(),
                        meal.getMealTime(),
                        meal.getMealType(),
                        "Edit | Delete"
                });
            }
            System.out.println("Loaded " + meals.size() + " meals from database");
        } catch (Exception e) {
            System.err.println("Error loading meals: " + e.getMessage());
        }

        updateStats();
    }

    private void refreshTable() {
        loadMealsFromDatabase();
        updateStats();
        showInfo("Table refreshed!");
    }

    private void deleteAllMeals() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete all meals?\nThis action cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            // Delete from database
            if (dbConnector != null && dbConnector.isConnected()) {
                boolean success = dbConnector.deleteAllTodayMeals();
                if (!success) {
                    showError("Failed to delete meals from database!");
                    return;
                }
            }

            // Clear table
            tableModel.setRowCount(0);
            updateStats();
            showInfo("All meals deleted successfully!");
        }
    }

    private void setDailyGoal() {
        try {
            int newGoal = Integer.parseInt(newGoalField.getText());
            if (newGoal < 500 || newGoal > 5000) {
                showError("Please enter a realistic goal (500-5000 calories)!");
                return;
            }
            dailyGoal = newGoal;

            // Update in database
            if (dbConnector != null && dbConnector.isConnected()) {
                dbConnector.updateDailyCalorieGoal(newGoal);
            }

            updateStats();
            showInfo("Daily goal updated to " + newGoal + " calories!");
        } catch (NumberFormatException e) {
            showError("Please enter a valid number!");
        }
    }

    private void saveReport() {
        String report = "DAILY CALORIE REPORT\n" +
                "====================\n\n" +
                "Date: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "\n" +
                "Total Calories Consumed: " + totalCalories + "\n" +
                "Daily Goal: " + dailyGoal + "\n" +
                "Calories Remaining: " + Math.max(0, dailyGoal - totalCalories) + "\n" +
                "Progress: " + (dailyGoal > 0 ? (totalCalories * 100 / dailyGoal) : 0) + "%\n" +
                "Number of Meals: " + tableModel.getRowCount() + "\n\n";

        if (dbConnector != null && dbConnector.isConnected()) {
            report += "Status: ✓ Data saved to database\n";
        } else {
            report += "Status: ⚠ Data stored locally only\n";
        }

        if (tableModel.getRowCount() > 0) {
            report += "\n--- Meal Breakdown ---\n";
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String food = tableModel.getValueAt(i, 1).toString();
                String quantity = tableModel.getValueAt(i, 2).toString();
                String calories = tableModel.getValueAt(i, 3).toString();
                String mealType = tableModel.getValueAt(i, 6).toString();
                report += String.format("- %s: %s, %s calories (%s)\n",
                        food, quantity, calories, mealType);
            }
        }

        // Show report
        JTextArea textArea = new JTextArea(report, 20, 40);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Daily Report", JOptionPane.INFORMATION_MESSAGE);
    }

    // Helper methods for messages
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Button Renderer for table actions
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            setBackground(new Color(52, 152, 219));
            setForeground(Color.WHITE);
            return this;
        }
    }

    // Button Editor for table actions
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            button.setBackground(new Color(231, 76, 60));
            button.setForeground(Color.WHITE);
            isPushed = true;

            // Handle button click
            SwingUtilities.invokeLater(() -> {
                if (label.contains("Delete")) {
                    handleDelete(row);
                } else if (label.contains("Edit")) {
                    handleEdit(row);
                }
            });

            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                // Action already handled
            }
            isPushed = false;
            return label;
        }

        private void handleDelete(int row) {
            int response = JOptionPane.showConfirmDialog(CalorieTrackerGUI.this,
                    "Are you sure you want to delete this meal?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                try {
                    // Get meal ID
                    Object idObj = tableModel.getValueAt(row, 0);
                    if (idObj instanceof Integer) {
                        int mealId = (int) idObj;

                        // Delete from database
                        if (dbConnector != null && dbConnector.isConnected()) {
                            boolean success = dbConnector.deleteMeal(mealId);
                            if (!success) {
                                showError("Failed to delete meal from database!");
                                return;
                            }
                        }

                        // Remove from table
                        tableModel.removeRow(row);
                        updateStats();
                        showInfo("Meal deleted successfully!");
                    }
                } catch (Exception e) {
                    showError("Error deleting meal: " + e.getMessage());
                }
            }
        }

        private void handleEdit(int row) {
            try {
                // Get meal data
                String foodName = tableModel.getValueAt(row, 1).toString();
                String quantity = tableModel.getValueAt(row, 2).toString().replace("g", "");
                String calories = tableModel.getValueAt(row, 3).toString();
                String mealType = tableModel.getValueAt(row, 6).toString();

                // Try to find the food in combo box
                boolean found = false;
                for (int i = 0; i < foodSearchCombo.getItemCount(); i++) {
                    Object item = foodSearchCombo.getItemAt(i);
                    if (item instanceof FoodItem) {
                        FoodItem food = (FoodItem) item;
                        if (food.getName().equals(foodName)) {
                            foodSearchCombo.setSelectedItem(food);
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    foodNameField.setText(foodName);
                    caloriesField.setText("");
                }

                quantityField.setText(quantity);
                mealTypeCombo.setSelectedItem(mealType);

                if (found) {
                    calculateTotalCalories();
                }

                showInfo("Edit mode: Modify values and click 'Add Meal' to update");

            } catch (Exception e) {
                showError("Error editing meal: " + e.getMessage());
            }
        }
    }

    // Close database connection
    @Override
    public void dispose() {
        if (dbConnector != null) {
            dbConnector.close();
        }
        super.dispose();
    }

}