package com.vectrasync.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vectrasync.ui.views.HistoryView;
import com.vectrasync.ui.views.SettingsView;
import com.vectrasync.ui.views.SyncView;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("VectraSync");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span tag = new Span("agentic CRM sync");
        tag.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY,
                LumoUtility.Margin.Left.SMALL);

        HorizontalLayout header = new HorizontalLayout(toggle, title, tag);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void addDrawerContent() {
        Span appName = new Span("VectraSync");
        appName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE,
                LumoUtility.Padding.MEDIUM);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Sync", SyncView.class, VaadinIcon.CLOUD_UPLOAD_O.create()));
        nav.addItem(new SideNavItem("History", HistoryView.class, VaadinIcon.CLOCK.create()));
        nav.addItem(new SideNavItem("Settings", SettingsView.class, VaadinIcon.COG.create()));

        VerticalLayout drawer = new VerticalLayout(appName, nav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        addToDrawer(drawer);
    }
}
