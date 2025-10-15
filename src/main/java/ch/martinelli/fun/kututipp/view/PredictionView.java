package ch.martinelli.fun.kututipp.view;

import ch.martinelli.fun.kututipp.db.enums.GenderType;
import ch.martinelli.fun.kututipp.dto.CompetitionDto;
import ch.martinelli.fun.kututipp.dto.CompetitionEntryDto;
import ch.martinelli.fun.kututipp.dto.PredictionInputDto;
import ch.martinelli.fun.kututipp.service.PredictionService;
import ch.martinelli.fun.kututipp.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View for making predictions on gymnast performances.
 * Implements UC-008: Make Predictions and UC-010: Edit Predictions.
 */
@PermitAll
@Route("predictions")
@PageTitle("Make Predictions - Kutu-Tipp")
public class PredictionView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(PredictionView.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final transient PredictionService predictionService;
    private final Long currentUserId;
    private final String currentUsername;

    // UI Components
    private ComboBox<CompetitionDto> competitionComboBox;
    private Span deadlineLabel;
    private Span countdownLabel;
    private Grid<CompetitionEntryDto> grid;
    private ListDataProvider<CompetitionEntryDto> dataProvider;
    private final Span predictionCountLabel;
    private Button saveDraftButton;
    private Button submitButton;
    private Button clearAllButton;

    // Filters
    private TextField gymnastFilter;
    private ComboBox<String> teamFilter;
    private ComboBox<String> apparatusFilter;
    private ComboBox<GenderType> genderFilter;

    // Data
    private CompetitionDto selectedCompetition;
    private List<CompetitionEntryDto> entries = new ArrayList<>();
    private final Map<Long, BigDecimal> predictionInputs = new HashMap<>();

    public PredictionView(PredictionService predictionService, UserService userService) {
        this.predictionService = predictionService;

        // Get current user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        this.currentUsername = authentication.getName();
        this.currentUserId = userService.getCurrentUserId(currentUsername);

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        add(new H1("Make Predictions"));

        // Description
        add(new Paragraph(
                "Select a competition and enter your predictions for gymnast scores. " +
                        "Predictions must be submitted at least 30 minutes before competition start."
        ));

        // Competition selector
        add(createCompetitionSelector());

        // Deadline info (initially hidden)
        add(createDeadlineInfo());

        // Filter panel
        add(createFilterPanel());

        // Prediction count
        predictionCountLabel = new Span();
        predictionCountLabel.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)");
        add(predictionCountLabel);

        // Grid
        createGrid();
        add(grid);

        // Action buttons
        add(createActionButtons());

        // Load available competitions
        loadCompetitions();
    }

    /**
     * Creates the competition selector.
     */
    private HorizontalLayout createCompetitionSelector() {
        var layout = new HorizontalLayout();
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        competitionComboBox = new ComboBox<>("Select Competition");
        competitionComboBox.setItemLabelGenerator(CompetitionDto::name);
        competitionComboBox.setWidth("400px");
        competitionComboBox.addValueChangeListener(event -> {
            selectedCompetition = event.getValue();
            loadCompetitionEntries();
        });

        layout.add(competitionComboBox);
        return layout;
    }

    /**
     * Creates the deadline information display.
     */
    private VerticalLayout createDeadlineInfo() {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setVisible(false); // Hidden until competition selected

        deadlineLabel = new Span();
        deadlineLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        countdownLabel = new Span();
        countdownLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "bold");

        layout.add(deadlineLabel, countdownLabel);
        return layout;
    }

    /**
     * Creates the filter panel.
     */
    private VerticalLayout createFilterPanel() {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setVisible(false); // Hidden until competition selected

        var filterBar = new HorizontalLayout();
        filterBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        // Gymnast name filter
        gymnastFilter = new TextField("Gymnast");
        gymnastFilter.setPlaceholder("Filter by name...");
        gymnastFilter.setClearButtonVisible(true);
        gymnastFilter.setWidth("200px");
        gymnastFilter.addValueChangeListener(e -> applyFilters());

        // Team filter
        teamFilter = new ComboBox<>("Team");
        teamFilter.setPlaceholder("All teams");
        teamFilter.setClearButtonVisible(true);
        teamFilter.setWidth("200px");
        teamFilter.addValueChangeListener(e -> applyFilters());

        // Apparatus filter
        apparatusFilter = new ComboBox<>("Apparatus");
        apparatusFilter.setPlaceholder("All apparatus");
        apparatusFilter.setClearButtonVisible(true);
        apparatusFilter.setWidth("200px");
        apparatusFilter.addValueChangeListener(e -> applyFilters());

        // Gender filter
        genderFilter = new ComboBox<>("Gender");
        genderFilter.setItems(GenderType.values());
        genderFilter.setPlaceholder("All");
        genderFilter.setClearButtonVisible(true);
        genderFilter.setWidth("150px");
        genderFilter.addValueChangeListener(e -> applyFilters());

        var clearFiltersButton = new Button("Clear Filters", e -> clearFilters());
        clearFiltersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        filterBar.add(gymnastFilter, teamFilter, apparatusFilter, genderFilter, clearFiltersButton);
        layout.add(filterBar);

        return layout;
    }

    /**
     * Creates the grid for competition entries.
     */
    private void createGrid() {
        grid = new Grid<>();
        grid.setHeight("500px");

        // Gymnast column
        grid.addColumn(CompetitionEntryDto::gymnastName)
                .setHeader("Gymnast")
                .setSortable(true)
                .setAutoWidth(true);

        // Team column
        grid.addColumn(CompetitionEntryDto::teamName)
                .setHeader("Team")
                .setSortable(true)
                .setAutoWidth(true);

        // Apparatus column
        grid.addColumn(CompetitionEntryDto::apparatusName)
                .setHeader("Apparatus")
                .setSortable(true)
                .setAutoWidth(true);

        // Gender column
        grid.addColumn(CompetitionEntryDto::gender)
                .setHeader("Gender")
                .setWidth("100px")
                .setFlexGrow(0);

        // Predicted score column (editable)
        grid.addComponentColumn(entry -> {
            var field = new BigDecimalField();
            field.setWidth("150px");
            field.setPlaceholder("0.000");

            // Set existing prediction if available
            if (entry.predictedScore() != null) {
                field.setValue(entry.predictedScore());
                predictionInputs.put(entry.competitionEntryId(), entry.predictedScore());
            }

            // Update map when value changes
            field.addValueChangeListener(e -> {
                var value = e.getValue();
                if (value != null && value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(new BigDecimal("20.000")) <= 0) {
                    predictionInputs.put(entry.competitionEntryId(), value);
                    field.setInvalid(false);
                } else if (value != null) {
                    field.setInvalid(true);
                    field.setErrorMessage("Score must be between 0.000 and 20.000");
                    predictionInputs.remove(entry.competitionEntryId());
                } else {
                    predictionInputs.remove(entry.competitionEntryId());
                }
                updatePredictionCount();
            });

            return field;
        }).setHeader("Predicted Score");

        // Add visual indicator for rows with existing predictions
        grid.setClassNameGenerator(entry -> {
            if (entry.hasPrediction()) {
                return "has-prediction";
            }
            return null;
        });

        dataProvider = new ListDataProvider<>(entries);
        grid.setDataProvider(dataProvider);
    }

    /**
     * Creates the action buttons.
     */
    private HorizontalLayout createActionButtons() {
        var layout = new HorizontalLayout();
        layout.setVisible(false); // Hidden until competition selected

        saveDraftButton = new Button("Save Draft", new Icon(VaadinIcon.UPLOAD), e -> saveDraft());
        saveDraftButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        clearAllButton = new Button("Clear All", new Icon(VaadinIcon.TRASH), e -> confirmClearAll());
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        submitButton = new Button("Submit Predictions", new Icon(VaadinIcon.CHECK), e -> confirmSubmit());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(saveDraftButton, clearAllButton, submitButton);
        return layout;
    }

    /**
     * Loads available competitions.
     */
    private void loadCompetitions() {
        try {
            var competitions = predictionService.getAvailableCompetitions();
            competitionComboBox.setItems(competitions);

            if (competitions.isEmpty()) {
                Notification.show(
                        "No competitions available for predictions at this time.",
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            }
        } catch (Exception e) {
            log.error("Error loading competitions", e);
            Notification.show(
                    "Error loading competitions: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Loads competition entries for the selected competition.
     */
    private void loadCompetitionEntries() {
        if (selectedCompetition == null) {
            entries.clear();
            dataProvider.refreshAll();
            predictionInputs.clear();
            updateVisibility(false);
            return;
        }

        try {
            entries = predictionService.getCompetitionEntriesWithPredictions(
                    selectedCompetition.id(),
                    currentUserId
            );

            // Populate prediction inputs from existing predictions
            predictionInputs.clear();
            for (var entry : entries) {
                if (entry.predictedScore() != null) {
                    predictionInputs.put(entry.competitionEntryId(), entry.predictedScore());
                }
            }

            dataProvider = new ListDataProvider<>(entries);
            grid.setDataProvider(dataProvider);

            // Update filter options
            updateFilterOptions();

            // Update deadline info
            updateDeadlineInfo();

            // Update prediction count
            updatePredictionCount();

            // Show filters and buttons
            updateVisibility(true);

            log.debug("Loaded {} entries for competition {}", entries.size(), selectedCompetition.name());
        } catch (Exception e) {
            log.error("Error loading competition entries", e);
            Notification.show(
                    "Error loading competition entries: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Updates filter dropdown options based on current entries.
     */
    private void updateFilterOptions() {
        var teams = entries.stream()
                .map(CompetitionEntryDto::teamName)
                .distinct()
                .sorted()
                .toList();
        teamFilter.setItems(teams);

        var apparatus = entries.stream()
                .map(CompetitionEntryDto::apparatusName)
                .distinct()
                .sorted()
                .toList();
        apparatusFilter.setItems(apparatus);
    }

    /**
     * Updates deadline information display.
     */
    private void updateDeadlineInfo() {
        if (selectedCompetition == null) {
            return;
        }

        var deadline = selectedCompetition.getPredictionDeadline();
        deadlineLabel.setText("Prediction deadline: " + deadline.format(DATE_TIME_FORMATTER));

        // Calculate time remaining
        var now = OffsetDateTime.now();
        var duration = Duration.between(now, deadline);

        if (duration.isNegative()) {
            countdownLabel.setText("DEADLINE PASSED");
            countdownLabel.getStyle().set("color", "var(--lumo-error-color)");
            disableEditing();
        } else {
            var hours = duration.toHours();
            var minutes = duration.toMinutesPart();
            countdownLabel.setText(String.format("Time remaining: %d hours, %d minutes", hours, minutes));

            if (hours < 1) {
                countdownLabel.getStyle().set("color", "var(--lumo-error-color)");
            } else {
                countdownLabel.getStyle().set("color", "var(--lumo-success-color)");
            }
        }
    }

    /**
     * Applies filters to the grid.
     */
    private void applyFilters() {
        dataProvider.clearFilters();

        // Gymnast name filter
        if (gymnastFilter.getValue() != null && !gymnastFilter.getValue().isBlank()) {
            dataProvider.addFilter(entry ->
                    entry.gymnastName().toLowerCase().contains(gymnastFilter.getValue().toLowerCase())
            );
        }

        // Team filter
        if (teamFilter.getValue() != null) {
            dataProvider.addFilter(entry -> entry.teamName().equals(teamFilter.getValue()));
        }

        // Apparatus filter
        if (apparatusFilter.getValue() != null) {
            dataProvider.addFilter(entry -> entry.apparatusName().equals(apparatusFilter.getValue()));
        }

        // Gender filter
        if (genderFilter.getValue() != null) {
            dataProvider.addFilter(entry -> entry.gender() == genderFilter.getValue());
        }
    }

    /**
     * Clears all filters.
     */
    private void clearFilters() {
        gymnastFilter.clear();
        teamFilter.clear();
        apparatusFilter.clear();
        genderFilter.clear();
        dataProvider.clearFilters();
    }

    /**
     * Updates the prediction count label.
     */
    private void updatePredictionCount() {
        var count = predictionInputs.size();
        var total = entries.size();
        predictionCountLabel.setText(String.format("Predictions entered: %d of %d", count, total));

        // Update button states
        saveDraftButton.setEnabled(count > 0);
        submitButton.setEnabled(count > 0);
    }

    /**
     * Updates visibility of filters and action buttons.
     */
    private void updateVisibility(boolean visible) {
        getChildren()
                .filter(component -> component instanceof VerticalLayout || component instanceof HorizontalLayout)
                .forEach(component -> {
                    if (component.getElement().hasAttribute("slot")) {
                        return; // Skip slotted components
                    }
                    if (component != competitionComboBox.getParent().orElse(null)) {
                        component.setVisible(visible);
                    }
                });
        deadlineLabel.getParent().ifPresent(parent -> parent.setVisible(visible));
        predictionCountLabel.setVisible(visible);
    }

    /**
     * Disables editing when deadline has passed.
     */
    private void disableEditing() {
        saveDraftButton.setEnabled(false);
        submitButton.setEnabled(false);
        clearAllButton.setEnabled(false);
    }

    /**
     * Saves predictions as draft.
     */
    private void saveDraft() {
        if (predictionInputs.isEmpty()) {
            Notification.show("Please enter at least one prediction", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        try {
            var predictions = predictionInputs.entrySet().stream()
                    .map(entry -> new PredictionInputDto(entry.getKey(), entry.getValue()))
                    .toList();

            var savedCount = predictionService.savePredictions(currentUserId, predictions);

            Notification.show(
                    String.format("Draft saved: %d predictions", savedCount),
                    3000,
                    Notification.Position.BOTTOM_START
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            log.info("User {} saved draft with {} predictions", currentUsername, savedCount);
        } catch (Exception e) {
            log.error("Error saving draft", e);
            Notification.show(
                    "Error saving draft: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Confirms and submits predictions.
     */
    private void confirmSubmit() {
        if (predictionInputs.isEmpty()) {
            Notification.show("Please enter at least one prediction", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        var dialog = new ConfirmDialog();
        dialog.setHeader("Submit Predictions");
        dialog.setText(String.format(
                "Are you sure you want to submit %d predictions? You can still edit them before the deadline.",
                predictionInputs.size()
        ));
        dialog.setCancelable(true);
        dialog.setConfirmText("Submit");
        dialog.addConfirmListener(event -> submitPredictions());
        dialog.open();
    }

    /**
     * Submits predictions.
     */
    private void submitPredictions() {
        try {
            var predictions = predictionInputs.entrySet().stream()
                    .map(entry -> new PredictionInputDto(entry.getKey(), entry.getValue()))
                    .toList();

            var savedCount = predictionService.savePredictions(currentUserId, predictions);

            Notification.show(
                    String.format("Successfully submitted %d predictions!", savedCount),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            log.info("User {} submitted {} predictions for competition {}",
                    currentUsername, savedCount, selectedCompetition.name());

            // Reload to show updated data
            loadCompetitionEntries();
        } catch (PredictionService.PredictionDeadlinePassedException e) {
            log.warn("Deadline passed during submission: {}", e.getMessage());
            Notification.show(
                    "Deadline has passed! Predictions cannot be submitted.",
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            updateDeadlineInfo();
        } catch (PredictionService.PredictionValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            Notification.show(
                    "Validation error: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            log.error("Error submitting predictions", e);
            Notification.show(
                    "Error submitting predictions: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Confirms and clears all predictions.
     */
    private void confirmClearAll() {
        if (predictionInputs.isEmpty()) {
            return;
        }

        var dialog = new ConfirmDialog();
        dialog.setHeader("Clear All Predictions");
        dialog.setText("Are you sure you want to clear all predictions? This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Clear All");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> clearAllPredictions());
        dialog.open();
    }

    /**
     * Clears all predictions.
     */
    private void clearAllPredictions() {
        try {
            var deletedCount = predictionService.deleteAllPredictions(currentUserId, selectedCompetition.id());

            predictionInputs.clear();
            loadCompetitionEntries(); // Reload to reflect changes

            Notification.show(
                    String.format("Cleared %d predictions", deletedCount),
                    3000,
                    Notification.Position.BOTTOM_START
            ).addThemeVariants(NotificationVariant.LUMO_CONTRAST);

            log.info("User {} cleared {} predictions for competition {}",
                    currentUsername, deletedCount, selectedCompetition.name());
        } catch (Exception e) {
            log.error("Error clearing predictions", e);
            Notification.show(
                    "Error clearing predictions: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Handles navigation with query parameters.
     * UC-010: Allows pre-selecting a competition when navigating from My Predictions.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var location = event.getLocation();
        var queryParams = location.getQueryParameters();

        // Check if competition ID is provided
        var competitionIdParam = queryParams.getParameters().get("competitionId");
        if (competitionIdParam != null && !competitionIdParam.isEmpty()) {
            try {
                var competitionId = Long.parseLong(competitionIdParam.getFirst());
                // Set the competition after the view is attached
                getElement().executeJs("return true").then(_ -> selectCompetitionById(competitionId));
            } catch (NumberFormatException _) {
                log.warn("Invalid competition ID parameter: {}", competitionIdParam.getFirst());
            }
        }
    }

    /**
     * Selects a competition by ID.
     *
     * @param competitionId The competition ID to select
     */
    private void selectCompetitionById(Long competitionId) {
        // Find the competition in the combo box
        var competitions = competitionComboBox.getListDataView().getItems().toList();
        competitions.stream()
                .filter(comp -> comp.id().equals(competitionId))
                .findFirst()
                .ifPresent(comp -> {
                    competitionComboBox.setValue(comp);
                    log.debug("Pre-selected competition: {}", comp.name());
                });
    }
}
