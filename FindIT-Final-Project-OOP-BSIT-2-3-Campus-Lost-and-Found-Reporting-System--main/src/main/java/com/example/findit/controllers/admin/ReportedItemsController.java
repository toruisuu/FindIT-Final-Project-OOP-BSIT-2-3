package com.example.findit.controllers.admin;

import com.example.findit.model.AppDataStore;
import com.example.findit.model.ClaimRequest;
import com.example.findit.model.ItemMatch;
import com.example.findit.model.ItemReport;
import com.example.findit.util.ImageStorage;
import com.example.findit.util.MatchDetailsDialog;
import com.example.findit.util.ResponsiveTable;
import com.example.findit.util.toast;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

public class ReportedItemsController implements Initializable {

    @FXML private AdminSidebarController sidebarController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> dateFilter;
    @FXML private DatePicker monthPicker;
    @FXML private ComboBox<String> viewToggle;
    @FXML private Button archivedItemsButton;
    @FXML private Label lblTimestamp;
    @FXML private TableView<ReportRow> reportsTable;

    @FXML private TableColumn<ReportRow, String> colReportStatus;
    @FXML private TableColumn<ReportRow, String> colClaimTrackingId;
    @FXML private TableColumn<ReportRow, String> colItemTrackingId;
    @FXML private TableColumn<ReportRow, String> colItemName;
    @FXML private TableColumn<ReportRow, String> colType;
    @FXML private TableColumn<ReportRow, String> colCategory;
    @FXML private TableColumn<ReportRow, String> colDate;
    @FXML private TableColumn<ReportRow, String> colClaimant;
    @FXML private TableColumn<ReportRow, String> colLocation;
    @FXML private TableColumn<ReportRow, String> colAction;

    private final ObservableList<ReportRow> masterData = FXCollections.observableArrayList();
    private FilteredList<ReportRow> filteredData;
    private boolean showingArchived;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.setActiveTab("Reported");
        }

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm:ss a");
            if (lblTimestamp != null) {
                lblTimestamp.setText(LocalDateTime.now().format(formatter));
            }
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        setupFilters();
        configureTableColumns();
        ResponsiveTable.fillAvailableWidth(reportsTable);
        refreshTableData();

        // Refresh when either item reports or claim requests change
        AppDataStore.getItemReports().addListener((javafx.collections.ListChangeListener<ItemReport>) change -> {
            if (!showingArchived) refreshTableData();
        });
        AppDataStore.getClaimRequests().addListener((javafx.collections.ListChangeListener<ClaimRequest>) change -> {
            if (!showingArchived) refreshTableData();
        });
    }

    @FXML
    private void handleToggleArchivedItems() {
        showingArchived = !showingArchived;
        if (viewToggle != null) {
            viewToggle.setValue(showingArchived ? "Archived History" : "Active Reports");
        }
        if (showingArchived) {
            AppDataStore.refreshArchivedItems();
            AppDataStore.refreshArchivedClaims();
        }
        refreshTableData();
        updateArchiveButton();
        reportsTable.refresh();
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList(
                "All Types", "Lost", "Found", "Claim Report"));
        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Pending", "Ready to claim", "Approved", "Rejected"));
        dateFilter.setItems(FXCollections.observableArrayList(
                "All Dates", "Today", "A Day Ago", "A Week Ago", "A Year Ago", "Specific Month"
        ));
        typeFilter.getSelectionModel().selectFirst();
        statusFilter.getSelectionModel().selectFirst();
        dateFilter.getSelectionModel().selectFirst();

        if (viewToggle != null) {
            viewToggle.setItems(FXCollections.observableArrayList("Active Reports", "Archived History"));
            viewToggle.getSelectionModel().selectFirst();
            viewToggle.valueProperty().addListener((obs, oldVal, newVal) -> {
                showingArchived = "Archived History".equals(newVal);
                refreshTableData();
                updateArchiveButton();
            });
        }

        filteredData = new FilteredList<>(masterData, row -> true);
        reportsTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        dateFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateMonthPickerVisibility();
            applyFilter();
        });
        monthPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        updateMonthPickerVisibility();
        updateArchiveButton();
    }

    private void configureTableColumns() {
        colReportStatus.setCellValueFactory(new PropertyValueFactory<>("reviewStatus"));
        colClaimTrackingId.setCellValueFactory(new PropertyValueFactory<>("claimTrackingId"));
        colItemTrackingId.setCellValueFactory(new PropertyValueFactory<>("itemTrackingId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("reportKind"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colClaimant.setCellValueFactory(new PropertyValueFactory<>("claimantName"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Status badge column
        colReportStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(item);
                badge.setStyle(statusStyle(item));
                setGraphic(badge);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Report Kind badge column (Lost / Found / Claim Report)
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(item);
                if ("Lost".equalsIgnoreCase(item)) {
                    badge.setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; -fx-font-weight: bold; "
                            + "-fx-padding: 3 8 3 8; -fx-background-radius: 10;");
                } else if ("Found".equalsIgnoreCase(item)) {
                    badge.setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; "
                            + "-fx-padding: 3 8 3 8; -fx-background-radius: 10;");
                } else {
                    // Claim Report
                    badge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; "
                            + "-fx-padding: 3 8 3 8; -fx-background-radius: 10;");
                }
                setGraphic(badge);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn    = iconButton("/com/example/findit/assets/ViewEye.png", "View details");
            private final Button approveBtn = iconButton("/com/example/findit/assets/check.png", "Approve report");
            private final Button rejectBtn  = iconButton("/com/example/findit/assets/ekis.png", "Reject report");
            private final Button archiveBtn = new Button("Archive");
            private final Button restoreBtn = new Button("Restore");

            {
                approveBtn.setOnAction(e -> handleApprove(getTableView().getItems().get(getIndex())));
                rejectBtn.setOnAction(e  -> handleReject(getTableView().getItems().get(getIndex())));
                viewBtn.setOnAction(e    -> showReportDetails(getTableView().getItems().get(getIndex())));
                archiveBtn.setOnAction(e -> handleArchiveReport(getTableView().getItems().get(getIndex())));
                restoreBtn.setOnAction(e -> handleRestoreReport(getTableView().getItems().get(getIndex())));
                archiveBtn.setStyle("-fx-background-color: #800000; -fx-background-radius: 7; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 9 5 9;");
                restoreBtn.setStyle("-fx-background-color: #FFCC00; -fx-background-radius: 7; -fx-cursor: hand; -fx-text-fill: #4A1212; -fx-font-weight: bold; -fx-padding: 5 9 5 9;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ReportRow row = getTableRow().getItem();
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);

                if (showingArchived) {
                    box.getChildren().addAll(viewBtn, restoreBtn);
                } else if (row.isItemReport()) {
                    // Item reports (Lost/Found): view + archive only
                    box.getChildren().addAll(viewBtn, archiveBtn);
                } else if ("Pending".equalsIgnoreCase(row.getReviewStatus())) {
                    box.getChildren().addAll(viewBtn, approveBtn, rejectBtn, archiveBtn);
                } else {
                    box.getChildren().addAll(viewBtn, archiveBtn);
                }
                setGraphic(box);
            }
        });
    }

    private void refreshTableData() {
        java.util.List<ReportRow> rows = new java.util.ArrayList<>();

        if (showingArchived) {
            // Archived item reports
            AppDataStore.ARCHIVED_ITEMS.stream()
                    .map(ReportRow::fromItem)
                    .forEach(rows::add);
            // Archived claim reports
            AppDataStore.ARCHIVED_CLAIMS.stream()
                    .map(ReportRow::fromClaim)
                    .forEach(rows::add);
        } else {
            // Active item reports (Lost / Found submissions)
            AppDataStore.getItemReports().stream()
                    .map(ReportRow::fromItem)
                    .forEach(rows::add);
            // Active claim reports in the review queue
            AppDataStore.getClaimRequests().stream()
                    .filter(claim -> isReportQueueStatus(claim.getStatus()))
                    .map(ReportRow::fromClaim)
                    .forEach(rows::add);
        }

        masterData.setAll(rows);
        applyFilter();
    }

    private boolean isReportQueueStatus(String status) {
        return "Pending".equalsIgnoreCase(status)
                || "Ready to claim".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status);
    }

    private void applyFilter() {
        if (filteredData == null) {
            return;
        }

        String search = text(searchField.getText()).toLowerCase(Locale.ROOT);
        String kind = typeFilter.getValue();
        String status = statusFilter.getValue();

        filteredData.setPredicate(row -> {
            boolean matchesSearch = search.isBlank()
                    || row.getItemName().toLowerCase(Locale.ROOT).contains(search)
                    || row.getClaimTrackingId().toLowerCase(Locale.ROOT).contains(search)
                    || row.getItemTrackingId().toLowerCase(Locale.ROOT).contains(search)
                    || row.getClaimantName().toLowerCase(Locale.ROOT).contains(search)
                    || row.getCategory().toLowerCase(Locale.ROOT).contains(search)
                    || row.getLocation().toLowerCase(Locale.ROOT).contains(search);
            boolean matchesKind = kind == null || "All Types".equals(kind)
                    || row.getReportKind().equalsIgnoreCase(kind);
            boolean matchesStatus = status == null || "All Status".equals(status)
                    || row.getReviewStatus().equalsIgnoreCase(status);
            return matchesSearch && matchesKind && matchesStatus && matchesDate(row);
        });
    }

    private boolean matchesDate(ReportRow row) {
        String filter = dateFilter.getValue();
        if (filter == null || "All Dates".equals(filter)) {
            return true;
        }

        LocalDate itemDate = parseDate(row.getDate());
        if (itemDate == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        return switch (filter) {
            case "Today" -> itemDate.isEqual(today);
            case "A Day Ago" -> itemDate.isEqual(today.minusDays(1));
            case "A Week Ago" -> !itemDate.isBefore(today.minusWeeks(1));
            case "A Year Ago" -> !itemDate.isBefore(today.minusYears(1));
            case "Specific Month" -> matchesSelectedMonth(itemDate);
            default -> true;
        };
    }

    private boolean matchesSelectedMonth(LocalDate itemDate) {
        LocalDate selectedDate = monthPicker.getValue();
        return selectedDate == null || YearMonth.from(itemDate).equals(YearMonth.from(selectedDate));
    }

    private LocalDate parseDate(String date) {
        try {
            return date == null || date.isBlank() || "N/A".equals(date) ? null : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void updateMonthPickerVisibility() {
        boolean showMonth = "Specific Month".equals(dateFilter.getValue());
        monthPicker.setVisible(showMonth);
        monthPicker.setManaged(showMonth);
    }

    private void updateArchiveButton() {
        if (archivedItemsButton != null) {
            archivedItemsButton.setText(showingArchived ? "Active Reports" : "Archived Reports");
            archivedItemsButton.setStyle(showingArchived
                    ? "-fx-background-color: #FFCC00; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: #4A1212; -fx-font-weight: bold;"
                    : "-fx-background-color: #800000; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        }
    }

    private void handleApprove(ReportRow row) {
        if (row.isItemReport()) {
            return; // Item reports don't have an approve action
        }
        if (!confirm("Approve Report", "Approve this claim report and move it to Claims as Unclaimed?")) {
            return;
        }
        AppDataStore.updateClaimStatus(row.getRequest(), "Unclaimed");
        refreshTableData();
        toast.show(reportsTable.getScene().getWindow(), "Report approved. Claim is now Unclaimed.", "success");
    }

    private void handleReject(ReportRow row) {
        if (row.isItemReport()) {
            return;
        }
        if (!confirm("Reject Report", "Reject this claim report?")) {
            return;
        }
        AppDataStore.updateClaimStatus(row.getRequest(), "Rejected");
        refreshTableData();
        toast.show(reportsTable.getScene().getWindow(), "Report rejected.", "error");
    }

    private void handleArchiveReport(ReportRow row) {
        if (!confirm("Archive Report", "Move this report to archived history?")) {
            return;
        }
        if (row.isItemReport()) {
            AppDataStore.archiveItemReport(row.getItemReport());
        } else {
            AppDataStore.archiveClaimRequest(row.getRequest());
        }
        refreshTableData();
    }

    private void handleRestoreReport(ReportRow row) {
        if (!confirm("Restore Report", "Bring this report back to the active reports list?")) {
            return;
        }
        if (row.isItemReport()) {
            AppDataStore.restoreItemReport(row.getItemReport());
            AppDataStore.refreshArchivedItems();
        } else {
            AppDataStore.restoreClaimRequest(row.getRequest());
            AppDataStore.refreshArchivedClaims();
        }
        refreshTableData();
    }

    @FXML
    private void handleShowMatchSuggestions() {
        AppDataStore.refreshAll();
        showMatchSuggestionsDialog();
    }

    private void showMatchSuggestionsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Match Suggestions");
        dialog.setHeaderText("Potential lost and found matches");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(780);

        VBox list = new VBox(14);
        list.setPadding(new Insets(10));

        var matches = AppDataStore.getMatchSuggestions().stream()
                .filter(m -> !hasActiveClaimFlow(m.getFoundItem().getId()))
                .toList();

        if (matches.isEmpty()) {
            Label empty = new Label("No match suggestions available.");
            empty.setStyle("-fx-text-fill: #777777; -fx-font-size: 15;");
            list.getChildren().add(empty);
        } else {
            for (ItemMatch match : matches) {
                list.getChildren().add(createMatchSuggestionCard(match, dialog));
            }
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(460);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
        refreshTableData();
    }

    private boolean hasActiveClaimFlow(int itemId) {
        return AppDataStore.getClaimRequests().stream()
                .anyMatch(c -> c.getItem().getId() == itemId
                        && ("Pending".equalsIgnoreCase(c.getStatus())
                        || "Approved".equalsIgnoreCase(c.getStatus())
                        || "Unclaimed".equalsIgnoreCase(c.getStatus())
                        || "Claimed".equalsIgnoreCase(c.getStatus())));
    }

    private VBox createMatchSuggestionCard(ItemMatch match, Dialog<ButtonType> parentDialog) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E6E6E6; -fx-border-radius: 10;");

        Label title = new Label("Potential Match");
        title.setStyle("-fx-text-fill: #4A1212; -fx-font-weight: bold; -fx-font-size: 16;");

        Label status = new Label(match.getStatus());
        status.setStyle("-fx-background-color: #FFE0B2; -fx-background-radius: 12; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-padding: 3 10 3 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, status);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox comparison = new HBox(14,
                createMatchItemSummary(match.getLostItem()),
                createMatchItemSummary(match.getFoundItem())
        );

        Button detailsButton = new Button("View Match Details");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setPrefHeight(38);
        detailsButton.setStyle("-fx-background-color: #800000; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        detailsButton.setOnAction(event -> {
            javafx.stage.Window mainWindow = reportsTable.getScene() == null
                    ? null : reportsTable.getScene().getWindow();
            MatchDetailsDialog.show(mainWindow, match);
            AppDataStore.refreshAll();
            parentDialog.close();
            showMatchSuggestionsDialog();
        });

        card.getChildren().addAll(header, comparison, detailsButton);
        return card;
    }

    private VBox createMatchItemSummary(ItemReport item) {
        VBox box = new VBox(5);
        box.setPrefWidth(320);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle("Lost".equalsIgnoreCase(item.getType())
                ? "-fx-border-color: #FFCDD2; -fx-border-radius: 8; -fx-border-width: 1.5;"
                : "-fx-border-color: #A5D6A7; -fx-border-radius: 8; -fx-border-width: 1.5;");
        HBox.setHgrow(box, Priority.ALWAYS);

        Label type = new Label(item.getType().toUpperCase(Locale.ROOT) + " ITEM");
        type.setStyle("Lost".equalsIgnoreCase(item.getType())
                ? "-fx-text-fill: #E53935; -fx-font-weight: bold; -fx-font-size: 10;"
                : "-fx-text-fill: #43A047; -fx-font-weight: bold; -fx-font-size: 10;");
        Label name = new Label(item.getItemName());
        name.setWrapText(true);
        name.setStyle("-fx-text-fill: #222222; -fx-font-weight: bold;");
        Label reporter = new Label("Reported by: " + display(item.getReportedBy()));
        reporter.setWrapText(true);
        reporter.setStyle("-fx-text-fill: #777777; -fx-font-size: 12;");

        box.getChildren().addAll(type, name, reporter);
        return box;
    }

    private void openMatchDetail(ItemMatch match) {
        MatchDetailsDialog.show(reportsTable.getScene() == null ? null : reportsTable.getScene().getWindow(), match);
    }

    private boolean confirm(String title, String message) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle(title);
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText(message);
        return confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showReportDetails(ReportRow row) {
        if (row.isItemReport()) {
            showItemReportDetails(row);
        } else {
            showClaimReportDetails(row);
        }
    }

    private void showItemReportDetails(ReportRow row) {
        ItemReport item = row.getItemReport();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Item Report Details");
        dialog.setHeaderText(item.getItemName() + " — " + item.getType() + " Report");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(820);

        HBox content = new HBox(22);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(10, 5, 5, 5));

        VBox details = new VBox(14);
        details.setPrefWidth(490);
        details.getChildren().add(createSection("Item Details", createItemOnlyGrid(item)));
        HBox.setHgrow(details, Priority.ALWAYS);

        content.getChildren().addAll(createImagePanel(item), details);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showClaimReportDetails(ReportRow row) {
        ClaimRequest claim = row.getRequest();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Claim Report Details");
        dialog.setHeaderText(row.getItemName() + " - " + row.getReviewStatus());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(820);

        HBox content = new HBox(22);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(10, 5, 5, 5));

        VBox details = new VBox(14);
        details.setPrefWidth(490);
        details.getChildren().addAll(
                createSection("Claim Report", createClaimGrid(claim, row)),
                createSection("Item Report", createItemGrid(claim.getItem())),
                createProofBlock(claim)
        );
        HBox.setHgrow(details, Priority.ALWAYS);

        content.getChildren().addAll(createImagePanel(claim.getItem()), details);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private VBox createImagePanel(ItemReport item) {
        VBox panel = new VBox(8);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(260);

        StackPane imageFrame = new StackPane();
        imageFrame.setPrefSize(260, 240);
        imageFrame.setStyle("-fx-background-color: #F0F0F3; -fx-background-radius: 10;");

        Image image = ImageStorage.loadImage(item.getImagePath());
        if (image == null) {
            Label placeholder = new Label("No Image Available");
            placeholder.setStyle("-fx-text-fill: #777777; -fx-font-weight: bold;");
            imageFrame.getChildren().add(placeholder);
        } else {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(240);
            imageView.setFitHeight(220);
            imageView.setPreserveRatio(true);
            imageFrame.getChildren().add(imageView);
        }

        Label imageLabel = new Label(item.getItemName());
        imageLabel.setWrapText(true);
        imageLabel.setStyle("-fx-text-fill: #4A1212; -fx-font-weight: bold;");
        panel.getChildren().addAll(imageFrame, imageLabel);
        return panel;
    }

    private VBox createSection(String titleText, GridPane grid) {
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #4A1212; -fx-font-weight: bold; -fx-font-size: 13;");
        return new VBox(7, title, grid);
    }

    private GridPane createItemOnlyGrid(ItemReport item) {
        GridPane grid = createDetailsGrid();
        addDetailRow(grid, 0, "Item Tracking ID", item.getTrackingId());
        addDetailRow(grid, 1, "Type", item.getType());
        addDetailRow(grid, 2, "Item Name", item.getItemName());
        addDetailRow(grid, 3, "Category", item.getCategory());
        addDetailRow(grid, 4, "Date", item.getDate());
        addDetailRow(grid, 5, "Location", item.getLocation());
        addDetailRow(grid, 6, "Reported By", item.getReportedBy());
        addDetailRow(grid, 7, "Contact", item.getContact());
        addDetailRow(grid, 8, "Description", item.getDescription());
        return grid;
    }

    private GridPane createClaimGrid(ClaimRequest claim, ReportRow row) {
        GridPane grid = createDetailsGrid();
        addDetailRow(grid, 0, "Report Status", row.getReviewStatus());
        addDetailRow(grid, 1, "Claim Tracking ID", claim.getTrackingId());
        addDetailRow(grid, 2, "Claimant", claim.getClaimantName());
        addDetailRow(grid, 3, "Student Number", claim.getStudentNumber());
        addDetailRow(grid, 4, "Contact", claim.getContactInfo());
        addDetailRow(grid, 5, "Workflow Status", claim.getStatus());
        return grid;
    }

    private GridPane createItemGrid(ItemReport item) {
        GridPane grid = createDetailsGrid();
        addDetailRow(grid, 0, "Item Tracking ID", item.getTrackingId());
        addDetailRow(grid, 1, "Type", item.getType());
        addDetailRow(grid, 2, "Category", item.getCategory());
        addDetailRow(grid, 3, "Date", item.getDate());
        addDetailRow(grid, 4, "Location", item.getLocation());
        addDetailRow(grid, 5, "Reported By", item.getReportedBy());
        addDetailRow(grid, 6, "Reporter Contact", item.getContact());
        addDetailRow(grid, 7, "Description", item.getDescription());
        return grid;
    }

    private VBox createProofBlock(ClaimRequest claim) {
        Label title = new Label("Proof");
        title.setStyle("-fx-text-fill: #4A1212; -fx-font-weight: bold; -fx-font-size: 13;");

        Label proof = new Label(display(claim.getProofDescription()));
        proof.setWrapText(true);
        proof.setMinHeight(80);
        proof.setStyle("-fx-background-color: #F7F7F9; -fx-background-radius: 8; -fx-padding: 10; -fx-text-fill: #333333;");
        return new VBox(7, title, proof);
    }

    private GridPane createDetailsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(130);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);
        return grid;
    }

    private void addDetailRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText + ":");
        label.setStyle("-fx-text-fill: #777777; -fx-font-weight: bold;");

        Label value = new Label(display(valueText));
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #222222;");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private Button iconButton(String path, String tooltip) {
        Button button = new Button();
        button.setGraphic(createIcon(path));
        button.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private ImageView createIcon(String path) {
        java.io.InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Missing table icon: " + path);
            return new ImageView();
        }
        ImageView imgView = new ImageView(new Image(stream));
        imgView.setFitWidth(20);
        imgView.setFitHeight(20);
        imgView.setPreserveRatio(true);
        return imgView;
    }

    private String statusStyle(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 12;";
        }
        if ("Rejected".equalsIgnoreCase(status)) {
            return "-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 12;";
        }
        if ("Ready to claim".equalsIgnoreCase(status)) {
            return "-fx-background-color: #FFF9C4; -fx-text-fill: #F57F17; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 12;";
        }
        if ("Item Report".equalsIgnoreCase(status)) {
            return "-fx-background-color: #E8EAF6; -fx-text-fill: #3F51B5; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 12;";
        }
        // Pending
        return "-fx-background-color: #FFE0B2; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 12;";
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    public static class ReportRow {
        private final ClaimRequest request;   // null for item reports
        private final ItemReport   itemReport; // null for claim reports

        private ReportRow(ClaimRequest request, ItemReport itemReport) {
            this.request    = request;
            this.itemReport = itemReport;
        }

        /** Factory: wrap a ClaimRequest (Claim Report row). */
        public static ReportRow fromClaim(ClaimRequest claim) {
            return new ReportRow(claim, null);
        }

        /** Factory: wrap an ItemReport (Item Report row). */
        public static ReportRow fromItem(ItemReport item) {
            return new ReportRow(null, item);
        }

        public boolean isItemReport()  { return itemReport != null; }
        public ClaimRequest getRequest()    { return request; }
        public ItemReport   getItemReport() { return itemReport; }

        /** Displayed in the Status badge column. */
        public String getReviewStatus() {
            if (isItemReport()) {
                return "Item Report";
            }
            String status = display(request.getStatus());
            if ("Rejected".equalsIgnoreCase(status)) return "Rejected";
            if ("Ready to claim".equalsIgnoreCase(status)) return "Ready to claim";
            if ("Approved".equalsIgnoreCase(status)
                    || "Unclaimed".equalsIgnoreCase(status)
                    || "Claimed".equalsIgnoreCase(status)) return "Approved";
            return "Pending";
        }

        /** Displayed in the Type/Kind badge column. */
        public String getReportKind() {
            if (isItemReport()) {
                // Show "Lost" or "Found" for item reports
                return display(itemReport.getType());
            }
            return "Claim Report";
        }

        public String getClaimTrackingId() {
            return isItemReport() ? "—" : display(request.getTrackingId());
        }

        public String getItemTrackingId() {
            ItemReport item = isItemReport() ? itemReport : request.getItem();
            return display(item.getTrackingId());
        }

        public String getItemName() {
            ItemReport item = isItemReport() ? itemReport : request.getItem();
            return display(item.getItemName());
        }

        /** Legacy getter kept for any leftover references; use getReportKind() for the column. */
        public String getType() {
            return isItemReport() ? display(itemReport.getType()) : display(request.getItem().getType());
        }

        public String getCategory() {
            ItemReport item = isItemReport() ? itemReport : request.getItem();
            return display(item.getCategory());
        }

        public String getDate() {
            ItemReport item = isItemReport() ? itemReport : request.getItem();
            return display(item.getDate());
        }

        public String getClaimantName() {
            return isItemReport() ? display(itemReport.getReportedBy()) : display(request.getClaimantName());
        }

        public String getLocation() {
            ItemReport item = isItemReport() ? itemReport : request.getItem();
            return display(item.getLocation());
        }

        private static String display(String value) {
            return value == null || value.isBlank() ? "N/A" : value;
        }
    }
}
