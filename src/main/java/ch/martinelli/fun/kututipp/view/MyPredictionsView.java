package ch.martinelli.fun.kututipp.view;

import ch.martinelli.fun.kututipp.dto.UserCompetitionSummaryDto;
import ch.martinelli.fun.kututipp.repository.PredictionRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import static ch.martinelli.fun.kututipp.db.Tables.APP_USER;

/**
 * View for displaying and managing user's existing predictions.
 * Implements UC-010: Edit Predictions.
 */
@PermitAll
@Route("my-predictions")
@PageTitle("My Predictions - Kutu-Tipp")
public class MyPredictionsView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(MyPredictionsView.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final transient PredictionRepository predictionRepository;
    private final Long currentUserId;
    private final String currentUsername;

    private final Grid<UserCompetitionSummaryDto> grid;

    public MyPredictionsView(PredictionRepository predictionRepository, DSLContext dsl) {
        this.predictionRepository = predictionRepository;

        // Get current user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        this.currentUsername = authentication.getName();
        this.currentUserId = dsl.selectFrom(APP_USER)
                .where(APP_USER.USERNAME.eq(currentUsername))
                .fetchOne()
                .getId();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        add(new H1("My Predictions"));

        // Description
        var description = new Paragraph(
                "View and edit your predictions for all competitions. Click on a competition to modify your predictions."
        );
        add(description);

        // Create grid
        grid = createGrid();
        add(grid);

        // Load data
        loadCompetitions();
    }

    /**
     * Creates and configures the grid.
     */
    private Grid<UserCompetitionSummaryDto> createGrid() {
        var competitionGrid = new Grid<UserCompetitionSummaryDto>();
        competitionGrid.setHeight("600px");

        // Competition name column
        competitionGrid.addColumn(UserCompetitionSummaryDto::competitionName)
                .setHeader("Competition")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Date column
        competitionGrid.addColumn(summary -> summary.competitionDate().format(DATE_TIME_FORMATTER))
                .setHeader("Date")
                .setWidth("150px")
                .setFlexGrow(0);

        // Status column with badge
        competitionGrid.addColumn(new ComponentRenderer<>(summary -> {
            var statusBadge = new Span(summary.status().getLiteral());
            statusBadge.getElement().getThemeList().add("badge");

            switch (summary.status()) {
                case upcoming -> statusBadge.getElement().getThemeList().add("success");
                case live -> statusBadge.getElement().getThemeList().add("contrast");
                case finished -> statusBadge.getElement().getThemeList().add("error");
            }

            return statusBadge;
        })).setHeader("Status").setWidth("120px").setFlexGrow(0);

        // Predictions count column
        competitionGrid.addColumn(summary ->
                        String.format("%d / %d", summary.predictedEntries(), summary.totalEntries())
                )
                .setHeader("Predictions")
                .setWidth("120px")
                .setFlexGrow(0);

        // Completion percentage column with progress bar
        competitionGrid.addColumn(new ComponentRenderer<>(summary -> {
            var percentage = summary.getCompletionPercentage();
            var layout = new HorizontalLayout();
            layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            layout.setSpacing(true);

            var progressText = new Span(percentage + "%");
            progressText.getStyle().set("min-width", "50px");

            layout.add(progressText);

            // Add checkmark if complete
            if (summary.isComplete()) {
                var checkIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
                checkIcon.setColor("var(--lumo-success-color)");
                layout.add(checkIcon);
            }

            return layout;
        })).setHeader("Completion").setWidth("150px").setFlexGrow(0);

        // Editable indicator column
        competitionGrid.addColumn(new ComponentRenderer<>(summary -> {
            if (summary.isEditable()) {
                var editableIcon = new Icon(VaadinIcon.EDIT);
                editableIcon.setColor("var(--lumo-success-color)");
                editableIcon.setTooltipText("Editable");
                return editableIcon;
            } else {
                var lockedIcon = new Icon(VaadinIcon.LOCK);
                lockedIcon.setColor("var(--lumo-error-color)");
                lockedIcon.setTooltipText("Locked - deadline passed");
                return lockedIcon;
            }
        })).setHeader("").setWidth("60px").setFlexGrow(0);

        // Actions column
        competitionGrid.addColumn(new ComponentRenderer<>(summary -> {
            var editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            editButton.setEnabled(summary.isEditable());

            editButton.addClickListener(event -> navigateToPredictionView(summary.competitionId()));

            // View button for locked competitions
            if (!summary.isEditable()) {
                var viewButton = new Button("View", new Icon(VaadinIcon.EYE));
                viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
                viewButton.addClickListener(event -> navigateToPredictionView(summary.competitionId()));
                return viewButton;
            }

            return editButton;
        })).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        return competitionGrid;
    }

    /**
     * Loads competitions with user's predictions.
     */
    private void loadCompetitions() {
        try {
            var competitions = predictionRepository.getCompetitionsWithPredictions(currentUserId);
            grid.setItems(competitions);

            if (competitions.isEmpty()) {
                // Show message if no predictions
                var emptyState = new VerticalLayout();
                emptyState.setAlignItems(Alignment.CENTER);
                emptyState.setSpacing(true);

                var emptyIcon = new Icon(VaadinIcon.INFO_CIRCLE);
                emptyIcon.setSize("48px");
                emptyIcon.setColor("var(--lumo-secondary-text-color)");

                var emptyMessage = new H3("No predictions yet");
                emptyMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");

                var emptyDescription = new Paragraph(
                        "You haven't made any predictions yet. Visit the Make Predictions page to get started!"
                );
                emptyDescription.getStyle().set("color", "var(--lumo-secondary-text-color)");

                var makePredictionsButton = new Button("Make Predictions", new Icon(VaadinIcon.PLUS));
                makePredictionsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                makePredictionsButton.addClickListener(_ ->
                        UI.getCurrent().navigate(PredictionView.class));

                emptyState.add(emptyIcon, emptyMessage, emptyDescription, makePredictionsButton);
                add(emptyState);
            }

            log.debug("Loaded {} competitions with predictions for user {}", competitions.size(), currentUsername);
        } catch (Exception e) {
            log.error("Error loading competitions with predictions", e);
            var errorMessage = new Paragraph("Error loading your predictions. Please try again later.");
            errorMessage.getStyle().set("color", "var(--lumo-error-color)");
            add(errorMessage);
        }
    }

    /**
     * Navigates to the prediction view for editing.
     *
     * @param competitionId The competition ID
     */
    private void navigateToPredictionView(Long competitionId) {
        // Navigate to prediction view with competition ID as parameter
        // We'll use query parameters to pass the competition ID
        var queryParams = QueryParameters.simple(Map.of("competitionId", competitionId.toString()));
        UI.getCurrent().navigate(PredictionView.class, queryParams);
    }
}
