package ch.martinelli.fun.kututipp.view;

import ch.martinelli.fun.kututipp.dto.LeaderboardEntry;
import ch.martinelli.fun.kututipp.dto.RankTrend;
import ch.martinelli.fun.kututipp.service.LeaderboardService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Leaderboard view showing user rankings and statistics.
 * Implements UC-015: View Leaderboard.
 * <p>
 * Features:
 * - Overall rankings across all competitions
 * - Filtering by competition, apparatus, and gender
 * - Highlighting of current user
 * - Real-time refresh capability
 */
@PermitAll
@Route("leaderboard")
@PageTitle("Leaderboard - Kutu-Tipp")
public class LeaderboardView extends VerticalLayout {

    private static final String HIGHLIGHTED_ROW_CLASS = "highlighted-row";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LeaderboardService leaderboardService;
    private final Grid<LeaderboardEntry> grid;
    private final Span lastUpdatedLabel;
    private String currentUsername;

    // Filter components
    private ComboBox<String> filterTypeCombo;

    public LeaderboardView(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;

        // Get current username
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            this.currentUsername = authentication.getName();
        }

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        var title = new H1("Leaderboard");
        add(title);

        // Description
        var description = new Paragraph(
                "View rankings based on prediction accuracy. Top performers earn the most points!"
        );
        add(description);

        // Filter bar
        var filterBar = createFilterBar();
        add(filterBar);

        // Action bar with refresh button and last updated label
        var actionBar = createActionBar();
        add(actionBar);

        // Create and configure grid
        this.grid = createGrid();
        add(grid);

        // Last updated timestamp
        lastUpdatedLabel = new Span();
        lastUpdatedLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
        lastUpdatedLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(lastUpdatedLabel);

        // Load initial data
        refreshLeaderboard();
    }

    /**
     * Creates the filter bar with filter options.
     */
    private HorizontalLayout createFilterBar() {
        var filterBar = new HorizontalLayout();
        filterBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        filterBar.setSpacing(true);

        // Filter type selector
        filterTypeCombo = new ComboBox<>("Filter");
        filterTypeCombo.setItems("Overall", "By Competition", "By Apparatus", "By Gender");
        filterTypeCombo.setValue("Overall");
        filterTypeCombo.setWidth("200px");
        filterTypeCombo.addValueChangeListener(event -> {
            // TODO: Implement filtering when competition/apparatus data is available
            refreshLeaderboard();
        });

        var resetButton = new Button("Reset Filters", event -> {
            filterTypeCombo.setValue("Overall");
            refreshLeaderboard();
        });
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        filterBar.add(filterTypeCombo, resetButton);
        return filterBar;
    }

    /**
     * Creates the action bar with refresh button.
     */
    private HorizontalLayout createActionBar() {
        var actionBar = new HorizontalLayout();
        actionBar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        actionBar.setSpacing(true);

        var refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH), event -> refreshLeaderboard());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        // TODO: Future enhancements
        // - Export CSV button
        // - Top 10 toggle checkbox

        actionBar.add(refreshButton);
        return actionBar;
    }

    /**
     * Creates and configures the leaderboard grid.
     */
    private Grid<LeaderboardEntry> createGrid() {
        var leaderboardGrid = new Grid<LeaderboardEntry>();
        leaderboardGrid.setHeight("600px");

        // Rank column with special rendering for top 3
        leaderboardGrid.addColumn(new ComponentRenderer<>(entry -> {
            var rankSpan = new Span();
            var rank = entry.rank();

            // Add medal icons for top 3
            if (rank == 1) {
                var medal = new Icon(VaadinIcon.TROPHY);
                medal.setColor("gold");
                rankSpan.add(medal, new Span(" " + rank));
            } else if (rank == 2) {
                var medal = new Icon(VaadinIcon.MEDAL);
                medal.setColor("silver");
                rankSpan.add(medal, new Span(" " + rank));
            } else if (rank == 3) {
                var medal = new Icon(VaadinIcon.MEDAL);
                medal.setColor("#CD7F32"); // Bronze color
                rankSpan.add(medal, new Span(" " + rank));
            } else {
                rankSpan.setText(String.valueOf(rank));
            }

            return rankSpan;
        })).setHeader("Rank").setWidth("100px").setFlexGrow(0);

        // Username column
        leaderboardGrid.addColumn(LeaderboardEntry::username)
                .setHeader("Username")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Total points column
        leaderboardGrid.addColumn(LeaderboardEntry::totalPoints)
                .setHeader("Total Points")
                .setWidth("120px")
                .setFlexGrow(0);

        // Predictions made column
        leaderboardGrid.addColumn(LeaderboardEntry::totalPredictions)
                .setHeader("Predictions")
                .setWidth("120px")
                .setFlexGrow(0);

        // Exact predictions column (3 points)
        leaderboardGrid.addColumn(LeaderboardEntry::exactPredictions)
                .setHeader("Exact (3pts)")
                .setWidth("120px")
                .setFlexGrow(0);

        // Average points column
        leaderboardGrid.addColumn(entry -> String.format("%.2f", entry.avgPoints()))
                .setHeader("Avg Points")
                .setWidth("120px")
                .setFlexGrow(0);

        // Trend column with icons
        leaderboardGrid.addColumn(new ComponentRenderer<>(entry -> {
            var trend = entry.trend();
            var icon = switch (trend) {
                case UP -> new Icon(VaadinIcon.ARROW_UP);
                case DOWN -> new Icon(VaadinIcon.ARROW_DOWN);
                case STABLE -> new Icon(VaadinIcon.ARROW_RIGHT);
                case NEW -> new Icon(VaadinIcon.STAR);
            };

            // Color coding
            if (trend == RankTrend.UP) {
                icon.setColor("green");
            } else if (trend == RankTrend.DOWN) {
                icon.setColor("red");
            } else if (trend == RankTrend.NEW) {
                icon.setColor("blue");
            }

            return icon;
        })).setHeader("Trend").setWidth("80px").setFlexGrow(0);

        // Highlight current user's row
        leaderboardGrid.setClassNameGenerator(entry -> {
            if (entry.isCurrentUser()) {
                return HIGHLIGHTED_ROW_CLASS;
            }
            return null;
        });

        return leaderboardGrid;
    }

    /**
     * Refreshes the leaderboard data from the service.
     */
    private void refreshLeaderboard() {
        List<LeaderboardEntry> entries;

        // Get filter selection
        var filterType = filterTypeCombo.getValue();

        if ("Overall".equals(filterType) || filterType == null) {
            entries = leaderboardService.getOverallLeaderboard(currentUsername);
        } else {
            // For now, just show overall leaderboard
            // TODO: Implement specific filtering when competition/apparatus data is available
            entries = leaderboardService.getOverallLeaderboard(currentUsername);
        }

        grid.setItems(entries);

        // Update timestamp
        var now = OffsetDateTime.now();
        lastUpdatedLabel.setText("Last updated: " + now.format(TIME_FORMATTER));

        // If current user is in the list, scroll to their position
        entries.stream()
                .filter(LeaderboardEntry::isCurrentUser)
                .findFirst()
                .ifPresent(entry -> {
                    grid.select(entry);
                    grid.scrollToIndex(entries.indexOf(entry));
                });
    }
}
