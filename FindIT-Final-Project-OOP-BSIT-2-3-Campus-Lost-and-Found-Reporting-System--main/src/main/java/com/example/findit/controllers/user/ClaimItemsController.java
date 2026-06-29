package com.example.findit.controllers.user;

import com.example.findit.model.AppDataStore;
import com.example.findit.model.ClaimRequest;
import com.example.findit.model.ItemReport;
import com.example.findit.util.InputValidator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClaimItemsController {

    @FXML private TextField txtClaimantName;
    @FXML private TextField txtContact;
    @FXML private TextArea txtProofDescription;
    private ItemReport item;

    public void setItem(ItemReport item) {
        this.item = item;
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        if (item == null) {
            showAlert(Alert.AlertType.ERROR, "Item Missing", "Please open a claim from an item details window.");
            return;
        }

        if (!"Found".equalsIgnoreCase(item.getType())) {
            showAlert(Alert.AlertType.WARNING, "Claim Unavailable",
                    "Only found item reports can be claimed.");
            return;
        }

        if (txtClaimantName.getText().isBlank() || txtContact.getText().isBlank()
                || txtProofDescription.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in all required fields.");
            return;
        }

        if (!InputValidator.isValidContact(txtContact.getText())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Contact",
                    "Please enter a valid 11-digit phone number or email address.");
            return;
        }

        if (!InputValidator.isValidNameText(txtClaimantName.getText())
                || !InputValidator.isValidDescriptionText(txtProofDescription.getText())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Characters",
                    "Please remove unsupported special characters from the claim.");
            return;
        }

        ClaimRequest savedClaim;
        try {
            savedClaim = AppDataStore.addClaimRequest(
                    item,
                    txtClaimantName.getText().trim(),
                    "",
                    txtContact.getText().trim(),
                    txtProofDescription.getText().trim()
            );
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "The claim could not be saved. Please try again.");
            return;
        }

        showClaimTrackingReceipt(savedClaim);
        closeWindow(event);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showClaimTrackingReceipt(ClaimRequest savedClaim) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Claim Submitted");
        alert.setHeaderText("Success! Your Claim is Saved.");
        ButtonType doneButton = new ButtonType("Done");
        alert.getButtonTypes().setAll(doneButton);

        TextField idField = new TextField(savedClaim.getTrackingId());
        idField.setEditable(false);
        idField.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center; -fx-background-color: #F0F0F0;");

        VBox layout = new VBox(15,
                new Label("Keep this ID to track your claim report later."),
                new Label("Please screenshot or take a picture of this tracking ID before tapping Done."),
                idField
        );
        layout.setAlignment(Pos.CENTER);
        alert.getDialogPane().setContent(layout);
        Button done = (Button) alert.getDialogPane().lookupButton(doneButton);
        done.setStyle("-fx-cursor: hand; -fx-background-color: #800000; -fx-text-fill: white; -fx-font-weight: bold;");
        alert.showAndWait();
    }
}
