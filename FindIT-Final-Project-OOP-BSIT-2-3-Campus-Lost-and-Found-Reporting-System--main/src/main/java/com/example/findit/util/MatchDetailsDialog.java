package com.example.findit.util;

import com.example.findit.controllers.admin.MatchSuggestionController;
import com.example.findit.model.ItemMatch;
import com.example.findit.model.ItemReport;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class MatchDetailsDialog {
    private MatchDetailsDialog() {
    }

    public static void show(Window owner, ItemMatch match) {
        if (match == null) {
            showFallback(owner, null);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(MatchDetailsDialog.class.getResource(
                    "/com/example/findit/views/admin/MatchSuggestion.fxml"
            ));
            Parent root = loader.load();

            MatchSuggestionController controller = loader.getController();
            controller.setOwnerWindow(owner);
            controller.loadMatch(match);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Match Details");
            dialog.setMinWidth(920);
            dialog.setMinHeight(650);
            dialog.setScene(new Scene(root, 920, 650));
            dialog.centerOnScreen();
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showFallback(owner, match);
        }
    }

    private static void showFallback(Window owner, ItemMatch match) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Match Details");
        alert.setHeaderText("Potential lost and found match");
        alert.getButtonTypes().setAll(ButtonType.CLOSE);
        if (owner != null) {
            alert.initOwner(owner);
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(16);
        grid.setVgap(8);

        if (match == null) {
            grid.add(new Label("No match details are available."), 0, 0);
        } else {
            addItem(grid, 0, "Lost Item", match.getLostItem());
            addItem(grid, 2, "Found Item", match.getFoundItem());
        }

        alert.getDialogPane().setContent(grid);
        alert.showAndWait();
    }

    private static void addItem(GridPane grid, int column, String title, ItemReport item) {
        grid.add(header(title), column, 0);
        grid.add(value("Item: " + display(item.getItemName())), column, 1);
        grid.add(value("Category: " + display(item.getCategory())), column, 2);
        grid.add(value("Reported by: " + display(item.getReportedBy())), column, 3);
        grid.add(value("Contact: " + display(item.getContact())), column, 4);
        grid.add(value("Date: " + display(item.getDate())), column, 5);
        grid.add(value("Location: " + display(item.getLocation())), column, 6);
        grid.add(value("Description: " + display(item.getDescription())), column, 7);
    }

    private static Label header(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #4A1212; -fx-font-weight: bold; -fx-font-size: 15;");
        return label;
    }

    private static Label value(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(360);
        return label;
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
