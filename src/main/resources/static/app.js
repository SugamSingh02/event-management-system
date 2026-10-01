const authView = document.getElementById('auth-view');
const dashboardView = document.getElementById('dashboard-view');
const dashboardContent = document.getElementById('dashboard-content');
const loginForm = document.getElementById('login-form');
const signupForm = document.getElementById('signup-form');
const loginIdentifier = document.getElementById('login-identifier');
const loginPassword = document.getElementById('login-password');
const authMessage = document.getElementById('auth-message');
const showSignupBtn = document.getElementById('show-signup-btn');
const showLoginBtn = document.getElementById('show-login-btn');
const roleToggle = document.getElementById('role-toggle');
const rolePicker = document.querySelector('.role-picker');
const roleMenu = document.getElementById('role-menu');
const selectedRoleText = document.getElementById('selected-role-text');
const roleBadge = document.getElementById('role-badge');
const welcomeUser = document.getElementById('welcome-user');
const logoutButton = document.getElementById('logout-button');
const eventModal = document.getElementById('event-modal');
const eventModalContent = document.getElementById('event-modal-content');
const eventModalClose = document.getElementById('event-modal-close');
const eventEditModal = document.getElementById('event-edit-modal');
const eventEditModalClose = document.getElementById('event-edit-modal-close');
const eventEditForm = document.getElementById('event-edit-form');
const attendeesModal = document.getElementById('attendees-modal');
const attendeesModalContent = document.getElementById('attendees-modal-content');
const attendeesModalClose = document.getElementById('attendees-modal-close');
let editingEventId = null;

const state = {
    currentUser: null,
    events: [],
    users: [],
    registrations: []
};

let selectedRole = 'ADMIN';

const formatDate = (value) => {
    if (!value) {
        return 'N/A';
    }

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString();
};

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return '';
    }

    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function showAuthMessage(message, type = 'error') {
    authMessage.textContent = message;
    authMessage.classList.remove(
        'hidden',
        'error',
        'success'
    );
    authMessage.classList.add(type);
}

function hideAuthMessage() {
    authMessage.classList.add('hidden');
    authMessage.textContent = '';
}

function humanizeRole(role) {
    return role.charAt(0).toUpperCase()
        + role.slice(1).toLowerCase();
}

function getRolePlaceholder(role) {
    const normalizedRole =
        (role || selectedRole || 'ADMIN').toUpperCase();

    if (normalizedRole === 'ORGANIZER') {
        return 'organizer@event.com';
    }

    if (normalizedRole === 'ATTENDEE') {
        return 'attendee@event.com';
    }

    return 'admin@event.com';
}

function setSelectedRole(role) {
    selectedRole = role.toUpperCase();

    selectedRoleText.textContent =
        humanizeRole(selectedRole);

    loginIdentifier.placeholder =
        getRolePlaceholder(selectedRole);

    document
        .querySelectorAll('.role-option')
        .forEach(option => {

            option.classList.toggle(
                'active',
                option.dataset.role === selectedRole
            );
        });

    roleMenu.classList.add('hidden');

    roleToggle.setAttribute(
        'aria-expanded',
        'false'
    );
}

function showAuthView() {
    authView.classList.remove('hidden');
    dashboardView.classList.add('hidden');
}

function showDashboardView() {
    authView.classList.add('hidden');
    dashboardView.classList.remove('hidden');
}

async function getResponseMessage(response) {
    const text = await response.text();

    if (!text) {
        return `Request failed with status ${response.status}`;
    }

    try {
        const data = JSON.parse(text);

        if (data.message) {
            return data.message;
        }

        if (data.error) {
            return data.error;
        }

        return text;

    } catch {
        return text;
    }
}

/* ==================================================
   CSRF
================================================== */

async function getCsrfToken() {
    const response = await fetch('/csrf', {
        method: 'GET',
        credentials: 'same-origin'
    });

    if (!response.ok) {
        throw new Error(
            'Unable to obtain CSRF token.'
        );
    }

    const data = await response.json();

    if (!data.token) {
        throw new Error(
            'CSRF token was not provided by the server.'
        );
    }

    return data.token;
}

async function csrfFetch(url, options = {}) {
    const method =
        (options.method || 'GET').toUpperCase();

    const safeMethods = [
        'GET',
        'HEAD',
        'OPTIONS',
        'TRACE'
    ];

    if (safeMethods.includes(method)) {
        return fetch(url, {
            ...options,
            credentials: 'same-origin'
        });
    }

    const token = await getCsrfToken();

    const headers =
        new Headers(options.headers || {});

    headers.set(
        'X-XSRF-TOKEN',
        token
    );

    return fetch(url, {
        ...options,
        credentials: 'same-origin',
        headers
    });
}

/* ==================================================
   MARKUP
================================================== */

function statusPill(status) {
    const normalized = String(status || 'N/A').toUpperCase();
    const cls = normalized === 'CONFIRMED'
        ? 'confirmed'
        : normalized === 'CANCELLED'
            ? 'cancelled'
            : 'neutral';
    return `<span class="status-pill ${cls}">${escapeHtml(normalized)}</span>`;
}

function latestById(items, limit) {
    if (!Array.isArray(items)) {
        return [];
    }

    return [...items]
        .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
        .slice(0, limit);
}

function currentRole() {
    return state.currentUser?.role?.toUpperCase() || '';
}

function canManageEvent(event) {
    const role = currentRole();
    if (role === 'ADMIN') return true;
    return role === 'ORGANIZER'
        && Number(event?.organizerId) === Number(state.currentUser?.id);
}

function confirmedRegistrationsForEvent(eventId) {
    return state.registrations.filter(reg =>
        Number(reg.eventId) === Number(eventId)
        && String(reg.status || '').toUpperCase() === 'CONFIRMED'
    );
}

function generateEventMarkup(events, options = {}) {
    if (!Array.isArray(events) || events.length === 0) {
        return '<div class="empty-state">No events found.</div>';
    }

    const showRegister = options.showRegister === true;
    const showManage = options.showManage === true;
    const showRegistrationCount = options.showRegistrationCount === true;
    const showViewAttendees = options.showViewAttendees === true;
    const registeredEventIds = new Set(
        Array.isArray(options.registeredEventIds)
            ? options.registeredEventIds.map(Number)
            : []
    );

    return events.map(event => {
        const eventId = Number(event.id);
        const alreadyRegistered = registeredEventIds.has(eventId);
        const soldOut = Number(event.availableSeats) <= 0;
        const canManage = showManage && canManageEvent(event);
        const attendeeCount = confirmedRegistrationsForEvent(eventId).length;

        let actionMarkup = '';

        if (showRegister) {
            if (alreadyRegistered) {
                actionMarkup += '<button type="button" class="action-btn event-register-btn" disabled>Registered</button>';
            } else if (soldOut) {
                actionMarkup += '<button type="button" class="action-btn event-register-btn" disabled>Full</button>';
            } else {
                actionMarkup += `<button type="button" class="primary-btn event-register-btn" data-register-event-id="${eventId}">Register</button>`;
            }
        }

        if (canManage) {
            actionMarkup += `<button type="button" class="action-btn event-edit-btn" data-edit-event-id="${eventId}">Edit</button>`;

            if (currentRole() === 'ADMIN') {
                actionMarkup += `<button type="button" class="action-btn event-delete-btn" data-delete-event-id="${eventId}">Delete</button>`;
            }
        }

        const actionWrapper = actionMarkup
            ? `<div class="item-card-actions">${actionMarkup}</div>`
            : '';

        return `
        <article class="item-card event-card searchable-card" data-event-id="${eventId}" tabindex="0" role="button" aria-label="View details for ${escapeHtml(event.title)}">
            <div class="item-card-heading">
                <h4><span class="item-kicker">EVENT</span>${escapeHtml(event.title)}</h4>
                ${actionWrapper}
            </div>
            <div class="item-grid">
                <div class="meta"><strong>Event ID</strong><span>#${escapeHtml(event.id)}</span></div>
                <div class="meta"><strong>Location</strong><span>${escapeHtml(event.location)}</span></div>
                <div class="meta"><strong>Date</strong><span>${escapeHtml(formatDate(event.eventDate))}</span></div>
                <div class="meta"><strong>Capacity</strong><span>${escapeHtml(event.capacity)}</span></div>
                <div class="meta"><strong>Available seats</strong><span>${escapeHtml(event.availableSeats)}</span></div>
                ${showRegistrationCount ? `<div class="meta"><strong>Registered</strong><span>${attendeeCount}</span></div>` : ''}
            </div>
            <div class="event-card-description"><strong>Description</strong><span>${escapeHtml(event.description)}</span></div>
            <div class="event-card-hint">${canManage ? 'Click to view details · Edit to manage event' : 'Click to view event details'}</div>
        </article>
        `;
    }).join('');
}

function generateUserMarkup(users) {
    if (!Array.isArray(users) || users.length === 0) {
        return '<div class="empty-state">No users found.</div>';
    }

    return users.map(user => `
        <article class="item-card searchable-card">
            <h4><span class="item-kicker">USER</span>${escapeHtml(user.name)}</h4>
            <div class="item-grid">
                <div class="meta"><strong>Email</strong><span>${escapeHtml(user.email)}</span></div>
                <div class="meta"><strong>Phone</strong><span>${escapeHtml(user.phone)}</span></div>
                <div class="meta"><strong>User ID</strong><span>#${escapeHtml(user.id)}</span></div>
                <div class="meta"><strong>Role</strong><span>${escapeHtml(user.role)}</span></div>
            </div>
        </article>
    `).join('');
}

function generateRegistrationMarkup(registrations) {
    if (!Array.isArray(registrations) || registrations.length === 0) {
        return '<div class="empty-state">No registrations found.</div>';
    }

    const staffView = currentRole() === 'ADMIN' || currentRole() === 'ORGANIZER';

    return registrations.map(reg => {
        const registeredEvent = state.events.find(
            event => Number(event.id) === Number(reg.eventId)
        );
        const title = registeredEvent?.title || `Event #${reg.eventId}`;
        const location = registeredEvent?.location || 'N/A';
        const eventDate = registeredEvent?.eventDate || null;

        return `
        <article class="item-card registration-card searchable-card" data-event-id="${escapeHtml(reg.eventId)}" tabindex="0" role="button" aria-label="View details for ${escapeHtml(title)}">
            <div class="item-card-heading">
                <h4><span class="item-kicker">REG</span>${escapeHtml(title)}</h4>
                ${statusPill(reg.status)}
            </div>
            <div class="item-grid">
                <div class="meta"><strong>Registration ID</strong><span>#${escapeHtml(reg.id)}</span></div>
                <div class="meta"><strong>Event ID</strong><span>#${escapeHtml(reg.eventId)}</span></div>
                <div class="meta"><strong>Date</strong><span>${escapeHtml(formatDate(eventDate))}</span></div>
                <div class="meta"><strong>Location</strong><span>${escapeHtml(location)}</span></div>
                ${staffView ? `<div class="meta"><strong>Attendee</strong><span>${escapeHtml(reg.attendeeName || 'N/A')}</span></div>` : ''}
                ${staffView ? `<div class="meta"><strong>Email</strong><span>${escapeHtml(reg.attendeeEmail || 'N/A')}</span></div>` : ''}
                <div class="meta"><strong>Registered at</strong><span>${escapeHtml(formatDate(reg.registeredAt))}</span></div>
            </div>
            <div class="event-card-hint">Click to view event details</div>
        </article>
        `;
    }).join('');
}

function generateAttendanceOverviewMarkup(events, registrations) {
    if (!Array.isArray(events) || events.length === 0) {
        return '<div class="empty-state">No events available for attendance tracking.</div>';
    }

    return [...events]
        .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
        .map(event => {
            const confirmed = registrations.filter(reg =>
                Number(reg.eventId) === Number(event.id)
                && String(reg.status || '').toUpperCase() === 'CONFIRMED'
            );

            return `
                <article class="attendance-event-card searchable-card">
                    <div class="attendance-event-header">
                        <div>
                            <span class="item-kicker">EVENT #${escapeHtml(event.id)}</span>
                            <h4>${escapeHtml(event.title)}</h4>
                        </div>
                        <div class="attendance-event-actions">
                            <div class="attendance-count"><strong>${confirmed.length}</strong><span>registered</span></div>
                            <button type="button" class="action-btn event-attendees-btn" data-view-attendees-event-id="${Number(event.id)}">View attendees</button>
                        </div>
                    </div>
                </article>
            `;
        }).join('');
}

function dashboardHero(title, subtitle) {
    return `
        <section class="dashboard-hero section-anchor" id="overview-section">
            <div class="dashboard-hero-copy">
                <p class="kicker">LIVE EVENT INTELLIGENCE</p>
                <h1>${escapeHtml(title)} <span>in one workspace.</span></h1>
                <p>${escapeHtml(subtitle)}</p>
            </div>
        </section>
    `;
}

/* ==================================================
   ADMIN DASHBOARD
================================================== */

function buildAdminDashboard() {
    const managedEvents = latestById(state.events, 10);
    const confirmedTotal = state.registrations.filter(
        reg => String(reg.status || '').toUpperCase() === 'CONFIRMED'
    ).length;

    return `
        <div class="dashboard-grid">
            ${dashboardHero('Run every event', 'Monitor events, users, registrations, and attendance without leaving the operational view.')}

            <section class="summary-grid">
                <article class="summary-card"><span>Total Events</span><strong>${state.events.length}</strong><small>All events in workspace</small></article>
                <article class="summary-card"><span>Total Users</span><strong>${state.users.length}</strong><small>Registered platform users</small></article>
                <article class="summary-card"><span>Confirmed Registrations</span><strong>${confirmedTotal}</strong><small>Active attendee registrations</small></article>
            </section>

            <section class="dashboard-columns section-anchor" id="events-section">
                <section class="panel">
                    <div class="panel-header">
                        <div><h3>Create Event</h3><p class="panel-subtitle">Launch a new event with live seat tracking.</p></div>
                        <span class="panel-chip">ADMIN ACTION</span>
                    </div>
                    <form id="admin-event-form" class="management-form">
                        <div class="form-grid">
                            <label><span>Title</span><input type="text" name="title" placeholder="Conference 2026" required /></label>
                            <label><span>Location</span><input type="text" name="location" placeholder="Main auditorium" required /></label>
                        </div>
                        <label><span>Description</span><textarea name="description" rows="3" placeholder="What should attendees know?" required></textarea></label>
                        <div class="form-grid">
                            <label><span>Event Date</span>
                                <div class="datetime-picker-field">
                                    <input type="text" class="datetime-display" data-datetime-display placeholder="dd/mm/yyyy --:-- --" readonly required aria-label="Event date and time" />
                                    <input type="hidden" name="eventDate" data-datetime-value />
                                    <button type="button" class="datetime-trigger" data-open-datetime-picker aria-label="Choose event date and time">◷</button>
                                </div>
                            </label>
                            <label><span>Capacity</span><input type="number" name="capacity" min="1" value="50" required /></label>
                        </div>
                        <button type="submit" class="primary-btn">Create event ↗</button>
                    </form>
                </section>

                <section class="panel">
                    <div class="panel-header">
                        <div><h3>Event Management</h3><p class="panel-subtitle">Admins can edit any event from this workspace.</p></div>
                        <span class="panel-chip">${managedEvents.length} SHOWN</span>
                    </div>
                    <div class="card-list">${generateEventMarkup(managedEvents, { showManage: true, showRegistrationCount: true })}</div>
                </section>
            </section>

            <section class="panel section-anchor" id="users-section">
                <div class="panel-header">
                    <div><h3>Create User</h3><p class="panel-subtitle">Add an attendee, organizer, or administrator.</p></div>
                    <span class="panel-chip">USER ACCESS</span>
                </div>
                <form id="admin-user-form" class="management-form">
                    <div class="form-grid">
                        <label><span>Name</span><input type="text" name="name" required /></label>
                        <label><span>Email</span><input type="email" name="email" required /></label>
                    </div>
                    <div class="form-grid">
                        <label><span>Phone</span><input type="text" name="phone" required /></label>
                        <label><span>Password</span><input type="password" name="password" required /></label>
                    </div>
                    <label><span>Role</span>
                        <select name="role" required>
                            <option value="ATTENDEE">Attendee</option>
                            <option value="ORGANIZER">Organizer</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </label>
                    <button type="submit" class="primary-btn">Create user ↗</button>
                </form>
            </section>

            <section class="panel section-anchor" id="registrations-section">
                <div class="panel-header">
                    <div><h3>Registration & Attendance</h3><p class="panel-subtitle">See who is registered at each event, including attendee names and emails.</p></div>
                    <span class="panel-chip">${confirmedTotal} CONFIRMED</span>
                </div>
                <div class="attendance-overview">${generateAttendanceOverviewMarkup(state.events, state.registrations)}</div>
            </section>
        </div>
    `;
}

/* ==================================================
   ORGANIZER DASHBOARD
================================================== */

function buildOrganizerDashboard() {
    const myEvents = state.events.filter(
        event => Number(event.organizerId) === Number(state.currentUser?.id)
    );
    const recentMyEvents = latestById(myEvents, 8);
    const myEventIds = new Set(myEvents.map(event => Number(event.id)));
    const myRegistrations = state.registrations.filter(
        reg => myEventIds.has(Number(reg.eventId))
            && String(reg.status || '').toUpperCase() === 'CONFIRMED'
    );
    const confirmedTotal = myRegistrations.length;

    return `
        <div class="dashboard-grid">
            ${dashboardHero('Operate your events', 'Create events, edit your own events, and view registered students for each event from a dedicated attendee window.')}

            <section class="summary-grid">
                <article class="summary-card"><span>Your Events</span><strong>${myEvents.length}</strong><small>Events created by you</small></article>
                <article class="summary-card"><span>Confirmed Attendees</span><strong>${confirmedTotal}</strong><small>Active registrations across your events</small></article>
                <article class="summary-card"><span>Available Seats</span><strong>${myEvents.reduce((sum, event) => sum + Math.max(0, Number(event.availableSeats) || 0), 0)}</strong><small>Seats remaining across your events</small></article>
            </section>

            <section class="dashboard-columns section-anchor" id="events-section">
                <section class="panel">
                    <div class="panel-header">
                        <div><h3>Create Event</h3><p class="panel-subtitle">Publish a new event for attendees.</p></div>
                        <span class="panel-chip">ORGANIZER</span>
                    </div>
                    <form id="organizer-event-form" class="management-form">
                        <div class="form-grid">
                            <label><span>Title</span><input type="text" name="title" placeholder="Product meetup" required /></label>
                            <label><span>Location</span><input type="text" name="location" placeholder="Innovation hall" required /></label>
                        </div>
                        <label><span>Description</span><textarea name="description" rows="3" placeholder="Describe the session or event." required></textarea></label>
                        <div class="form-grid">
                            <label><span>Event Date</span>
                                <div class="datetime-picker-field">
                                    <input type="text" class="datetime-display" data-datetime-display placeholder="dd/mm/yyyy --:-- --" readonly required aria-label="Event date and time" />
                                    <input type="hidden" name="eventDate" data-datetime-value />
                                    <button type="button" class="datetime-trigger" data-open-datetime-picker aria-label="Choose event date and time">◷</button>
                                </div>
                            </label>
                            <label><span>Capacity</span><input type="number" name="capacity" min="1" value="40" required /></label>
                        </div>
                        <button type="submit" class="primary-btn">Create event ↗</button>
                    </form>
                </section>

                <section class="panel">
                    <div class="panel-header">
                        <div><h3>My Events</h3><p class="panel-subtitle">Edit your events or open attendee details in a separate glass window.</p></div>
                        <span class="panel-chip">${recentMyEvents.length} SHOWN</span>
                    </div>
                    <div class="card-list">${generateEventMarkup(recentMyEvents, { showManage: true, showRegistrationCount: true })}</div>
                </section>
            </section>

            <section class="panel section-anchor" id="registrations-section">
                <div class="panel-header">
                    <div><h3>Registration & Attendance</h3><p class="panel-subtitle">See how many students registered for each of your events; open an event to view their details.</p></div>
                    <span class="panel-chip">${confirmedTotal} CONFIRMED</span>
                </div>
                <div class="attendance-overview">${generateAttendanceOverviewMarkup(myEvents, state.registrations)}</div>
            </section>
        </div>
    `;
}

/* ==================================================
   ATTENDEE DASHBOARD
================================================== */

function buildAttendeeDashboard() {
    const myRegistrations = state.registrations.filter(
        reg => reg.userId === Number(state.currentUser.id)
    );
    const registeredEventIds = myRegistrations
        .filter(reg => String(reg.status).toUpperCase() === 'CONFIRMED')
        .map(reg => reg.eventId);

    const availableEvents = [...state.events]
        .filter(event => !registeredEventIds.includes(Number(event.id)))
        .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
        .slice(0, 8);

    return `
        <div class="dashboard-grid">
            ${dashboardHero('Find your next event', 'Explore the latest events, open any event for full details, and register without leaving the dashboard.')}

            <section class="summary-grid">
                <article class="summary-card summary-card-clickable" data-summary-target="events-section" tabindex="0" role="button">
                    <span>Available Events</span><strong>${availableEvents.length}</strong><small>Open events you can join</small>
                </article>
                <article class="summary-card summary-card-clickable" data-summary-target="registrations-section" tabindex="0" role="button">
                    <span>My Registered Events</span><strong>${myRegistrations.filter(reg => String(reg.status).toUpperCase() === 'CONFIRMED').length}</strong><small>Your confirmed registrations</small>
                </article>
                <article class="summary-card"><span>Available Seats</span><strong>${availableEvents.reduce((sum, event) => sum + Math.max(0, Number(event.availableSeats) || 0), 0)}</strong><small>Across available events</small></article>
            </section>

            <section class="panel section-anchor" id="events-section">
                <div class="panel-header">
                    <div><h3>Available Events</h3><p class="panel-subtitle">Newest events first. Click an event to open its details.</p></div>
                    <span class="panel-chip">${availableEvents.length} AVAILABLE</span>
                </div>
                <div class="card-list">${generateEventMarkup(availableEvents, { showRegister: true, registeredEventIds })}</div>
            </section>

            <section class="panel section-anchor" id="registrations-section">
                <div class="panel-header">
                    <div><h3>My Registered Events</h3><p class="panel-subtitle">Open a registered event to review its details.</p></div>
                    <span class="panel-chip">${myRegistrations.length} RECORDS</span>
                </div>
                <div class="card-list">${generateRegistrationMarkup(latestById(myRegistrations, 10))}</div>
            </section>
        </div>
    `;
}

/* ==================================================
   DATA
================================================== */

async function refreshData() {
    if (!state.currentUser) {
        state.events = [];
        state.users = [];
        state.registrations = [];

        return;
    }

    try {
        const role =
            state.currentUser.role.toUpperCase();

        const requests = [
            fetch(
                '/api/events',
                {
                    credentials: 'same-origin'
                }
            ),

            fetch(
                '/api/registrations',
                {
                    credentials: 'same-origin'
                }
            )
        ];

        if (role === 'ADMIN') {
            requests.push(
                fetch(
                    '/api/users',
                    {
                        credentials: 'same-origin'
                    }
                )
            );
        }

        const responses =
            await Promise.all(requests);

        const eventsRes = responses[0];
        const registrationsRes = responses[1];
        const usersRes = responses[2];

        if (!eventsRes.ok) {
            throw new Error(
                await getResponseMessage(eventsRes)
            );
        }

        if (!registrationsRes.ok) {

            if (
                registrationsRes.status === 401
                || registrationsRes.status === 403
            ) {

                state.currentUser = null;

                localStorage.removeItem(
                    'event-management-user'
                );

                showAuthView();
                toggleAuthMode('login');

                return;
            }

            throw new Error(
                await getResponseMessage(
                    registrationsRes
                )
            );
        }

        state.events =
            await eventsRes.json();

        state.registrations =
            await registrationsRes.json();

        if (role === 'ADMIN' && usersRes) {

            if (!usersRes.ok) {
                throw new Error(
                    await getResponseMessage(usersRes)
                );
            }

            state.users =
                await usersRes.json();

        } else {

            state.users = [];
        }

        renderDashboard();

    } catch (error) {

        console.error(
            'Failed to load dashboard data:',
            error
        );

        if (state.currentUser) {
            showDashboardView();
        }
    }
}

/* ==================================================
   DASHBOARD
================================================== */

function renderDashboard() {
    if (!state.currentUser) {
        showAuthView();
        return;
    }

    const role =
        state.currentUser.role.toUpperCase();

    welcomeUser.textContent =
        `${state.currentUser.name} (${role})`;

    roleBadge.textContent = role;

    const topbarContext = document.getElementById('topbar-context');
    if (topbarContext) topbarContext.textContent = 'OVERVIEW';

    showDashboardView();

    if (role === 'ADMIN') {

        dashboardContent.innerHTML =
            buildAdminDashboard();

    } else if (role === 'ORGANIZER') {

        dashboardContent.innerHTML =
            buildOrganizerDashboard();

    } else {

        dashboardContent.innerHTML =
            buildAttendeeDashboard();
    }

    bindDashboardChrome();
}

/* ==================================================
   AUTH MODE
================================================== */

function toggleAuthMode(mode) {
    const isSignup = mode === 'signup';

    loginForm.classList.toggle(
        'hidden',
        isSignup
    );

    signupForm.classList.toggle(
        'hidden',
        !isSignup
    );

    rolePicker.classList.toggle(
        'hidden',
        isSignup
    );

    showSignupBtn.classList.toggle(
        'hidden',
        isSignup
    );

    showLoginBtn.classList.toggle(
        'hidden',
        !isSignup
    );

    hideAuthMessage();

    const authHeading = document.getElementById('auth-heading');
    const authDescription = document.getElementById('auth-description');
    const authSwitchLabel = document.getElementById('auth-switch-label');
    if (authHeading) authHeading.textContent = isSignup ? 'Create your account' : 'Welcome back';
    if (authDescription) authDescription.textContent = isSignup
        ? 'Join the workspace as an attendee.'
        : 'Sign in to your event workspace.';
    if (authSwitchLabel) authSwitchLabel.textContent = isSignup ? 'Already registered?' : 'Need an account?';
}

/* ==================================================
   LOGIN
================================================== */

async function handleLogin(event) {
    event.preventDefault();

    const email =
        loginIdentifier.value.trim();

    const password =
        loginPassword.value.trim();

    if (!email || !password) {

        showAuthMessage(
            'Please enter your email and password.'
        );

        return;
    }

    try {

        const response =
            await csrfFetch(
                '/api/users/login',
                {
                    method: 'POST',

                    headers: {
                        'Content-Type':
                            'application/json'
                    },

                    body: JSON.stringify({
                        identifier: email,
                        password,
                        role: selectedRole
                    })
                }
            );

        if (!response.ok) {
            throw new Error(
                await getResponseMessage(response)
            );
        }

        state.currentUser =
            await response.json();

        localStorage.setItem(
            'event-management-user',
            JSON.stringify(state.currentUser)
        );

        /*
         * Authentication can invalidate the previous
         * CSRF token, so obtain a fresh one.
         */
        await getCsrfToken();

        loginForm.reset();

        hideAuthMessage();

        await refreshData();

    } catch (error) {

        showAuthMessage(
            error.message ||
            'Invalid login credentials.'
        );
    }
}

/* ==================================================
   SIGNUP
================================================== */

async function handleSignup(event) {
    event.preventDefault();

    const payload = {

        name:
            document
                .getElementById('signup-name')
                .value
                .trim(),

        email:
            document
                .getElementById('signup-email')
                .value
                .trim(),

        phone:
            document
                .getElementById('signup-phone')
                .value
                .trim(),

        password:
            document
                .getElementById('signup-password')
                .value
                .trim()
    };

    if (
        !payload.name
        || !payload.email
        || !payload.phone
        || !payload.password
    ) {

        showAuthMessage(
            'Please complete all account fields.'
        );

        return;
    }

    try {

        const response =
            await csrfFetch(
                '/api/users/register',
                {
                    method: 'POST',

                    headers: {
                        'Content-Type':
                            'application/json'
                    },

                    body: JSON.stringify(payload)
                }
            );

        if (!response.ok) {
            throw new Error(
                await getResponseMessage(response)
            );
        }

        signupForm.reset();

        toggleAuthMode('login');

        loginIdentifier.value =
            payload.email;

        showAuthMessage(
            'Account created successfully. Please log in.',
            'success'
        );

        setSelectedRole('ATTENDEE');

    } catch (error) {

        showAuthMessage(
            error.message ||
            'Unable to create account.'
        );
    }
}

/* ==================================================
   DASHBOARD FORMS
================================================== */

async function handleFormSubmit(event) {
    event.preventDefault();

    const form = event.target;

    try {

        const formData =
            new FormData(form);

        const payload =
            Object.fromEntries(
                formData.entries()
            );

        if (form.id.includes('event-form')) {

            payload.capacity =
                Number(payload.capacity);
        }

        if (form.id.includes('registration-form')) {

            payload.eventId =
                Number(payload.eventId);
        }

        const endpoint =
            form.id.includes('user-form')
                ? '/api/users'
                : form.id.includes('event-form')
                    ? '/api/events'
                    : '/api/registrations';

        const response =
            await csrfFetch(
                endpoint,
                {
                    method: 'POST',

                    headers: {
                        'Content-Type':
                            'application/json'
                    },

                    body:
                        JSON.stringify(payload)
                }
            );

        if (!response.ok) {
            throw new Error(
                await getResponseMessage(response)
            );
        }

        form.reset();

        await refreshData();

    } catch (error) {

        alert(
            `Operation failed: ${
                error.message ||
                'Unknown error'
            }`
        );
    }
}

/* ==================================================
   THEMED DATE / TIME PICKER
================================================== */

const dateTimePickerState = {
    activeField: null,
    value: null,
    viewYear: null,
    viewMonth: null
};

function pad2(value) {
    return String(value).padStart(2, '0');
}

function parseLocalDateTime(value) {
    if (!value) return null;
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (!match) return null;
    const [, year, month, day, hour, minute] = match.map(Number);
    const date = new Date(year, month - 1, day, hour, minute, 0, 0);
    return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateTimeLocal(value) {
    if (!value) return '';
    const date = typeof value === 'string' ? parseLocalDateTime(String(value).slice(0, 16)) : value;
    if (!date) return '';
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

function formatDateTimeDisplay(date) {
    if (!date || Number.isNaN(date.getTime())) return '';
    let hour = date.getHours() % 12;
    if (hour === 0) hour = 12;
    const period = date.getHours() >= 12 ? 'PM' : 'AM';
    return `${pad2(date.getDate())}/${pad2(date.getMonth() + 1)}/${date.getFullYear()} ${pad2(hour)}:${pad2(date.getMinutes())} ${period}`;
}

function getPickerValueDate() {
    return dateTimePickerState.value || new Date();
}

function isSameCalendarDay(a, b) {
    return Boolean(a && b
        && a.getFullYear() === b.getFullYear()
        && a.getMonth() === b.getMonth()
        && a.getDate() === b.getDate());
}

function isBeforeToday(date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const candidate = new Date(date);
    candidate.setHours(0, 0, 0, 0);
    return candidate < today;
}

function renderDateTimePicker() {
    const modal = document.getElementById('datetime-picker-modal');
    const calendar = document.getElementById('datetime-picker-calendar');
    const monthTitle = document.getElementById('datetime-picker-month');
    const hourList = document.getElementById('datetime-hour-list');
    const minuteList = document.getElementById('datetime-minute-list');
    const periodList = document.getElementById('datetime-period-list');
    const value = getPickerValueDate();

    if (!modal || !calendar || !monthTitle || !hourList || !minuteList || !periodList) return;

    const viewDate = new Date(dateTimePickerState.viewYear, dateTimePickerState.viewMonth, 1);
    monthTitle.textContent = `${viewDate.toLocaleString(undefined, { month: 'long' })} ${viewDate.getFullYear()}`;

    const weekdays = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];
    const firstDay = (viewDate.getDay() + 6) % 7;
    const daysInMonth = new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 0).getDate();
    const daysInPrevMonth = new Date(viewDate.getFullYear(), viewDate.getMonth(), 0).getDate();
    let calendarMarkup = weekdays.map(day => `<span class="datetime-weekday">${day}</span>`).join('');

    for (let index = 0; index < 42; index += 1) {
        let year = viewDate.getFullYear();
        let month = viewDate.getMonth();
        let day;
        let muted = false;

        if (index < firstDay) {
            day = daysInPrevMonth - firstDay + index + 1;
            month -= 1;
            muted = true;
        } else if (index >= firstDay + daysInMonth) {
            day = index - firstDay - daysInMonth + 1;
            month += 1;
            muted = true;
        } else {
            day = index - firstDay + 1;
        }

        const cellDate = new Date(year, month, day);
        const selected = isSameCalendarDay(cellDate, value);
        const disabled = isBeforeToday(cellDate);
        const classes = ['datetime-day', muted ? 'muted' : '', selected ? 'selected' : '', disabled ? 'disabled' : ''].filter(Boolean).join(' ');
        calendarMarkup += `<button type="button" class="${classes}" data-calendar-date="${formatDateTimeLocal(cellDate)}" ${disabled ? 'disabled' : ''}>${day}</button>`;
    }

    calendar.innerHTML = calendarMarkup;

    const hour12 = ((value.getHours() + 11) % 12) + 1;
    const minute = value.getMinutes();
    const period = value.getHours() >= 12 ? 'PM' : 'AM';

    hourList.innerHTML = Array.from({ length: 12 }, (_, i) => i + 1)
        .map(hour => `<button type="button" class="datetime-time-option ${hour === hour12 ? 'selected' : ''}" data-hour="${hour}">${pad2(hour)}</button>`).join('');
    minuteList.innerHTML = Array.from({ length: 60 }, (_, i) => i)
        .map(item => `<button type="button" class="datetime-time-option ${item === minute ? 'selected' : ''}" data-minute="${item}">${pad2(item)}</button>`).join('');
    periodList.innerHTML = ['AM', 'PM']
        .map(item => `<button type="button" class="datetime-time-option period-option ${item === period ? 'selected' : ''}" data-period="${item}">${item}</button>`).join('');

    modal.classList.remove('hidden');
    document.body.classList.add('modal-open');
}

function openDateTimePicker(field) {
    const hidden = field.querySelector('[data-datetime-value]');
    const existing = parseLocalDateTime(hidden?.value || '');
    const value = existing || new Date();

    if (!existing) {
        value.setSeconds(0, 0);
        value.setMinutes(Math.ceil(value.getMinutes() / 5) * 5);
        if (value.getMinutes() === 60) {
            value.setHours(value.getHours() + 1);
            value.setMinutes(0);
        }
    }

    dateTimePickerState.activeField = field;
    dateTimePickerState.value = value;
    dateTimePickerState.viewYear = value.getFullYear();
    dateTimePickerState.viewMonth = value.getMonth();
    renderDateTimePicker();
}

function closeDateTimePicker() {
    const modal = document.getElementById('datetime-picker-modal');
    if (modal) modal.classList.add('hidden');
    if (eventModal?.classList.contains('hidden') && attendeesModal?.classList.contains('hidden') && eventEditModal?.classList.contains('hidden')) {
        document.body.classList.remove('modal-open');
    }
    dateTimePickerState.activeField = null;
}

function setPickerTime({ hour, minute, period } = {}) {
    const value = new Date(getPickerValueDate());
    let hour12 = ((value.getHours() + 11) % 12) + 1;
    let minuteValue = value.getMinutes();
    let periodValue = value.getHours() >= 12 ? 'PM' : 'AM';

    if (hour !== undefined) hour12 = Number(hour);
    if (minute !== undefined) minuteValue = Number(minute);
    if (period !== undefined) periodValue = period;

    let hour24 = hour12 % 12;
    if (periodValue === 'PM') hour24 += 12;
    value.setHours(hour24, minuteValue, 0, 0);
    dateTimePickerState.value = value;
    renderDateTimePicker();
}

function applyDateTimePicker() {
    const field = dateTimePickerState.activeField;
    const value = dateTimePickerState.value;
    if (!field || !value) return;

    if (value <= new Date()) {
        alert('Please choose a future date and time.');
        return;
    }

    const hidden = field.querySelector('[data-datetime-value]');
    const display = field.querySelector('[data-datetime-display]');
    if (hidden) hidden.value = formatDateTimeLocal(value);
    if (display) display.value = formatDateTimeDisplay(value);
    closeDateTimePicker();
}

function clearDateTimePicker() {
    const field = dateTimePickerState.activeField;
    if (!field) return;
    const hidden = field.querySelector('[data-datetime-value]');
    const display = field.querySelector('[data-datetime-display]');
    if (hidden) hidden.value = '';
    if (display) display.value = '';
    closeDateTimePicker();
}

/* ==================================================
   EVENT EDITING
================================================== */

function openEditEventModal(eventId) {
    const targetEvent = state.events.find(
        event => Number(event.id) === Number(eventId)
    );

    if (!targetEvent || !eventEditModal || !eventEditForm || !canManageEvent(targetEvent)) {
        return;
    }

    editingEventId = Number(eventId);
    eventEditForm.elements.title.value = targetEvent.title || '';
    eventEditForm.elements.description.value = targetEvent.description || '';
    eventEditForm.elements.location.value = targetEvent.location || '';
    const editDateTimeValue = formatDateTimeLocal(targetEvent.eventDate);
    const editDateTimeHidden = eventEditForm.querySelector('[data-datetime-value]');
    const editDateTimeDisplay = eventEditForm.querySelector('[data-datetime-display]');
    if (editDateTimeHidden) editDateTimeHidden.value = editDateTimeValue;
    if (editDateTimeDisplay) editDateTimeDisplay.value = formatDateTimeDisplay(parseLocalDateTime(editDateTimeValue));
    eventEditForm.elements.capacity.value = targetEvent.capacity ?? '';

    const title = document.getElementById('event-edit-title');
    if (title) title.textContent = `Edit Event #${targetEvent.id}`;

    eventEditModal.classList.remove('hidden');
    document.body.classList.add('modal-open');
    eventEditForm.elements.title.focus();
}

function closeEditEventModal() {
    if (!eventEditModal) return;
    eventEditModal.classList.add('hidden');
    document.body.classList.remove('modal-open');
    editingEventId = null;
}

async function handleEditEventSubmit(event) {
    event.preventDefault();

    if (!editingEventId) return;

    const payload = Object.fromEntries(new FormData(eventEditForm).entries());
    payload.capacity = Number(payload.capacity);

    const submitButton = eventEditForm.querySelector('button[type="submit"]');
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = 'Saving…';
    }

    try {
        const response = await csrfFetch(
            `/api/events/${editingEventId}`,
            {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }
        );

        if (!response.ok) {
            throw new Error(await getResponseMessage(response));
        }

        const updatedEvent = await response.json();
        closeEditEventModal();
        await refreshData();
        openEventModal(Number(updatedEvent.id));
    } catch (error) {
        alert(`Unable to update event: ${error.message || 'Unknown error'}`);
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = 'Save changes ↗';
        }
    }
}

/* ==================================================
   EVENT DETAILS MODAL
================================================== */

function openEventModal(eventId) {
    const targetEvent = state.events.find(
        event => Number(event.id) === Number(eventId)
    );

    if (!targetEvent || !eventModal || !eventModalContent) {
        return;
    }

    const myRegistration = state.registrations.find(
        registration =>
            Number(registration.eventId) === Number(eventId)
            && Number(registration.userId) === Number(state.currentUser?.id)
    );

    const role = state.currentUser?.role?.toUpperCase() || '';
    const isAttendee = role === 'ATTENDEE';
    const isConfirmed = String(myRegistration?.status || '').toUpperCase() === 'CONFIRMED';
    const soldOut = Number(targetEvent.availableSeats) <= 0;

    let actionMarkup = '';
    if (isAttendee) {
        if (isConfirmed) {
            actionMarkup = `<div class="modal-registration-state">${statusPill('CONFIRMED')}<span>You are registered for this event.</span></div>`;
        } else if (soldOut) {
            actionMarkup = '<button type="button" class="action-btn modal-action-btn" disabled>Event Full</button>';
        } else {
            actionMarkup = `<button type="button" class="primary-btn modal-action-btn" data-modal-register-event-id="${Number(targetEvent.id)}">Register for Event ↗</button>`;
        }
    }

    eventModalContent.innerHTML = `
        <div class="event-modal-eyebrow">EVENT DETAILS</div>
        <div class="event-modal-header">
            <div>
                <h2 id="event-modal-title">${escapeHtml(targetEvent.title)}</h2>
                <p>${escapeHtml(targetEvent.description)}</p>
            </div>
            <span class="modal-event-id">#${escapeHtml(targetEvent.id)}</span>
        </div>
        <div class="event-modal-grid">
            <div class="event-detail-box"><span>DATE & TIME</span><strong>${escapeHtml(formatDate(targetEvent.eventDate))}</strong></div>
            <div class="event-detail-box"><span>LOCATION</span><strong>${escapeHtml(targetEvent.location)}</strong></div>
            <div class="event-detail-box"><span>CAPACITY</span><strong>${escapeHtml(targetEvent.capacity)}</strong></div>
            <div class="event-detail-box"><span>AVAILABLE SEATS</span><strong>${escapeHtml(targetEvent.availableSeats)}</strong></div>
        </div>
        ${actionMarkup}
    `;

    eventModal.classList.remove('hidden');
    document.body.classList.add('modal-open');
    eventModalClose?.focus();
}

function closeEventModal() {
    if (!eventModal) {
        return;
    }

    eventModal.classList.add('hidden');
    document.body.classList.remove('modal-open');
}

/* ==================================================
   ATTENDEE VIEW
================================================== */

function openAttendeesModal(eventId) {
    const targetEvent = state.events.find(event => Number(event.id) === Number(eventId));
    if (!targetEvent || !attendeesModal || !attendeesModalContent) return;

    const role = currentRole();
    if (role !== 'ADMIN' && role !== 'ORGANIZER') return;

    const confirmed = state.registrations
        .filter(reg => Number(reg.eventId) === Number(eventId))
        .filter(reg => String(reg.status || '').toUpperCase() === 'CONFIRMED')
        .sort((a, b) => String(a.attendeeName || '').localeCompare(String(b.attendeeName || '')));

    const rows = confirmed.length
        ? confirmed.map(reg => `
            <div class="attendee-modal-row">
                <div class="attendee-modal-name">
                    <span class="attendee-avatar">${escapeHtml((reg.attendeeName || '?').charAt(0).toUpperCase())}</span>
                    <strong>${escapeHtml(reg.attendeeName || 'Unknown attendee')}</strong>
                </div>
                <span class="attendee-modal-email">${escapeHtml(reg.attendeeEmail || 'No email available')}</span>
            </div>
        `).join('')
        : '<div class="attendance-modal-empty">No confirmed students have registered for this event yet.</div>';

    attendeesModalContent.innerHTML = `
        <div class="event-modal-eyebrow">ATTENDEE LIST</div>
        <div class="event-modal-header">
            <div>
                <h2 id="attendees-modal-title">${escapeHtml(targetEvent.title)}</h2>
                <p>${escapeHtml(formatDate(targetEvent.eventDate))} · ${escapeHtml(targetEvent.location)}</p>
            </div>
            <div class="attendees-modal-count"><strong>${confirmed.length}</strong><span>registered</span></div>
        </div>
        <div class="attendee-modal-table">
            <div class="attendee-modal-table-head"><span>STUDENT NAME</span><span>EMAIL</span></div>
            <div class="attendee-modal-rows">${rows}</div>
        </div>
    `;

    attendeesModal.classList.remove('hidden');
    document.body.classList.add('modal-open');
    attendeesModalClose?.focus();
}

function closeAttendeesModal() {
    if (!attendeesModal) return;
    attendeesModal.classList.add('hidden');
    if (eventModal?.classList.contains('hidden') && document.getElementById('datetime-picker-modal')?.classList.contains('hidden') && eventEditModal?.classList.contains('hidden')) {
        document.body.classList.remove('modal-open');
    }
}

/* ==================================================
   LOGOUT
================================================== */

async function handleLogout() {
    try {

        const response =
            await csrfFetch(
                '/logout',
                {
                    method: 'POST'
                }
            );

        if (!response.ok
                && response.status !== 302) {

            console.error(
                'Logout failed:',
                await getResponseMessage(response)
            );
        }

    } catch (error) {

        console.error(
            'Logout request failed:',
            error
        );

    } finally {

        state.currentUser = null;
        state.events = [];
        state.users = [];
        state.registrations = [];

        localStorage.removeItem(
            'event-management-user'
        );

        dashboardContent.innerHTML = '';

        showAuthView();

        toggleAuthMode('login');

        /*
         * Logout invalidates the CSRF token.
         * A fresh token will be obtained on the
         * next state-changing request.
         */
    }
}

function bindDashboardChrome() {
    const role =
        state.currentUser?.role?.toUpperCase() || '';

    const usersNavItem =
        document.getElementById('users-nav-item');

    if (usersNavItem) {
        usersNavItem.classList.toggle(
            'hidden',
            role !== 'ADMIN'
        );
    }

    document.querySelectorAll('.nav-item[data-scroll-target]').forEach(button => {
        button.onclick = () => {
            const target = document.getElementById(button.dataset.scrollTarget);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
                button.classList.add('active');
                const label = button.textContent.trim().toUpperCase();
                const topbarContext = document.getElementById('topbar-context');
                if (topbarContext) topbarContext.textContent = label;
            }
            const sidebar = document.getElementById('sidebar');
            if (sidebar) sidebar.classList.remove('open');
        };
    });

    const search = document.getElementById('dashboard-search');
    if (search) {
        search.oninput = () => {
            const query = search.value.trim().toLowerCase();
            document.querySelectorAll('.searchable-card').forEach(card => {
                card.classList.toggle('search-hidden', Boolean(query) && !card.textContent.toLowerCase().includes(query));
            });
        };
        search.onkeydown = event => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
                event.preventDefault();
                search.focus();
            }
        };
    }

    const sidebarToggle = document.getElementById('sidebar-toggle');
    const sidebar = document.getElementById('sidebar');
    if (sidebarToggle && sidebar) {
        sidebarToggle.onclick = () => sidebar.classList.toggle('open');
    }
}

/* ==================================================
   EVENT LISTENERS
================================================== */

document
    .querySelectorAll('.role-option')
    .forEach(button => {

        button.addEventListener(
            'click',
            () =>
                setSelectedRole(
                    button.dataset.role
                )
        );
    });

roleToggle.addEventListener(
    'click',
    () => {

        const expanded =
            roleToggle.getAttribute(
                'aria-expanded'
            ) === 'true';

        roleToggle.setAttribute(
            'aria-expanded',
            String(!expanded)
        );

        roleMenu.classList.toggle(
            'hidden',
            expanded
        );
    }
);

showSignupBtn.addEventListener(
    'click',
    () =>
        toggleAuthMode('signup')
);

showLoginBtn.addEventListener(
    'click',
    () =>
        toggleAuthMode('login')
);

loginForm.addEventListener(
    'submit',
    handleLogin
);

signupForm.addEventListener(
    'submit',
    handleSignup
);

logoutButton.addEventListener(
    'click',
    handleLogout
);

dashboardContent.addEventListener(
    'click',
    async event => {
        const attendeeButton = event.target.closest('[data-view-attendees-event-id]');
        if (attendeeButton && state.currentUser) {
            event.stopPropagation();
            openAttendeesModal(Number(attendeeButton.dataset.viewAttendeesEventId));
            return;
        }

        const deleteButton = event.target.closest('[data-delete-event-id]');
        if (deleteButton && state.currentUser) {
            event.stopPropagation();
            const eventId = Number(deleteButton.dataset.deleteEventId);
            const targetEvent = state.events.find(item => Number(item.id) === eventId);

            if (currentRole() !== 'ADMIN' || !targetEvent) {
                return;
            }

            const confirmed = window.confirm(
                `Delete \"${targetEvent.title}\" (Event #${eventId})?\n\nThis will also remove its registrations. This action cannot be undone.`
            );

            if (!confirmed) {
                return;
            }

            deleteButton.disabled = true;
            deleteButton.textContent = 'Deleting…';

            try {
                const response = await csrfFetch(
                    `/api/events/${eventId}`,
                    { method: 'DELETE' }
                );

                if (!response.ok) {
                    throw new Error(await getResponseMessage(response));
                }

                closeEventModal();
                closeAttendeesModal();
                await refreshData();
            } catch (error) {
                deleteButton.disabled = false;
                deleteButton.textContent = 'Delete';
                alert(`Unable to delete event: ${error.message || 'Unknown error'}`);
            }

            return;
        }

        const editButton = event.target.closest('[data-edit-event-id]');
        if (editButton && state.currentUser) {
            event.stopPropagation();
            openEditEventModal(Number(editButton.dataset.editEventId));
            return;
        }

        const registerButton = event.target.closest('[data-register-event-id], [data-modal-register-event-id]');

        if (registerButton && state.currentUser) {
            const eventId = Number(
                registerButton.dataset.registerEventId
                || registerButton.dataset.modalRegisterEventId
            );

            if (!Number.isInteger(eventId) || eventId < 1) {
                return;
            }

            registerButton.disabled = true;
            registerButton.textContent = 'Registering…';

            try {
                const response = await csrfFetch(
                    '/api/registrations',
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ eventId })
                    }
                );

                if (!response.ok) {
                    throw new Error(
                        await getResponseMessage(response)
                    );
                }

                closeEventModal();
                await refreshData();
                openEventModal(eventId);

            } catch (error) {
                registerButton.disabled = false;
                registerButton.textContent = 'Register for Event ↗';
                alert(
                    `Registration failed: ${error.message || 'Unknown error'}`
                );
            }

            return;
        }

        const eventCard = event.target.closest('.event-card');
        if (eventCard) {
            openEventModal(Number(eventCard.dataset.eventId));
            return;
        }

        const registrationCard = event.target.closest('.registration-card');
        if (registrationCard) {
            openEventModal(Number(registrationCard.dataset.eventId));
            return;
        }

        const summaryCard = event.target.closest('[data-summary-target]');
        if (summaryCard) {
            const target = document.getElementById(summaryCard.dataset.summaryTarget);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
    }
);

dashboardContent.addEventListener(
    'keydown',
    event => {
        if (event.key !== 'Enter' && event.key !== ' ') {
            return;
        }

        const card = event.target.closest('.event-card, .registration-card');
        if (card) {
            event.preventDefault();
            openEventModal(Number(card.dataset.eventId));
            return;
        }

        const summaryCard = event.target.closest('[data-summary-target]');
        if (summaryCard) {
            event.preventDefault();
            const target = document.getElementById(summaryCard.dataset.summaryTarget);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
    }
);

dashboardContent.addEventListener(
    'submit',
    handleFormSubmit
);

eventModalClose?.addEventListener('click', closeEventModal);
attendeesModalClose?.addEventListener('click', closeAttendeesModal);
attendeesModal?.addEventListener('click', event => {
    if (event.target.matches('[data-close-attendees-modal]')) {
        closeAttendeesModal();
    }
});
eventEditModalClose?.addEventListener('click', closeEditEventModal);
document.getElementById('event-edit-cancel')?.addEventListener('click', closeEditEventModal);
eventEditForm?.addEventListener('submit', handleEditEventSubmit);
eventEditModal?.addEventListener('click', event => {
    if (event.target.matches('[data-close-edit-event-modal]')) {
        closeEditEventModal();
    }
});

eventModal?.addEventListener('click', async event => {
    if (event.target.matches('[data-close-event-modal]')) {
        closeEventModal();
        return;
    }

    const registerButton = event.target.closest('[data-modal-register-event-id]');
    if (!registerButton || !state.currentUser) {
        return;
    }

    const eventId = Number(registerButton.dataset.modalRegisterEventId);
    if (!Number.isInteger(eventId) || eventId < 1) {
        return;
    }

    registerButton.disabled = true;
    registerButton.textContent = 'Registering…';

    try {
        const response = await csrfFetch(
            '/api/registrations',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ eventId })
            }
        );

        if (!response.ok) {
            throw new Error(await getResponseMessage(response));
        }

        await refreshData();
        openEventModal(eventId);
    } catch (error) {
        registerButton.disabled = false;
        registerButton.textContent = 'Register for Event ↗';
        alert(`Registration failed: ${error.message || 'Unknown error'}`);
    }
});

document.addEventListener('click', event => {
    const trigger = event.target.closest('[data-open-datetime-picker]');
    const display = event.target.closest('[data-datetime-display]');
    const field = trigger?.closest('.datetime-picker-field') || display?.closest('.datetime-picker-field');

    if (field) {
        openDateTimePicker(field);
        return;
    }

    const modal = document.getElementById('datetime-picker-modal');
    if (!modal || modal.classList.contains('hidden')) return;

    if (event.target.matches('[data-close-datetime-modal]')) {
        closeDateTimePicker();
        return;
    }

    if (event.target.matches('[data-datetime-prev]')) {
        dateTimePickerState.viewMonth -= 1;
        if (dateTimePickerState.viewMonth < 0) {
            dateTimePickerState.viewMonth = 11;
            dateTimePickerState.viewYear -= 1;
        }
        renderDateTimePicker();
        return;
    }

    if (event.target.matches('[data-datetime-next]')) {
        dateTimePickerState.viewMonth += 1;
        if (dateTimePickerState.viewMonth > 11) {
            dateTimePickerState.viewMonth = 0;
            dateTimePickerState.viewYear += 1;
        }
        renderDateTimePicker();
        return;
    }

    const calendarDate = event.target.closest('[data-calendar-date]');
    if (calendarDate && !calendarDate.disabled) {
        const selected = parseLocalDateTime(calendarDate.dataset.calendarDate);
        if (selected) {
            const current = getPickerValueDate();
            selected.setHours(current.getHours(), current.getMinutes(), 0, 0);
            dateTimePickerState.value = selected;
            renderDateTimePicker();
        }
        return;
    }

    const hourOption = event.target.closest('[data-hour]');
    if (hourOption) {
        setPickerTime({ hour: hourOption.dataset.hour });
        return;
    }

    const minuteOption = event.target.closest('[data-minute]');
    if (minuteOption) {
        setPickerTime({ minute: minuteOption.dataset.minute });
        return;
    }

    const periodOption = event.target.closest('[data-period]');
    if (periodOption) {
        setPickerTime({ period: periodOption.dataset.period });
        return;
    }

    if (event.target.matches('[data-datetime-clear]')) {
        clearDateTimePicker();
        return;
    }

    if (event.target.matches('[data-datetime-today]')) {
        const now = new Date();
        now.setSeconds(0, 0);
        now.setMinutes(Math.ceil(now.getMinutes() / 5) * 5);
        if (now.getMinutes() === 60) {
            now.setHours(now.getHours() + 1);
            now.setMinutes(0);
        }
        dateTimePickerState.value = now;
        dateTimePickerState.viewYear = now.getFullYear();
        dateTimePickerState.viewMonth = now.getMonth();
        renderDateTimePicker();
        return;
    }

    if (event.target.matches('[data-datetime-done]')) {
        applyDateTimePicker();
    }
});

document.addEventListener('keydown', event => {
    if (event.key !== 'Escape') return;
    if (eventModal && !eventModal.classList.contains('hidden')) closeEventModal();
    if (attendeesModal && !attendeesModal.classList.contains('hidden')) closeAttendeesModal();
    if (eventEditModal && !eventEditModal.classList.contains('hidden')) closeEditEventModal();
    const datetimeModal = document.getElementById('datetime-picker-modal');
    if (datetimeModal && !datetimeModal.classList.contains('hidden')) closeDateTimePicker();
});

/* ==================================================
   INITIALIZATION
================================================== */

setSelectedRole(selectedRole);
toggleAuthMode('login');

/*
 * Obtain an initial CSRF token so the first
 * POST request can be protected.
 */
getCsrfToken()
    .catch(error => {
        console.error(
            'Failed to initialize CSRF protection:',
            error
        );
    });

const savedUser =
    localStorage.getItem(
        'event-management-user'
    );

if (savedUser) {

    try {

        state.currentUser =
            JSON.parse(savedUser);

        refreshData();

    } catch (error) {

        console.error(
            'Invalid saved user:',
            error
        );

        localStorage.removeItem(
            'event-management-user'
        );

        showAuthView();
    }

} else {

    showAuthView();
}