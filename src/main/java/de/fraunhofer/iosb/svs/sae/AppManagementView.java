package de.fraunhofer.iosb.svs.sae;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.fraunhofer.iosb.svs.sae.db.App;
import de.fraunhofer.iosb.svs.sae.db.AppRepository;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;
import de.fraunhofer.iosb.svs.sae.security.ReloadableX509TrustManager;

@Route(value = "apps")
@PageTitle("App Subscribtion Control")
public class AppManagementView extends Div {
    /**
     * 
     */
    private static final long serialVersionUID = 2142322324129176901L;

    private List<App> appsToShow = new ArrayList<App>();
    private Grid<App> grid = new Grid<>(App.class);

    @Autowired
    public AppManagementView(AppRepository appRepository) {
        Tab tabPending = new Tab("Pending Apps");
        Tab tabAllowed = new Tab("Allowed Apps");

        Map<Tab, Boolean> tabSelectorDecision = new HashMap<>();
        tabSelectorDecision.put(tabPending, true);
        tabSelectorDecision.put(tabAllowed, false);

        Tabs tabs = new Tabs(tabPending, tabAllowed);

        grid.setItems(appsToShow);

        grid.addComponentColumn(item -> {
            Button btn1 = createAllowButton(grid, item, appRepository);
            Button btn2 = createDeclineButton(grid, item, appRepository);

            HorizontalLayout buttonsGroup = new HorizontalLayout(btn1, btn2);
            buttonsGroup.setSpacing(true);

            return buttonsGroup;
        }).setHeader("Actions").setKey("pendingActions");

        grid.addComponentColumn(item -> {
            Button btn1 = createDisallowButton(grid, item, appRepository);
            Button btn2 = createRemoveButton(grid, item, appRepository);
            Button btn3 = createTokenButton(item);

            HorizontalLayout buttonsGroup = new HorizontalLayout(btn3, btn1, btn2);
            buttonsGroup.setSpacing(true);

            return buttonsGroup;
        }).setHeader("Actions").setKey("allowedActions");

        tabs.addSelectedChangeListener(event -> {
            Boolean isAppPendingin = tabSelectorDecision.get(tabs.getSelectedTab());
            refreshData(appRepository, isAppPendingin);
        });

        grid.removeColumnByKey("certificate");
        grid.getColumnByKey("allowedActions").setVisible(true);
        grid.getColumnByKey("pendingActions").setVisible(false);
        refreshData(appRepository, true);

        add(tabs, grid);
    }

    private void refreshData(AppRepository appRepository, Boolean isAppPendingin) {
        appsToShow.clear();

        appRepository.findAll().forEach((App app) -> {
            if (app.isPending() == isAppPendingin) {
                appsToShow.add(app);
            }
        });

        if (isAppPendingin) {
            grid.getColumnByKey("allowedActions").setVisible(false);
            grid.getColumnByKey("pendingActions").setVisible(true);
        } else {
            grid.getColumnByKey("allowedActions").setVisible(true);
            grid.getColumnByKey("pendingActions").setVisible(false);
        }

        @SuppressWarnings("unchecked")
        ListDataProvider<App> dataProvider = (ListDataProvider<App>) grid.getDataProvider();
        dataProvider.refreshAll();
    }

    @SuppressWarnings("unchecked")
    private Button createAllowButton(Grid<App> grid, App app, AppRepository appRepository) {
        Button button = new Button("Allow", clickEvent -> {
            ListDataProvider<App> dataProvider = (ListDataProvider<App>) grid.getDataProvider();
            Notification notification = null;

            try {
                ReloadableX509TrustManager.getInstance().addServerCertAndReload(app.getCertificate(), app.getKey());
                app.setPending(false);
                appRepository.save(app);
                dataProvider.getItems().remove(app);
                notification = new Notification("App can now request operations to the engine", 3000,
                        Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                notification = new Notification("There was problem with the app certificate", 3000,
                        Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            notification.open();
            dataProvider.refreshAll();
        });

        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    private Button createTokenButton(App app) {
        Button button = new Button("Generate Token", clickEvent -> {
            Notification notification = null;

            try {
                // Generate JWT
                List<Pair<String, String>> claims = new ArrayList<Pair<String, String>>();
                claims.add(new Pair<String, String>("name", app.getKey()));
                String token = JWTMisc.signAndGenerate(claims);

                UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", token);

                notification = new Notification("Copied to clipboard", 3000, Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                notification = new Notification("Token generation failed", 3000, Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            notification.open();

        });

        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    @SuppressWarnings("unchecked")
    private Button createDeclineButton(Grid<App> grid, App app, AppRepository appRepository) {
        Button button = new Button("Reject", clickEvent -> {

            Dialog dialog = new Dialog();

            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);

            Span message = new Span("Are you sure you want to reject the app named " + app.getKey() + "?");

            Button confirmButton = new Button("Yes", event -> {
                appRepository.delete(app);
                ListDataProvider<App> dataProvider = (ListDataProvider<App>) grid.getDataProvider();
                dataProvider.getItems().remove(app);
                dataProvider.refreshAll();
                Notification notification = new Notification("App was rejected", 3000, Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                notification.open();
                dialog.close();
            });
            Button cancelButton = new Button("Cancel", event -> {
                dialog.close();
            });

            dialog.add(new HorizontalLayout(message));
            dialog.add(new HorizontalLayout(confirmButton, cancelButton));
            dialog.open();
        });
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        return button;

    }

    @SuppressWarnings("unchecked")
    private Button createRemoveButton(Grid<App> grid, App app, AppRepository appRepository) {
        Button button = new Button("Reject", clickEvent -> {
            Dialog dialog = new Dialog();

            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);

            Span message = new Span("Are you sure you want to remove the app named " + app.getKey() + "?");

            Button confirmButton = new Button("Yes", event -> {

                try {
                    ReloadableX509TrustManager.getInstance().addServerCertAndReload(app.getCertificate(), app.getKey());
                } catch (Exception e) {

                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                appRepository.delete(app);
                ListDataProvider<App> dataProvider = (ListDataProvider<App>) grid.getDataProvider();
                dataProvider.getItems().remove(app);
                dataProvider.refreshAll();

                Notification notification = new Notification("App was removed", 3000, Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                dialog.close();
            });
            Button cancelButton = new Button("Cancel", event -> {
                dialog.close();
            });

            dialog.add(new HorizontalLayout(message));
            dialog.add(new HorizontalLayout(confirmButton, cancelButton));
            dialog.open();

        });
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    @SuppressWarnings("unchecked")
    private Button createDisallowButton(Grid<App> grid, App app, AppRepository appRepository) {
        Button button = new Button("Disallow", clickEvent -> {
            Dialog dialog = new Dialog();

            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);

            Span message = new Span("Are you sure you want to deactivate the app named " + app.getKey() + "?");

            Button confirmButton = new Button("Yes", event -> {

                try {
                    ReloadableX509TrustManager.getInstance().deleteServerCertAndReload(app.getKey());
                    app.setPending(true);
                    appRepository.save(app);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                ListDataProvider<App> dataProvider = (ListDataProvider<App>) grid.getDataProvider();
                dataProvider.getItems().remove(app);
                dataProvider.refreshAll();

                Notification notification = new Notification("App was deactivated", 3000,
                        Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                notification.open();
                dialog.close();
            });
            Button cancelButton = new Button("Cancel", event -> {
                dialog.close();
            });

            dialog.add(new HorizontalLayout(message));
            dialog.add(new HorizontalLayout(confirmButton, cancelButton));
            dialog.open();

        });
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
        return button;
    }

}
