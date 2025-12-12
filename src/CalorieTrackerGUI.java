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
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CalorieTrackerGUI extends JFrame {
    private JLabel caloriesTodayLabel, dailyGoalLabel, remainingLabel, progressPercentLabel;
    private JTextField foodNameField, quantityField, totalCaloriesField, newGoalField;
    private JTable mealsTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JButton addMealBtn, clearBtn, refreshBtn, deleteAllBtn, setGoalBtn, saveReportBtn;
    private JComboBox<Object> mealTypeCombo;
    private JComboBox<FoodItem> foodSearchCombo;
    private Timer autoRefreshTimer;
    private JButton toggleAutoRefreshBtn;
    private boolean autoRefreshEnabled = true;
    private DatabaseConnector dbConnector;

    private final Font SEGOE_UI = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font SEGOE_UI_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    private final Font SEGOE_UI_TITLE = new Font("Segoe UI", Font.BOLD, 32);
    private final Font SEGOE_UI_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font SEGOE_UI_LARGE = new Font("Segoe UI", Font.BOLD, 28);
    private final Font SEGOE_UI_MEDIUM = new Font("Segoe UI", Font.BOLD, 16);
    private final Font SEGOE_UI_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    private String[] columns = {"ID", "Food", "Quantity", "Calories", "Category", "Time", "Meal Type", "Actions"};
    private String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
    private int dailyGoal = 2000;
    private int totalCalories = 0;

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
        startAutoRefresh();

        try {
            setIconImage(new ImageIcon("C:\\Users\\HP\\IdeaProjects\\Hellooo\\src\\images\\photo_2025-12-06_12-52-36.jpg").getImage());
        } catch (Exception e) {
            System.out.println("Note: Custom icon not found, using default Java icon.");
        }

        loadFoodsFromDatabase();
        loadMealsFromDatabase();
        updateStats();
    }

    private void initializeUI() {
        setTitle("Calorie Tracker");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 240));

        JPanel headerPanel = createHeaderPanel();
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setBackground(new Color(240, 240, 240));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createAddMealPanel(), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(createMealsPanel(), BorderLayout.CENTER);

        centerPanel.add(leftPanel);
        centerPanel.add(rightPanel);

        JPanel statsPanel = createStatsPanel();
        JPanel footerPanel = createFooterPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.SOUTH);
        add(mainPanel);
        setSize(1300, 850);
        setLocationRelativeTo(null);
        add(footerPanel, BorderLayout.PAGE_END);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Calorie Tracker");
        titleLabel.setFont(SEGOE_UI_TITLE);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Select food from list - calories calculated automatically");
        subtitleLabel.setFont(SEGOE_UI_SUBTITLE);
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dbStatusLabel = new JLabel();
        dbStatusLabel.setFont(SEGOE_UI);
        dbStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel autoRefreshLabel = new JLabel();
        autoRefreshLabel.setFont(SEGOE_UI_SMALL);
        autoRefreshLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        autoRefreshLabel.setForeground(new Color(200, 230, 255));

        if (dbConnector != null && dbConnector.isConnected()) {
            dbStatusLabel.setText(dbConnector.getFoodCount() + " foods available");
            dbStatusLabel.setForeground(new Color(46, 204, 113));
            autoRefreshLabel.setText("Auto-refresh: ON (every 60 seconds)");
        } else {
            dbStatusLabel.setText("Working in Local Mode");
            dbStatusLabel.setForeground(new Color(241, 196, 15));
            autoRefreshLabel.setText("Auto-refresh: OFF (no database connection)");
        }

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(dbStatusLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(autoRefreshLabel);

        return headerPanel;
    }

    private JPanel createAddMealPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Add New Meal"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBackground(Color.WHITE);

        JLabel mealTypeLabel = new JLabel("Meal Type:");
        mealTypeLabel.setFont(SEGOE_UI);
        formPanel.add(mealTypeLabel);
        mealTypeCombo = new JComboBox<>(mealTypes);
        mealTypeCombo.setFont(SEGOE_UI);
        formPanel.add(mealTypeCombo);

        JLabel selectFoodLabel = new JLabel("Select Food:");
        selectFoodLabel.setFont(SEGOE_UI);
        formPanel.add(selectFoodLabel);
        foodSearchCombo = new JComboBox<>();
        foodSearchCombo.setEditable(false);
        foodSearchCombo.setFont(SEGOE_UI);
        formPanel.add(foodSearchCombo);

        JLabel selectedFoodLabel = new JLabel("Selected Food:");
        selectedFoodLabel.setFont(SEGOE_UI);
        formPanel.add(selectedFoodLabel);
        foodNameField = new JTextField();
        foodNameField.setEditable(false);
        foodNameField.setBackground(new Color(245, 245, 245));
        foodNameField.setFont(SEGOE_UI);
        formPanel.add(foodNameField);

        JLabel quantityLabel = new JLabel("Quantity (grams):");
        quantityLabel.setFont(SEGOE_UI);
        formPanel.add(quantityLabel);
        quantityField = new JTextField("100");
        quantityField.setFont(SEGOE_UI);
        formPanel.add(quantityField);

        JLabel totalCaloriesLabel = new JLabel("Total Calories:");
        totalCaloriesLabel.setFont(SEGOE_UI);
        formPanel.add(totalCaloriesLabel);
        totalCaloriesField = new JTextField();
        totalCaloriesField.setEditable(false);
        totalCaloriesField.setBackground(new Color(220, 240, 220));
        totalCaloriesField.setFont(SEGOE_UI_BOLD);
        formPanel.add(totalCaloriesField);

        foodSearchCombo.addActionListener(e -> {
            FoodItem selected = (FoodItem) foodSearchCombo.getSelectedItem();
            if (selected != null) {
                foodNameField.setText(selected.getName());
                calculateTotalCalories();
                quantityField.requestFocus();
                quantityField.selectAll();
            } else {
                foodNameField.setText("");
                totalCaloriesField.setText("");
            }
        });

        quantityField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (foodSearchCombo.getSelectedItem() != null) {
                    calculateTotalCalories();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        addMealBtn = new JButton("Add Meal");
        addMealBtn.setBackground(new Color(46, 204, 113));
        addMealBtn.setForeground(Color.WHITE);
        addMealBtn.setFont(SEGOE_UI_BOLD);
        clearBtn = new JButton("Clear");
        clearBtn.setBackground(new Color(241, 196, 15));
        clearBtn.setForeground(Color.BLACK);
        clearBtn.setFont(SEGOE_UI_BOLD);
        buttonPanel.add(addMealBtn);
        buttonPanel.add(clearBtn);
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

        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };

        mealsTable = new JTable(tableModel);
        mealsTable.setRowHeight(30);
        mealsTable.setFont(SEGOE_UI);
        mealsTable.getColumnModel().getColumn(0).setMinWidth(0);
        mealsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        mealsTable.getColumnModel().getColumn(0).setWidth(0);
        mealsTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        mealsTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));

        JTableHeader header = mealsTable.getTableHeader();
        header.setBackground(new Color(52, 152, 219));
        header.setForeground(Color.WHITE);
        header.setFont(SEGOE_UI_BOLD);

        JScrollPane scrollPane = new JScrollPane(mealsTable);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(new Color(52, 152, 219));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(SEGOE_UI_BOLD);
        deleteAllBtn = new JButton("Delete All");
        deleteAllBtn.setBackground(new Color(231, 76, 60));
        deleteAllBtn.setForeground(Color.WHITE);
        deleteAllBtn.setFont(SEGOE_UI_BOLD);
        toggleAutoRefreshBtn = new JButton("Disable Auto-refresh");
        toggleAutoRefreshBtn.setBackground(new Color(155, 89, 182));
        toggleAutoRefreshBtn.setForeground(Color.WHITE);
        toggleAutoRefreshBtn.setFont(SEGOE_UI_BOLD);
        toggleAutoRefreshBtn.addActionListener(e -> toggleAutoRefresh());
        buttonPanel.add(refreshBtn);
        buttonPanel.add(deleteAllBtn);
        buttonPanel.add(toggleAutoRefreshBtn);
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
        JPanel caloriesPanel = createStatPanel("Calories Consumed Today", "0", new Color(41, 128, 185), true);
        JPanel goalPanel = createStatPanel("Daily Goal", String.valueOf(dailyGoal), new Color(46, 204, 113), false);
        JPanel remainingPanel = createStatPanel("Calories Remaining", String.valueOf(dailyGoal), new Color(231, 76, 60), false);
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

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SEGOE_UI_MEDIUM);
        titleLabel.setForeground(Color.DARK_GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(SEGOE_UI_LARGE);
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel unitLabel = new JLabel("calories");
        unitLabel.setFont(SEGOE_UI);
        unitLabel.setForeground(Color.GRAY);
        unitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(valueLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(unitLabel);

        if (title.equals("Calories Consumed Today")) {
            caloriesTodayLabel = valueLabel;
            if (showProgress) {
                panel.add(Box.createRigidArea(new Dimension(0, 20)));
                JPanel progressPanel = new JPanel();
                progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
                progressPanel.setBackground(Color.WHITE);
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.setBackground(Color.WHITE);
                JLabel progressLabel = new JLabel("Daily Progress");
                progressLabel.setFont(SEGOE_UI);
                progressLabel.setForeground(Color.GRAY);
                progressPercentLabel = new JLabel("0%");
                progressPercentLabel.setFont(SEGOE_UI_BOLD);
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
            JPanel goalInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            goalInputPanel.setBackground(Color.WHITE);
            newGoalField = new JTextField(String.valueOf(dailyGoal), 8);
            newGoalField.setFont(SEGOE_UI);
            setGoalBtn = new JButton("Set Goal");
            setGoalBtn.setBackground(color);
            setGoalBtn.setForeground(Color.WHITE);
            setGoalBtn.setFont(SEGOE_UI_BOLD);
            setGoalBtn.addActionListener(e -> setDailyGoal());
            goalInputPanel.add(newGoalField);
            goalInputPanel.add(setGoalBtn);
            panel.add(goalInputPanel);
            JLabel recLabel = new JLabel("Recommended: 2000-2500 calories/day");
            recLabel.setFont(SEGOE_UI_SMALL);
            recLabel.setForeground(Color.GRAY);
            recLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(recLabel);
        } else if (title.equals("Calories Remaining")) {
            remainingLabel = valueLabel;
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            saveReportBtn = new JButton("Save Report");
            saveReportBtn.setBackground(new Color(52, 152, 219));
            saveReportBtn.setForeground(Color.WHITE);
            saveReportBtn.setFont(SEGOE_UI_BOLD);
            saveReportBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            saveReportBtn.addActionListener(e -> saveReport());
            panel.add(saveReportBtn);
            panel.add(Box.createRigidArea(new Dimension(0, 15)));
            String dbInfo = (dbConnector != null && dbConnector.isConnected()) ?
                    " Your daily report will be saved " : "Local Mode Only";
            JLabel dbLabel = new JLabel(dbInfo);
            dbLabel.setFont(SEGOE_UI_SMALL);
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

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        infoPanel.setBackground(Color.WHITE);
        JLabel appIcon = new JLabel("");
        appIcon.setFont(SEGOE_UI_LARGE);
        appIcon.setForeground(new Color(41, 128, 185));
        appIcon.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);
        JLabel appName = new JLabel("           Calorie Tracker");
        appName.setFont(SEGOE_UI_MEDIUM);
        appName.setForeground(Color.DARK_GRAY);
        JLabel appDesc = new JLabel("Select and calories calculated automatically");
        appDesc.setFont(SEGOE_UI);
        appDesc.setForeground(Color.GRAY);
        textPanel.add(appName);
        textPanel.add(appDesc);
        infoPanel.add(appIcon);
        infoPanel.add(textPanel);
        JLabel copyright = new JLabel("          © 2025 Calorie Tracker");
        copyright.setFont(SEGOE_UI_SMALL);
        copyright.setForeground(Color.GRAY);
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel instruction = new JLabel("Select food from list → Enter quantity → Calories calculated automatically!");
        instruction.setFont(SEGOE_UI_SMALL);
        instruction.setForeground(new Color(41, 128, 185));
        instruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(infoPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(copyright);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(instruction);

        return panel;
    }

    private void startAutoRefresh() {
        if (dbConnector == null || !dbConnector.isConnected()) {
            System.out.println("⚠️ Auto-refresh disabled: No database connection");
            return;
        }

        autoRefreshTimer = new Timer(60000, new ActionListener() {
            private int refreshCount = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshCount++;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (autoRefreshEnabled) {
                            System.out.println("🔄 Auto-refresh #" + refreshCount + " running...");
                            refreshTable();
                            System.out.println("✅ Auto-refresh #" + refreshCount + " completed");
                            if (refreshCount <= 3) {
                                showTemporaryNotification("Table auto-refreshed");
                            }
                        }
                    }
                });
            }
        });

        autoRefreshTimer.start();
        System.out.println("Auto-refresh started (every 60 seconds)");
    }

    private void toggleAutoRefresh() {
        autoRefreshEnabled = !autoRefreshEnabled;
        if (autoRefreshEnabled) {
            autoRefreshTimer.start();
            toggleAutoRefreshBtn.setText("Disable Auto-refresh");
            toggleAutoRefreshBtn.setBackground(new Color(155, 89, 182));
            showInfo("Auto-refresh ENABLED\nTable will refresh every 60 seconds");
            System.out.println("✅ Auto-refresh ENABLED");
        } else {
            autoRefreshTimer.stop();
            toggleAutoRefreshBtn.setText("Enable Auto-refresh");
            toggleAutoRefreshBtn.setBackground(new Color(149, 165, 166));
            showInfo("Auto-refresh DISABLED");
            System.out.println("⏸️ Auto-refresh DISABLED");
        }
    }

    private void showTemporaryNotification(String message) {
        if (isVisible()) {
            System.out.println("💡 " + message);
        }
    }

    private void calculateTotalCalories() {
        try {
            int quantity = Integer.parseInt(quantityField.getText());
            FoodItem selected = (FoodItem) foodSearchCombo.getSelectedItem();
            if (selected == null) {
                totalCaloriesField.setText("0");
                return;
            }
            int caloriesPer100g = selected.getCalories();
            int total = (quantity * caloriesPer100g) / 100;
            totalCaloriesField.setText(String.valueOf(total));
        } catch (NumberFormatException e) {
            totalCaloriesField.setText("0");
        }
    }

    private void updateStats() {
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

        if (caloriesTodayLabel != null) {
            caloriesTodayLabel.setText(String.valueOf(total));
        }

        if (dailyGoalLabel != null) {
            dailyGoalLabel.setText(String.valueOf(dailyGoal));
        }

        if (remainingLabel != null) {
            int remaining = Math.max(0, dailyGoal - total);
            remainingLabel.setText(String.valueOf(remaining));
            if (remaining > dailyGoal * 0.5) {
                remainingLabel.setForeground(new Color(46, 204, 113));
            } else if (remaining > dailyGoal * 0.2) {
                remainingLabel.setForeground(new Color(241, 196, 15));
            } else {
                remainingLabel.setForeground(new Color(231, 76, 60));
            }
        }

        if (progressBar != null && progressPercentLabel != null) {
            int percent = dailyGoal > 0 ? Math.min((total * 100) / dailyGoal, 100) : 0;
            progressBar.setValue(percent);
            progressPercentLabel.setText(percent + "%");
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
        FoodItem selectedFood = (FoodItem) foodSearchCombo.getSelectedItem();
        if (selectedFood == null) {
            showError("Please select a food from the list!");
            return;
        }

        String quantityStr = quantityField.getText();
        String mealType = (String) mealTypeCombo.getSelectedItem();

        if (quantityStr.isEmpty()) {
            showError("Please enter quantity!");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            String totalCalStr = totalCaloriesField.getText();
            if (totalCalStr.isEmpty() || totalCalStr.equals("0")) {
                showError("Please calculate calories first!");
                return;
            }

            int totalCalories = Integer.parseInt(totalCalStr);
            String category = determineCategory(selectedFood.getName());
            String time = new SimpleDateFormat("HH:mm").format(new Date());

            int newRowIndex = tableModel.getRowCount();
            tableModel.addRow(new Object[]{
                    newRowIndex + 1,
                    selectedFood.getName(),
                    quantity + "g",
                    String.valueOf(totalCalories),
                    category,
                    time,
                    mealType,
                    "Delete"
            });

            if (dbConnector != null && dbConnector.isConnected()) {
                int mealId = dbConnector.saveMeal(selectedFood.getName(), quantity, totalCalories, category, mealType);
                if (mealId > 0) {
                    tableModel.setValueAt(mealId, newRowIndex, 0);
                    showInfo("✓ Meal added successfully!");
                } else {
                    showError("Failed to save meal to database!");
                }
            } else {
                showInfo("Meal added locally (database not connected)");
            }

            clearForm();
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
        totalCaloriesField.setText("");
    }

    private void loadFoodsFromDatabase() {
        if (dbConnector == null) return;

        try {
            List<FoodItem> foods = dbConnector.getAllFoods();
            foodSearchCombo.removeAllItems();
            foodSearchCombo.addItem(new FoodItem("-- Select Food --", 0));

            for (FoodItem food : foods) {
                foodSearchCombo.addItem(food);
            }

            System.out.println("Loaded " + foods.size() + " foods from database");
            for (FoodItem food : foods) {
                System.out.println("Loaded: " + food.getName() + " - " + food.getCalories() + " cal");
            }

        } catch (Exception e) {
            System.err.println("Error loading foods: " + e.getMessage());
            showError("Failed to load foods from database!");
            e.printStackTrace();
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
                        "Delete"
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
        if (!autoRefreshEnabled) {
            showInfo("Table refreshed!");
        }
    }

    private void deleteAllMeals() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete all meals?\nThis action cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            if (dbConnector != null && dbConnector.isConnected()) {
                boolean success = dbConnector.deleteAllTodayMeals();
                if (!success) {
                    showError("Failed to delete meals from database!");
                    return;
                }
            }

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
            report += "Status: Saved \n";
        } else {
            report += "Status: Stored locally only\n";
        }

        report += "Auto-refresh: " + (autoRefreshEnabled ? "ENABLED (every 60 seconds)" : "DISABLED") + "\n";

        if (tableModel.getRowCount() > 0) {
            report += "\n--- Meal Breakdown ---\n";
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String food = tableModel.getValueAt(i, 1).toString();
                String quantity = tableModel.getValueAt(i, 2).toString().replace("g", "");
                String calories = tableModel.getValueAt(i, 3).toString();
                String mealType = tableModel.getValueAt(i, 6).toString();
                report += String.format("- %s: %s, %s calories (%s)\n",
                        food, quantity, calories, mealType);
            }
        }

        JTextArea textArea = new JTextArea(report, 20, 40);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        Object[] options = {"Save to File", "Show Only", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, scrollPane,
                "Daily Report - Save to File?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            saveReportToFile(report);
        } else if (choice == 2) {
            return;
        }
    }

    private void saveReportToFile(String report) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report As");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Text Files (*.txt)", "txt");
        fileChooser.setFileFilter(filter);
        String defaultFileName = "calorie_report_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".txt")) {
                fileToSave = new File(filePath + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(report);
                writer.newLine();
                writer.write("\n--- Generated by Calorie Tracker ---");
                writer.newLine();
                writer.write("Generated on: " +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                JOptionPane.showMessageDialog(this,
                        "✓ Report saved successfully!\n" +
                                "Location: " + fileToSave.getAbsolutePath(),
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                logAction("Report saved to file: " + fileToSave.getName());

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "❌ Error saving file:\n" + e.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logAction(String action) {
        String logFile = "calorie_tracker_log.txt";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFile, true))) {
            writer.write(timestamp + " - " + action);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(SEGOE_UI_BOLD);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            setBackground(new Color(52, 152, 219));
            setForeground(Color.WHITE);
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(SEGOE_UI_BOLD);
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
                    Object idObj = tableModel.getValueAt(row, 0);
                    if (idObj instanceof Integer) {
                        int mealId = (int) idObj;
                        if (dbConnector != null && dbConnector.isConnected()) {
                            boolean success = dbConnector.deleteMeal(mealId);
                            if (!success) {
                                showError("Failed to delete meal from database!");
                                return;
                            }
                        }
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
                String foodName = tableModel.getValueAt(row, 1).toString();
                String quantity = tableModel.getValueAt(row, 2).toString().replace("g", "");
                String calories = tableModel.getValueAt(row, 3).toString();
                String mealType = tableModel.getValueAt(row, 6).toString();

                boolean found = false;
                for (int i = 0; i < foodSearchCombo.getItemCount(); i++) {
                    FoodItem item = (FoodItem) foodSearchCombo.getItemAt(i);
                    if (item.getName().equals(foodName)) {
                        foodSearchCombo.setSelectedItem(item);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    foodNameField.setText(foodName);
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

    @Override
    public void dispose() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            System.out.println("⏹️ Auto-refresh timer stopped");
        }

        if (dbConnector != null) {
            dbConnector.close();
        }
        super.dispose();
    }
}