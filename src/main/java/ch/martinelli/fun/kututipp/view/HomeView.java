package ch.martinelli.fun.kututipp.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Home/landing page for the Kutu-Tipp application.
 * Shows different content based on authentication status and user role.
 */
@AnonymousAllowed
@Route("")
@PageTitle("Home - Kutu-Tipp")
public class HomeView extends VerticalLayout {

    private static final String MAX_WIDTH = "600px";

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            // User is logged in
            showAuthenticatedView(authentication);
        } else {
            // User is not logged in
            showAnonymousView();
        }
    }

    private void showAuthenticatedView(Authentication authentication) {
        var username = authentication.getName();
        var isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        var title = new H1("Welcome to Kutu-Tipp, " + username + "!");

        var description = new Paragraph(
                "You are logged in and ready to participate in the Swiss Cup gymnastics prediction game."
        );

        var logoutButton = new Button("Logout", _ -> {
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        logoutButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var content = new VerticalLayout(title, description);

        if (isAdmin) {
            var adminSection = new H2("Administrator Functions");
            var adminInfo = new Paragraph(
                    "As an administrator, you have access to manage competitions, gymnasts, and apparatus."
            );
            content.add(adminSection, adminInfo);
        }

        content.add(logoutButton);
        content.setMaxWidth(MAX_WIDTH);
        content.setAlignItems(Alignment.CENTER);

        add(content);
    }

    private void showAnonymousView() {
        var title = new H1("Welcome to Kutu-Tipp!");

        var description = new Paragraph(
                "Kutu-Tipp is a prediction game for Swiss Cup gymnastics competitions. " +
                        "Predict scores for individual gymnasts and earn points based on your accuracy!"
        );
        description.getStyle().set("text-align", "center");
        description.setMaxWidth(MAX_WIDTH);

        var loginButton = new Button("Login", event ->
                getUI().ifPresent(ui -> ui.navigate(LoginView.class))
        );
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var registerButton = new Button("Register", event ->
                getUI().ifPresent(ui -> ui.navigate(RegistrationView.class))
        );
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        var content = new VerticalLayout(
                title,
                description,
                loginButton,
                registerButton
        );
        content.setMaxWidth(MAX_WIDTH);
        content.setAlignItems(Alignment.CENTER);
        content.setSpacing(true);

        add(content);
    }
}
