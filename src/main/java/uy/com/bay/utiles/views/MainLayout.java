package uy.com.bay.utiles.views;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;

import uy.com.bay.utiles.data.User;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.expenses.ReportesDialog;

/**
 * The main view is a top-level placeholder for other views.
 */

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

	private H1 viewTitle;

	private AuthenticatedUser authenticatedUser;
	private AccessAnnotationChecker accessChecker;
	private final SurveyorService surveyorService;
	private final StudyService studyService;
	private final JournalEntryService journalEntryService;

	public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker,
			SurveyorService surveyorService, StudyService studyService, JournalEntryService journalEntryService) {
		this.authenticatedUser = authenticatedUser;
		this.accessChecker = accessChecker;
		this.surveyorService = surveyorService;
		this.studyService = studyService;
		this.journalEntryService = journalEntryService;

		setPrimarySection(Section.DRAWER);
		addDrawerContent();
		addHeaderContent();
	}

	private void addHeaderContent() {
		DrawerToggle toggle = new DrawerToggle();
		toggle.setAriaLabel("Menu toggle");

		viewTitle = new H1();
		viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

		addToNavbar(true, toggle, viewTitle);
	}

	private void addDrawerContent() {
		Span appName = new Span("Útiles");
		appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
		Header header = new Header(appName);

		Scroller scroller = new Scroller(createNavigation());

		addToDrawer(header, scroller, createFooter());
	}

	private SideNav createNavigation() {
		SideNav nav = new SideNav();

		SideNavItem proyectosItem = new SideNavItem("Proyectos");
		proyectosItem.setPrefixComponent(new Icon("vaadin", "briefcase"));
		SideNavItem listarProeyctosItem = new SideNavItem("Listar proyectos", "studies");
		listarProeyctosItem.setPrefixComponent(new Icon("vaadin", "list"));
		proyectosItem.addItem(listarProeyctosItem);
		SideNavItem listarSolicitudesItem = new SideNavItem("Solicitudes de campo", "fieldworks");
		listarSolicitudesItem.setPrefixComponent(new Icon("vaadin", "list"));
		proyectosItem.addItem(listarSolicitudesItem);
		SideNavItem ganttItem = new SideNavItem("Gantt", "gantt");
		ganttItem.setPrefixComponent(new Icon("vaadin", "chart-timeline"));
		proyectosItem.addItem(ganttItem);
		nav.addItem(proyectosItem);

		List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
		menuEntries.forEach(entry -> {
			if (!entry.title().equals("Usuarios") && !entry.title().equals("Proyectos")) {
				if (entry.icon() != null) {
					nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
				} else {
					nav.addItem(new SideNavItem(entry.title(), entry.path()));
				}
			}
		});
		SideNavItem answersItem = new SideNavItem("Respuestas Alchemer", "answers");
		answersItem.setPrefixComponent(new Icon("vaadin", "comment-ellipsis-o"));
		nav.addItem(answersItem);

		SideNavItem gastosItem = new SideNavItem("Gastos");
		gastosItem.setPrefixComponent(new Icon("vaadin", "money"));
		SideNavItem conceptosItem = new SideNavItem("Conceptos", "conceptos");
		conceptosItem.setPrefixComponent(new Icon("vaadin", "book-dollar"));
		gastosItem.addItem(conceptosItem);
		SideNavItem solicitudesItem = new SideNavItem("Solicitudes");
		solicitudesItem.setPrefixComponent(new Icon("vaadin", "cash"));
		SideNavItem verSolicitudesItem = new SideNavItem("Ver Solicitudes", "expenses");
		verSolicitudesItem.setPrefixComponent(new Icon("vaadin", "list"));
		solicitudesItem.addItem(verSolicitudesItem);
		SideNavItem aprobarSolicitudesItem = new SideNavItem("Aprobar solicitudes", "expenses-approval");
		aprobarSolicitudesItem.setPrefixComponent(new Icon("vaadin", "check-square-o"));
		solicitudesItem.addItem(aprobarSolicitudesItem);

		SideNavItem transferirSolicitudesItem = new SideNavItem("Transferir Solicitudes", "expense-transfer");
		transferirSolicitudesItem.setPrefixComponent(new Icon("vaadin", "exchange"));
		solicitudesItem.addItem(transferirSolicitudesItem);
		gastosItem.addItem(solicitudesItem);
		nav.addItem(gastosItem);

		SideNavItem rendicionesItem = new SideNavItem("Rendiciones");
		rendicionesItem.setPrefixComponent(new Icon("vaadin", "file-text-o"));

		SideNavItem verRendicionesItems = new SideNavItem("Ver Rendiciones", "expense-reports");
		verRendicionesItems.setPrefixComponent(new Icon("vaadin", "list"));
		rendicionesItem.addItem(verRendicionesItems);

		SideNavItem aprobarRendicionesItem = new SideNavItem("Aprobar rendiciones", "expense-reports-approval");
		aprobarRendicionesItem.setPrefixComponent(new Icon("vaadin", "check-square-o"));
		rendicionesItem.addItem(aprobarRendicionesItem);

		gastosItem.addItem(rendicionesItem);

		SideNavItem reportesNavItem = new SideNavItem("Reportes");
		reportesNavItem.setPrefixComponent(new Icon("vaadin", "file-chart"));
		reportesNavItem.getElement().addEventListener("click", e -> {
			ReportesDialog dialog = new ReportesDialog(surveyorService, studyService, journalEntryService);
			dialog.open();
		});
		gastosItem.addItem(reportesNavItem);

		SideNavItem surveyorPortalItem = new SideNavItem("Portal Encuestador");
		surveyorPortalItem.setPrefixComponent(new Icon("vaadin", "user-card"));

		SideNavItem saldoGastoItem = new SideNavItem("Saldo de gastos", "surveyor-journal-entry");
		saldoGastoItem.setPrefixComponent(new Icon("vaadin", "wallet"));
		surveyorPortalItem.addItem(saldoGastoItem);

		SideNavItem solicitarGastoItem = new SideNavItem("Solicitud de gastos", "surveyor-expense-request");
		solicitarGastoItem.setPrefixComponent(new Icon("vaadin", "cash"));
		surveyorPortalItem.addItem(solicitarGastoItem);

		SideNavItem rendirGastoItem = new SideNavItem("Rendir gasto", "surveyor-expense-report");
		rendirGastoItem.setPrefixComponent(new Icon("vaadin", "file-add"));
		surveyorPortalItem.addItem(rendirGastoItem);

		nav.addItem(surveyorPortalItem);

		SideNavItem settingsItem = new SideNavItem("Configuración");
		settingsItem.setPrefixComponent(new Icon("vaadin", "cog"));
		SideNavItem usersItem = new SideNavItem("Usuarios", "useradmin");
		usersItem.setPrefixComponent(new Icon("vaadin", "users"));
		settingsItem.addItem(usersItem);
		
		
		
		SideNavItem areasItem = new SideNavItem("Areas", "areas");
		areasItem.setPrefixComponent(new Icon("vaadin", "list"));
		settingsItem.addItem(areasItem);

		SideNavItem tasksItem = new SideNavItem("Tareas", "tasks");
		tasksItem.setPrefixComponent(new Icon("vaadin", "tasks"));
		settingsItem.addItem(tasksItem);

		nav.addItem(settingsItem);

		return nav;
	}

	private Footer createFooter() {
		Footer layout = new Footer();

		Optional<User> maybeUser = authenticatedUser.get();
		if (maybeUser.isPresent()) {
			User user = maybeUser.get();

			Avatar avatar = new Avatar(user.getName());
			byte[] profilePictureData = user.getProfilePicture();
			if (profilePictureData != null) {
				StreamResource resource = new StreamResource("profile-pic",
						() -> new ByteArrayInputStream(profilePictureData));
				avatar.setImageResource(resource);
			}
			avatar.setThemeName("xsmall");
			avatar.getElement().setAttribute("tabindex", "-1");

			MenuBar userMenu = new MenuBar();
			userMenu.setThemeName("tertiary-inline contrast");

			MenuItem userName = userMenu.addItem("");
			Div div = new Div();
			div.add(avatar);
			div.add(user.getName());
			div.add(new Icon("lumo", "dropdown"));
			div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
			userName.add(div);
			userName.getSubMenu().addItem("Sign out", e -> {
				authenticatedUser.logout();
			});

			layout.add(userMenu);
		} else {
			Anchor loginLink = new Anchor("login", "Sign in");
			layout.add(loginLink);
		}

		return layout;
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		viewTitle.setText(getCurrentPageTitle());
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		attachEvent.getUI().setLocale(new Locale("es", "UY"));

	}

	private String getCurrentPageTitle() {
		return MenuConfiguration.getPageHeader(getContent()).orElse("");
	}
}
