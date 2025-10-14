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
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Home/landing page for the Kutu-Tipp application.
 * Shows different content based on authentication status and user role.
 */
@Route("")
@PageTitle("Home - Kutu-Tipp")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        H1 title = new H1("Welcome to Kutu-Tipp, " + username + "!");

        Paragraph description = new Paragraph(
                "You are logged in and ready to participate in the Swiss Cup gymnastics prediction game."
        );

        Button logoutButton = new Button("Logout", event -> {
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        logoutButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout(title, description);

        if (isAdmin) {
            H2 adminSection = new H2("Administrator Functions");
            Paragraph adminInfo = new Paragraph(
                    "As an administrator, you have access to manage competitions, gymnasts, and apparatus."
            );
            content.add(adminSection, adminInfo);
        }

        content.add(logoutButton);
        content.setMaxWidth("600px");
        content.setAlignItems(Alignment.CENTER);

        add(content);
    }

    private void showAnonymousView() {
        H1 title = new H1("Welcome to Kutu-Tipp!");

        Paragraph description = new Paragraph(
                "Kutu-Tipp is a prediction game for Swiss Cup gymnastics competitions. " +
                "Predict scores for individual gymnasts and earn points based on your accuracy!"
        );
        description.getStyle().set("text-align", "center");
        description.setMaxWidth("600px");

        Button loginButton = new Button("Login", event ->
                getUI().ifPresent(ui -> ui.navigate(LoginView.class))
        );
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button registerButton = new Button("Register", event ->
                getUI().ifPresent(ui -> ui.navigate(RegistrationView.class))
        );
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        VerticalLayout content = new VerticalLayout(
                title,
                description,
                loginButton,
                registerButton
        );
        content.setMaxWidth("600px");
        content.setAlignItems(Alignment.CENTER);
        content.setSpacing(true);

        add(content);
    }
}
